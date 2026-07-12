package com.swj.shiwujie.common.network;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.SocketDataV0;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局视频通话状态管理器
 * 负责管理视频通话状态、实时通知、全局监听
 */
public class VideoCallManager {
    private static final String TAG = "VideoCallManager";
    
    private static VideoCallManager instance;
    
    // 视频通话状态
    public static final int CALL_STATUS_IDLE = 0;        // 空闲
    public static final int CALL_STATUS_WAITING = 1;     // 等待匹配
    public static final int CALL_STATUS_IN_CALL = 2;     // 通话中
    public static final int CALL_STATUS_ENDED = 3;       // 通话结束
    
    // 当前状态
    private int currentCallStatus = CALL_STATUS_IDLE;
    private String currentCallId = null;
    private String matchedBlindPhone = null;
    private String matchedVolunteerPhone = null;
    private long callStartTime = 0;
    
    // 全局监听器列表
    private List<VideoCallStatusListener> statusListeners = new ArrayList<>();
    private List<VideoCallMessageListener> messageListeners = new ArrayList<>();
    
    // 上下文，用于显示Toast
    private Context context;

    // 监听器回调统一切到主线程：handleWebSocketMessage 跑在 java-websocket 读线程，
    // 直接 for-loop 调监听器会让 Activity 回调改 UI 触发 "Only the original thread..." 崩溃（A3）
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private VideoCallManager() {}
    
    public static synchronized VideoCallManager getInstance() {
        if (instance == null) {
            instance = new VideoCallManager();
        }
        return instance;
    }
    
