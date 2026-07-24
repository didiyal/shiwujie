package com.swj.shiwujie.common.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.SocketDataV0;

/**
 * AI turn WS 路由胶水层（chunk-2e-2 缝 A）。
 *
 * <p>挂全局 {@link WebSocketManager.MessageListener}，把后端 {@code AiWsRelayService} 下发的 5 类帧
 * （110 delta / 111 progress / 112 turn_end / 113 error / 114 emergency_token）路由成 {@link AiTurnListener}
 * 回调，让 AiFragment 不直接碰协议码；上行（requestType=100）也由此发。114 不进 turn 状态机
 * （prepare 在 turn 中途签发，仅透传 token 给 UI 弹确认面——chunk-2e-4 gate ③）。</p>
 *
 * <p><b>防重连 + 超时兜底</b>：turn 进行中置 {@code WebSocketManager.setMatchingStatus(true)} 压制重连
 * （避免 turn_end 被 WS 重连吃掉）；同时挂 45s watchdog——relay 异常断连不发 113 时主动 onError + reset，
 * 防止麦克风永久锁死（计划风险 #3 硬需求）。</p>
 *
 * <p><b>生命周期</b>：AiFragment 持有实例（非单例），{@link #AiTurnManager()} 构造时 addMessageListener，
 * {@link #destroy()} 移除。tab 切换反复 onCreate/onDestroyView 时正常工作（单例会在 destroy 后无法重新
 * 注册——instance 已存在，故改 Fragment 生命周期持有）。</p>
 *
 * <p><b>多监听器</b>：AiFragment 既有 MessageListener（5001-5006）与本类注册的都会收到 110-113；
 * 既有那个 switch 走 default 丢弃，本类处理，不冲突、不双更新。</p>
 */
public class AiTurnManager {

    private static final String TAG = "AiTurnManager";

    /** turn 超时兜底：relay 异常断连不发 113 时，到点主动收尾解锁麦克风。 */
    private static final long TURN_TIMEOUT_MS = 45_000L;

    public enum ProgressType {
        SEARCHING, THINKING, RECOGNIZING_PHOTO, ROUTING, UNKNOWN
    }

    public interface AiTurnListener {
        /** 首个 delta 到达（AI 开始说话）。UI 可在此"显示思考"已由 sendTurn 预置，此回调可选用于状态切换。 */
        void onTurnStart();

        /** 110：一个 token 片段 + 截止当前的完整累积文本（UI 直接 setText 累积值）。 */
        void onDelta(String token, StringBuilder accumulated);

        /** 111：工具进度（searching/thinking/recognizing_photo/routing）。 */
        void onProgress(ProgressType type);

        /** 112：本轮结束，传完整回复。App 收尾 TTS + 解锁输入。 */
        void onTurnEnd(String fullResponse);

        /** 113：中继/Python 异常（固定友好文案）或客户端超时。 */
        void onError(String message);

        /**
         * 114：紧急求助确认 token（chunk-2e-4 gate ③）。
         * <p>agent {@code request_emergency_help_prepare} 签发，socketData.text=token。App 收到后弹
         * 显式确认面（盲人无视觉冗余，须屏幕硬选择），用户确认才消费 token（非-MCP HTTP /blind/confirm）。</p>
         */
        void onEmergencyToken(String token);
    }

    private final WebSocketManager ws;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AiTurnListener listener;
    private final StringBuilder accumulator = new StringBuilder();
    private boolean turnActive = false;

    private final Runnable watchdog = () -> {
        Log.w(TAG, "AI turn 超时（" + (TURN_TIMEOUT_MS / 1000) + "s 无 turn_end），主动收尾");
        if (turnActive && listener != null) {
            listener.onError("回复超时，请重试");
        }
        resetTurnState();
    };

    /**
     * 字段持有方法引用，保证 add/remove 注册的是同一对象。
     * （Java 8 方法引用 {@code this::onWsMessage} 每次取都 new 新实例，remove 时取不回。）
     */
    private final WebSocketManager.MessageListener internalListener = this::onWsMessage;

    public AiTurnManager() {
        ws = WebSocketManager.getInstance();
        ws.addMessageListener(internalListener);
    }

    public void setListener(AiTurnListener listener) {
        this.listener = listener;
    }

