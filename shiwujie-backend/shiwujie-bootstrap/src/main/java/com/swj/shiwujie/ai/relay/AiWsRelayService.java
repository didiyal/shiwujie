package com.swj.shiwujie.ai.relay;

import cn.hutool.json.JSONUtil;
import com.swj.shiwujie.model.VO.call.SocketVO;
import com.swj.shiwujie.model.request.call.Position;
import com.swj.shiwujie.model.request.call.SocketData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * AI-turn 流式中继服务（design 缝 A · Java 侧）。
 *
 * <p>WS {@code onMessage} 收到 requestType=100（{@link AiWsTypes#IN_TURN}）→ 调 {@link #submitRelay}
 * 提交后台任务后立即返回（不阻塞容器 WS 线程）→ 后台线程 {@code HttpClient} POST Python {@code /ai/turn}
 * （FastAPI StreamingResponse，ndjson 逐事件）→ 每行 {@link AiTurnEvent#parse} → {@link #toFrame} 映射
 * SocketVO 帧 → {@link Session#getAsyncRemote()} 非阻塞推回同一 App session。</p>
 *
 * <p><b>断连省 token</b>：每行发送前查 {@code session.isOpen()}，用户中途断开即抛 {@link StreamAborted}
 * 中止拉流并关闭 HTTP 流（取消上游订阅 → Python 端停发）——chunk-2c 真 qwen 时这是成本闸门，避免盲人
 * 误触返回后 Python 继续烧 token。FakeChatModel（2a/2b）零成本，但机制先就位。</p>
 *
 * <p><b>并发注</b>：{@code getAsyncRemote()} 由容器排队发送（多生产者安全）；既有信令仍走 getBasicRemote()。
 * jakarta 规范对同 session 混用 basic/async 行为未定义，但本应用单盲人低并发、信令与流极少真并发，
 * 可接受；若未来并发增高，统一到 async 或加 per-session 发送锁。</p>
 */
@Slf4j
@Service
public class AiWsRelayService {


    @Value("${shiwujie.ai.python.base-url:http://127.0.0.1:8500}")
    private String baseUrl;

    @Value("${shiwujie.ai.python.internal-secret:dev-internal-secret}")
    private String internalSecret;

    @Value("${shiwujie.ai.python.turn-path:/ai/turn}")
    private String turnPath;

    @Value("${shiwujie.ai.python.timeout-seconds:60}")
    private int timeoutSeconds;

    private HttpClient httpClient;

    private ExecutorService executor;


    @PostConstruct
    void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "ai-relay");
            t.setDaemon(true);
            return t;
        });
    }


    @PreDestroy
    void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }


    /**
     * 提交一个 AI turn 中继任务（异步、立即返回）。Python 不可达 / 非 200 / 解析异常 → 发 error 帧（type 113）。
     * session 中途断开 → 静默中止拉流，不发 error 帧（用户已不在）。
     */
    public void submitRelay(Session session, long blindId, String text, Position position) {
        submitInternal(session, blindId, text, position, null);
    }

    /**
     * 图片 AI-turn 中继（chunk-2e-3，task 3.8 图片走 HTTP multipart）。
     * <p>App 经 HTTP multipart 上传图片到 {@code /api/call/ai/image-turn}，控制器查到该盲人 WS session 后调本方法——
     * <b>复用文本 turn 的 ndjson→WS 帧中继通路</b>：POST Python {@code /ai/turn} 携带 {@code image}（base64 data URL），
     * 把流式响应（agent 调 recognize_photo → VLM 描述 → 末答）经同一 session 推回 110/111/112/113 帧，App 端
     * {@code AiTurnManager} 现成路由（onDelta→文本+TTS / onProgress→"正在识别照片"）零改动复用。</p>
     *
     * <p>text 通常为默认提示（"请描述这张图片"）或盲人口述追问；image 为 {@code data:image/jpeg;base64,...}，
     * Python {@code set_image_context} 灌入 vlm contextvar 供 recognize_photo 工具体读取。</p>
     */
    public void submitImageRelay(Session session, long blindId, String text, String imageDataUrl) {
        submitInternal(session, blindId, text, null, imageDataUrl);
    }

    private void submitInternal(Session session, long blindId, String text, Position position, String imageDataUrl) {
        executor.submit(() -> {
            try {
                runRelay(session, blindId, text, position, imageDataUrl);
            } catch (StreamAborted e) {
                log.info("AI-turn 中继：session 已断开，停止拉流 blindId={}", blindId);
            } catch (Exception e) {
                log.error("AI-turn 中继失败 blindId={}：{}", blindId, e.toString());
                sendFrame(session, errorFrame());
            }
        });
    }


    private void runRelay(Session session, long blindId, String text, Position position, String imageDataUrl)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + turnPath))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("X-Internal-Secret", internalSecret)
                .header("X-Blind-Id", String.valueOf(blindId))
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(blindId, text, position, imageDataUrl)))
                .build();

        HttpResponse<Stream<String>> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofLines());
        if (resp.statusCode() != 200) {
            throw new IOException("Python /ai/turn 返回 " + resp.statusCode());
        }
        try (Stream<String> lines = resp.body()) {
            lines.forEach(line -> {
                if (!session.isOpen()) {
                    // 用户断开 → 抛出中止 forEach，try-with-resources 关流取消上游订阅（省 token）。
                    throw new StreamAborted();
                }
                AiTurnEvent ev;
                try {
                    ev = AiTurnEvent.parse(line);
                } catch (Exception parseEx) {
                    log.warn("ndjson 行解析失败，跳过：{} | {}", line, parseEx.toString());
                    return;
                }
                SocketVO frame = toFrame(ev);
                if (frame != null) {
                    sendFrame(session, frame);
                }
            });
        }
    }


    private String buildBody(long blindId, String text, Position position, String imageDataUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("thread_id", String.valueOf(blindId)); // Python TurnRequest.thread_id:str = blind_id
        body.put("text", text == null ? "" : text);
        if (position != null) {
            Map<String, Object> pos = new HashMap<>();
            pos.put("lat", position.getLat());
            pos.put("lng", position.getLng());
            pos.put("address", position.getAddress());
            body.put("position", pos);
        }
        // chunk-2e-3：图片 turn（imageDataUrl 非空）→ Python set_image_context 灌 vlm contextvar，
        // recognize_photo 工具体读取（data URL 直喂 qwen3-vl-flash）。文本 turn 不带 image 字段（Python 默认 None）。
        if (imageDataUrl != null) {
            body.put("image", imageDataUrl);
        }
        return JSONUtil.toJsonStr(body);
    }


    /** 发一帧到 App（非阻塞）；session 已关 / 发送异常 → 静默丢弃（中继可能正跑完）。 */
    private void sendFrame(Session session, SocketVO frame) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.getAsyncRemote().sendText(JSONUtil.toJsonStr(frame));
        } catch (Exception e) {
            log.warn("WS 帧发送失败（session 可能已关）：{}", e.toString());
        }
    }


    /**
     * ndjson 事件 → SocketVO 帧。{@code null}/无 type/turn_start/未知 → null（不发：turn_start 是 Python
     * 内部轮次号，App 不消费）。
     */
    static SocketVO toFrame(AiTurnEvent ev) {
        if (ev == null || ev.getType() == null) {
            return null;
        }
        switch (ev.getType()) {
            case "delta":
                return frame(AiWsTypes.OUT_DELTA, "AI回复", textSocketData(ev.getText()));
            case "progress":
                return frame(AiWsTypes.OUT_PROGRESS, "AI进度", textSocketData(ev.getEvent()));
            case "turn_end":
                return frame(AiWsTypes.OUT_TURN_END, "AI结束", new SocketData());
            default:
                return null;
        }
    }


    private static SocketVO errorFrame() {
        // design ⑫ encode-不抛在 Java 侧的等价：Python 不可达 / 异常 → encode 友好提示帧，盲人拿到降级答复。
        return frame(AiWsTypes.OUT_ERROR, "AI错误", textSocketData("系统暂时遇到问题，请稍后再试。"));
    }


    private static SocketVO frame(int requestType, String message, SocketData socketData) {
        socketData.setRequestType(requestType);
        SocketVO vo = new SocketVO();
        vo.setCode(0);
        vo.setMessage(message);
        vo.setSocketData(socketData);
        return vo;
    }


    private static SocketData textSocketData(String text) {
        SocketData d = new SocketData();
        d.setText(text);
        return d;
    }


    /** 内部信号：用户断开，中止拉 Python 流（省 token）。仅用于解 forEach 循环。 */
    private static final class StreamAborted extends RuntimeException {

        private static final long serialVersionUID = 1L;

    }


}