    /**
     * 设置上下文
     */
    public void setContext(Context context) {
        this.context = context;
    }
    
    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        if (context != null) {
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    /**
     * 更新通话状态
     */
    public void updateCallStatus(int status, String callId, String blindPhone, String volunteerPhone) {
        Log.d(TAG, "更新通话状态: " + status + ", callId: " + callId);
        
        this.currentCallStatus = status;
        this.currentCallId = callId;
        this.matchedBlindPhone = blindPhone;
        this.matchedVolunteerPhone = volunteerPhone;
        
        if (status == CALL_STATUS_IN_CALL && callStartTime == 0) {
            callStartTime = System.currentTimeMillis();
        } else if (status == CALL_STATUS_IDLE || status == CALL_STATUS_ENDED) {
            callStartTime = 0;
        }
        
        // 更新WebSocketManager的业务状态
        WebSocketManager webSocketManager = WebSocketManager.getInstance();
        boolean inVideoCall = (status == CALL_STATUS_IN_CALL);
        webSocketManager.setVideoCallStatus(inVideoCall);
        
        // 通知所有监听器
        notifyStatusChanged(status, callId, blindPhone, volunteerPhone);
        
        // 显示状态提示
        showStatusToast(status);
    }
    
    /**
     * 处理WebSocket消息
     */
    public void handleWebSocketMessage(SocketDataV0 data) {
        Log.e(TAG, "=== VideoCallManager处理WebSocket消息 ===");
        Log.e(TAG, "消息类型: " + data.getRequestType());
        Log.e(TAG, "消息内容: " + data.toString());
        
        switch (data.getRequestType()) {
            case SocketDataV0.REQUEST_TYPE_LOGIN:          // 0 Socket登录
                handleSocketLogin(data);
                break;
            case SocketDataV0.REQUEST_TYPE_MATCH_SUCCESS:  // 1 志愿者匹配成功通知
                handleMatchSuccess(data);
                break;
            case SocketDataV0.REQUEST_TYPE_VIDEO_INIT:     // 2 视频初始化成功通知
                handleVideoInitSuccess(data);
                break;
            default:
                // 3/4/5（紧急求助通知）与 5001~5006（AI/跳转）不在视频通话职责内：
                // 3/4 由 volunteer/HomeFragment 处理，5 由 blind/HomeFragment 处理，落 default 合理（A6 结论）。
                Log.d(TAG, "非视频通话消息类型，忽略: " + data.getRequestType());
                break;
        }
        
        // 通知消息监听器
        notifyMessageReceived(data);
    }
    

    
    /**
     * 处理Socket登录
     */
    private void handleSocketLogin(SocketDataV0 data) {
        Log.e(TAG, "=== 收到Socket登录消息 ===");
        Log.e(TAG, "Socket登录成功");
     /*   showToast("Socket登录成功");*/
    }
    
    /**
     * 处理匹配成功
     */
    private void handleMatchSuccess(SocketDataV0 data) {
        Log.e(TAG, "=== 收到志愿者匹配成功通知 ===");
        Log.e(TAG, "匹配成功: " + data.getCallId());
        Log.e(TAG, "盲人手机号: " + data.getBlindPhone());
        Log.e(TAG, "志愿者手机号: " + data.getVolunteerPhone());
        Log.e(TAG, "频道ID: " + data.getChannelId());
        showToast("匹配成功，开始通话");
        updateCallStatus(CALL_STATUS_IN_CALL, data.getCallId(), 
                        data.getBlindPhone(), data.getVolunteerPhone());
    }
    
    /**
     * 处理视频初始化成功
     */
    private void handleVideoInitSuccess(SocketDataV0 data) {
        Log.e(TAG, "=== 收到视频初始化成功通知 ===");
        Log.e(TAG, "视频初始化成功: " + data.getCallId());
       /* showToast("视频初始化成功");*/
    }
    
    /**
     * 显示状态提示
     */
    private void showStatusToast(int status) {
        String message = "";
        switch (status) {
            case CALL_STATUS_IDLE:
                message = "通话状态：空闲";
                break;
            case CALL_STATUS_WAITING:
                message = "通话状态：等待匹配";
                break;
            case CALL_STATUS_IN_CALL:
                message = "通话状态：通话中";
                break;
            case CALL_STATUS_ENDED:
                message = "通话状态：通话结束";
                break;
        }
       /* showToast(message);*/
    }
    
    /**
     * 通知状态变化
     */
    private void notifyStatusChanged(int status, String callId, String blindPhone, String volunteerPhone) {
        mainHandler.post(() -> {
            for (VideoCallStatusListener listener : statusListeners) {
                try {
                    listener.onCallStatusChanged(status, callId, blindPhone, volunteerPhone);
                } catch (Exception e) {
                    Log.e(TAG, "通知状态监听器失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 通知消息接收
     */
    private void notifyMessageReceived(SocketDataV0 data) {
        mainHandler.post(() -> {
            for (VideoCallMessageListener listener : messageListeners) {
                try {
                    listener.onVideoCallMessageReceived(data);
                } catch (Exception e) {
                    Log.e(TAG, "通知消息监听器失败: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 添加状态监听器
     */
    public void addStatusListener(VideoCallStatusListener listener) {
        if (!statusListeners.contains(listener)) {
            statusListeners.add(listener);
        }
    }
    
    /**
     * 移除状态监听器
     */
    public void removeStatusListener(VideoCallStatusListener listener) {
        statusListeners.remove(listener);
    }
    
    /**
     * 添加消息监听器
     */
    public void addMessageListener(VideoCallMessageListener listener) {
        if (!messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }
    
    /**
     * 移除消息监听器
     */
    public void removeMessageListener(VideoCallMessageListener listener) {
        messageListeners.remove(listener);
    }
    
    /**
     * 获取当前通话状态
     */
    public int getCurrentCallStatus() {
        return currentCallStatus;
    }
    
    /**
     * 获取当前通话ID
     */
    public String getCurrentCallId() {
        return currentCallId;
    }
    
    /**
     * 获取匹配的盲人手机号
     */
    public String getMatchedBlindPhone() {
        return matchedBlindPhone;
    }
    
    /**
     * 获取匹配的志愿者手机号
     */
    public String getMatchedVolunteerPhone() {
        return matchedVolunteerPhone;
    }
    
    /**
     * 获取通话开始时间
     */
    public long getCallStartTime() {
        return callStartTime;
    }
    
    /**
     * 获取通话时长（毫秒）
     */
    public long getCallDuration() {
        if (callStartTime > 0 && currentCallStatus == CALL_STATUS_IN_CALL) {
            return System.currentTimeMillis() - callStartTime;
        }
        return 0;
    }
    
    /**
     * 检查是否在通话中
     */
    public boolean isInCall() {
        return currentCallStatus == CALL_STATUS_IN_CALL;
    }
    
    /**
     * 检查是否在等待匹配
     */
    public boolean isWaitingForMatch() {
        return currentCallStatus == CALL_STATUS_WAITING;
    }
    
    /**
     * 清理资源
     */
    public void destroy() {
        statusListeners.clear();
        messageListeners.clear();
        context = null;
        instance = null;
    }
    
    /**
     * 视频通话状态监听器接口
     */
    public interface VideoCallStatusListener {
        void onCallStatusChanged(int status, String callId, String blindPhone, String volunteerPhone);
    }
    
    /**
     * 视频通话消息监听器接口
     */
    public interface VideoCallMessageListener {
        void onVideoCallMessageReceived(SocketDataV0 data);
    }
} 