    /**
     * 发送上行 AI turn（requestType=100）。position 允许 null（无定位降级）。
     * 复用 {@code WebSocketManager.setMatchingStatus(true)} 压制 turn 期间重连，挂超时 watchdog。
     */
    public void sendTurn(String text, SocketDataV0.Position position) {
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "sendTurn: text 空，忽略");
            return;
        }
        beginTurn();
        String phone = SharedPrefsUtil.getPhone();
        boolean isBlind = SharedPrefsUtil.isBlind();
        SocketDataV0 req = SocketDataV0.createAiTurnRequest(phone, isBlind, text, position);
        ws.sendMessage(req);
        Log.d(TAG, "sendTurn: 发送 100 帧 textLen=" + text.length() + " hasPos=" + (position != null));
    }

    /**
     * chunk-2e-3：图片 turn 前奏。图片经 HTTP multipart 上传（{@code AiImageTurnController}），<b>不走 WS 100 帧</b>；
     * 但响应仍骑 WS 推回（110/111/112/113），由现有 {@link #onWsMessage} 路由。故此处复用 sendTurn 的状态
     * 初始化——清 accumulator + 锁重连 + 挂 watchdog——<b>仅不发 WS 帧</b>。调用方上传成功后等 WS 帧即可；
     * 上传失败调 {@link #abortTurn} 立即收尾（不等 45s watchdog）。
     */
    public void beginImageTurn() {
        beginTurn();
        Log.d(TAG, "beginImageTurn: 图片 turn 前奏就绪（状态初始化，等 HTTP 上传 → WS 响应路由）");
    }

    /**
     * chunk-2e-3：图片上传失败等需立即收尾的场景——主动走 {@link AiTurnListener#onError} + resetTurnState
     * （复用 listener 错误清理路径：AiFragment.onError → handleAiResponseError + isAiTurnInProgress=false +
     * enableInput(true)），不等 watchdog 超时。
     */
    public void abortTurn(String errorMsg) {
        Log.w(TAG, "abortTurn: 主动收尾 - " + errorMsg);
        if (listener != null) {
            listener.onError(errorMsg);
        }
        resetTurnState();
    }

    /** turn 前奏（sendTurn / beginImageTurn 共用）：清累积 + 锁重连 + 挂 watchdog。 */
    private void beginTurn() {
        accumulator.setLength(0);
        turnActive = false; // 收到首个 delta 时置 true 并触发 onTurnStart
        ws.setMatchingStatus(true);
        mainHandler.removeCallbacks(watchdog);
        mainHandler.postDelayed(watchdog, TURN_TIMEOUT_MS);
    }

    /**
     * WS 消息分发。{@link WebSocketManager} 的 MessageListener 回调已在主线程（mainHandler.post），
     * 故此处及下游 listener 均主线程，listener 可直接操作 UI（AiFragment 仍用 runOnUiThread 双保险）。
     */
    private void onWsMessage(SocketDataV0 data) {
        if (data == null || listener == null) {
            return;
        }
        switch (data.getRequestType()) {
            case SocketDataV0.REQUEST_TYPE_AI_DELTA: { // 110
                if (!turnActive) {
                    turnActive = true;
                    listener.onTurnStart();
                }
                String token = data.getText();
                if (token != null && !token.isEmpty()) {
                    accumulator.append(token);
                    listener.onDelta(token, accumulator);
                }
                break;
            }
            case SocketDataV0.REQUEST_TYPE_AI_PROGRESS: { // 111
                listener.onProgress(parseProgress(data.getText()));
                break;
            }
            case SocketDataV0.REQUEST_TYPE_AI_TURN_END: { // 112
                listener.onTurnEnd(accumulator.toString());
                resetTurnState();
                break;
            }
            case SocketDataV0.REQUEST_TYPE_AI_ERROR: { // 113
                listener.onError(data.getText());
                resetTurnState();
                break;
            }
            case SocketDataV0.REQUEST_TYPE_EMERGENCY_TOKEN: { // 114 — gate ③ 紧急确认 token
                // 不归 turn 状态机：prepare() 在 turn 中途签发，turn 仍正常流转到 112/113。
                // 仅把 token 透传给 UI 弹确认面，不碰 accumulator / turnActive / watchdog。
                listener.onEmergencyToken(data.getText());
                break;
            }
            default:
                break; // 其他 requestType（-1/0/1-5/5001-5006）不归本类管
        }
    }

    private void resetTurnState() {
        turnActive = false;
        accumulator.setLength(0);
        mainHandler.removeCallbacks(watchdog);
        ws.setMatchingStatus(false);
    }

    private ProgressType parseProgress(String s) {
        if (s == null) {
            return ProgressType.UNKNOWN;
        }
        switch (s) {
            case "searching":
                return ProgressType.SEARCHING;
            case "thinking":
                return ProgressType.THINKING;
            case "recognizing_photo":
                return ProgressType.RECOGNIZING_PHOTO;
            case "routing":
                return ProgressType.ROUTING;
            default:
                return ProgressType.UNKNOWN;
        }
    }

    /** AiFragment onDestroy 调：摘本类 listener + 取消 watchdog + 释放业务锁，不动 WS 连接本身。 */
    public void destroy() {
        ws.removeMessageListener(internalListener);
        resetTurnState();
        listener = null;
    }
}
