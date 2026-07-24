package com.swj.shiwujie.common.network;

import android.content.Context;
import android.util.Log;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BaseResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.MultipartBody;
import java.io.File;

/**
 * 图片识别管理器。
 *
 * <p>chunk-2f-1：老 SSE 图片通道（{@code sendImage}/{@code OnStreamingListener}/
 * {@code handleStreamingResponse}，调已删的 {@code /api/ai/ai/doChatByImage}，必 404）全清。
 * 现仅承载 chunk-2e-3 的图片 AI-turn 上行：multipart 上传图 + text，流式答复骑 WS 经
 * {@code AiTurnManager} 现成路由消费（onDelta→文本+TTS / onProgress→"正在识别照片"）。</p>
 */
public class ImageRecognitionManager {
    private static final String TAG = "ImageRecognitionManager";

    private final ApiService apiService;

    /**
     * chunk-2e-3：图片 AI-turn 上行回调。仅 ack（流式响应骑 WS 经 AiTurnManager 路由，不经此回调）。
     */
    public interface ImageTurnCallback {
        /** 图片已上传、中继已提交；后续等 WS 110/111/112/113 帧（AiTurnManager 路由）。 */
        void onUploaded();
        /** 上传失败（网络 / 服务端非 1）；调用方应 {@code aiTurnManager.abortTurn} 解锁麦克风。 */
        void onError(String reason);
    }

    public ImageRecognitionManager(Context context) {
        // 初始化SharedPrefsUtil以获取token
        SharedPrefsUtil.init(context);
        this.apiService = RetrofitClient.getInstance().createService(ApiService.class);
    }

    /**
     * chunk-2e-3：图片 AI-turn 上行（WS-response 通道，替代老 SSE {@code sendImage}）。
     * <p>multipart 上传图 + text 到 {@code /api/call/ai/image-turn}，仅需 ack——Java 转图 base64 调 Python
     * {@code /ai/turn {image}}，流式答复骑 WS 经 AiTurnManager 路由（onDelta/onProgress/onTurnEnd）。
     * 失败 → {@link ImageTurnCallback#onError}（调用方应 {@code aiTurnManager.abortTurn(reason)} 立即解锁）。</p>
     *
     * @param imageFile 图片文件
     * @param text      口述追问；空则后端用默认提示诱导 recognize_photo
     * @param callback  上传 ack 回调
     */
    public void sendImageTurn(File imageFile, String text, ImageTurnCallback callback) {
        if (imageFile == null || !imageFile.exists()) {
            if (callback != null) {
                callback.onError("图片文件不存在");
            }
            return;
        }
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            if (callback != null) {
                callback.onError("用户未登录，请先登录");
            }
            return;
        }

        okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                "image", imageFile.getName(), requestFile);
        okhttp3.RequestBody textBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"), text == null ? "" : text);
        String authToken = "Bearer " + token;

        apiService.sendAiImageTurn(authToken, imagePart, textBody).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (callback == null) return;
                BaseResponse<Void> body = response.body();
                if (response.isSuccessful() && body != null && body.getCode() == 1) {
                    Log.d(TAG, "图片 turn 上传成功，中继已提交，等 WS 响应帧");
                    callback.onUploaded();
                } else {
                    String reason = "图片上传失败";
                    if (body != null && body.getMessage() != null && !body.getMessage().isEmpty()) {
                        reason = body.getMessage();
                    } else if (!response.isSuccessful()) {
                        reason = "服务器异常(HTTP " + response.code() + ")";
                    }
                    Log.e(TAG, "图片 turn 上传失败：" + reason);
                    callback.onError(reason);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                Log.e(TAG, "图片 turn 上传网络失败", t);
                if (callback != null) {
                    callback.onError(networkReason(t));
                }
            }
        });
    }

    private String networkReason(Throwable t) {
        if (t instanceof java.net.SocketTimeoutException) return "请求超时，请检查网络连接";
        if (t instanceof java.net.UnknownHostException) return "无法连接到服务器";
        if (t instanceof java.net.ConnectException) return "连接被拒绝";
        return "网络错误: " + t.getMessage();
    }
}
