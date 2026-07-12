package com.swj.shiwujie.common.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;
import okhttp3.MultipartBody;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图片识别管理器
 * 负责与后端图片识别接口通信，支持流式输出效果
 */
public class ImageRecognitionManager {
    private static final String TAG = "ImageRecognitionManager";
    
    private final Context context;
    private final ApiService apiService;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
    // 流式输出相关
    private boolean isStreaming = false;
    private String currentResponse = "";
    private OnStreamingListener streamingListener;
    
    // 打字机效果配置
    private int typingSpeed = 50; // 字符间延迟，单位毫秒
    
    public interface OnStreamingListener {
        void onStreamingStart();
        void onStreamingText(String text);
        void onStreamingChunk(String chunk, int totalLength);
        void onStreamingComplete(String fullResponse);
        void onStreamingError(String error);
    }
    
    public ImageRecognitionManager(Context context) {
        this.context = context;
        // 初始化SharedPrefsUtil以获取token
        SharedPrefsUtil.init(context);
        this.apiService = RetrofitClient.getInstance().createService(ApiService.class);
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 发送图片到AI接口
     * @param imageFile 图片文件
     * @param listener 流式输出监听器
     */
    public void sendImage(File imageFile, OnStreamingListener listener) {
        if (imageFile == null || !imageFile.exists()) {
            if (listener != null) {
                listener.onStreamingError("图片文件不存在");
            }
            return;
        }
        
        // 如果传入了监听器，则使用传入的；否则使用预先设置的
        OnStreamingListener targetListener = listener != null ? listener : this.streamingListener;
        if (targetListener == null) {
            Log.e(TAG, "没有设置流式输出监听器");
            return;
        }
        
        // 获取token
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            if (targetListener != null) {
                targetListener.onStreamingError("用户未登录，请先登录");
            }
            return;
        }
        
        // 构造完整的认证头
        String authToken = "Bearer " + token;
        
        // 创建MultipartBody.Part
        okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(
            okhttp3.MediaType.parse("image/jpeg"), 
            imageFile
        );
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
            "imageFile", 
            imageFile.getName(), 
            requestFile
        );
        
        // 调用图片识别接口
        Call<ResponseBody> call = apiService.sendAiImageMessage(authToken, imagePart);
        
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "AI图片识别请求响应: HTTP " + response.code());
                
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        Log.d(TAG, "开始处理流式响应，响应体大小: " + responseBody.contentLength());
                        // 处理流式响应
                        handleStreamingResponse(responseBody, targetListener);
                    } else {
                        Log.e(TAG, "响应体为空");
                        if (targetListener != null) {
                            targetListener.onStreamingError("响应体为空");
                        }
                    }
                } else {
                    Log.e(TAG, "请求失败，HTTP状态码: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "未知错误";
                        Log.e(TAG, "错误响应: " + errorBody);
                        if (targetListener != null) {
                            targetListener.onStreamingError("请求失败: " + errorBody);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "读取错误响应失败", e);
                        if (targetListener != null) {
                            targetListener.onStreamingError("网络请求失败，状态码: " + response.code());
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "AI图片识别请求失败", t);
                String errorMessage = "网络连接失败";
                if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "请求超时，请检查网络连接";
                } else if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "无法连接到服务器";
                } else if (t instanceof java.net.ConnectException) {
                    errorMessage = "连接被拒绝";
                } else {
                    errorMessage = "网络错误: " + t.getMessage();
                }
                
                if (targetListener != null) {
                    targetListener.onStreamingError(errorMessage);
                }
            }
        });
    }
    
    /**
     * 发送图片到AI接口（使用预先设置的监听器）
     * @param imageFile 图片文件
     */
    public void sendImage(File imageFile) {
        sendImage(imageFile, null);
    }
    
    /**
     * 处理流式响应
     * @param responseBody 响应体
     * @param listener 流式输出监听器
     */
    private void handleStreamingResponse(ResponseBody responseBody, OnStreamingListener listener) {
        if (responseBody == null) {
            OnStreamingListener targetListener = listener != null ? listener : this.streamingListener;
            if (targetListener != null) {
                targetListener.onStreamingError("响应体为空");
            }
            return;
        }
        
        isStreaming = true;
        currentResponse = "";
        
        // 通知开始流式输出
        OnStreamingListener targetListener = listener != null ? listener : this.streamingListener;
        if (targetListener != null) {
            mainHandler.post(() -> targetListener.onStreamingStart());
        }
        
        // 在后台线程中处理流式响应
        executorService.execute(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
                String line;
                StringBuilder fullResponse = new StringBuilder();
                int processedLines = 0;
                int processedChars = 0;
                
                Log.d(TAG, "开始处理流式响应...");
                
                while ((line = reader.readLine()) != null && isStreaming) {
                    Log.d(TAG, "接收到流式数据: " + line);
                    
                    if (line.trim().isEmpty()) continue;
                    
                    // 处理SSE格式的数据
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6); // 移除 "data: " 前缀
                        Log.d(TAG, "解析SSE数据: 原始行='" + line + "', 提取内容='" + data + "'");
                        
                        if ("[DONE]".equals(data)) {
                            Log.d(TAG, "流式响应结束");
                            break; // 流式响应结束
                        }
                        
                        // 实时处理每个数据片段，实现真正的流式输出
                        if (data != null && !data.isEmpty()) {
                            // 清理可能存在的"data:"前缀
                            String cleanData = data.replaceAll("(?i)data:", "").trim();
                            
                            if (!cleanData.isEmpty()) {
                                Log.d(TAG, "清理后数据: '" + cleanData + "'");
                                
                                // 将清理后的数据按字符逐个显示，实现打字机效果
                                for (int i = 0; i < cleanData.length(); i++) {
                                    if (!isStreaming) break; // 检查是否被中断
                                    
                                    char currentChar = cleanData.charAt(i);
                                    currentResponse += currentChar;
                                    fullResponse.append(currentChar);
                                    
                                    // 限制日志长度，避免累积内容过长
                                    String logContent = currentResponse.length() > 100 ? 
                                        currentResponse.substring(0, 100) + "..." : currentResponse;

                                    
                                    // 立即在主线程中更新UI，实现真正的流式输出
                                    final String currentText = currentResponse;
                                    mainHandler.post(() -> {
                                        if (targetListener != null) {
                                            targetListener.onStreamingText(currentText);
                                        }
                                    });
                                    
                                    // 流式播报：每累积一定字符数就触发播报回调
                                    if (currentResponse.length() % 50 == 0) {
                                        final String chunkText = currentResponse;
                                        final int totalLength = currentResponse.length();
                                        mainHandler.post(() -> {
                                            if (targetListener != null) {
                                                targetListener.onStreamingChunk(chunkText, totalLength);
                                            }
                                        });
                                    }
                                    
                                    // 添加字符间延迟，模拟打字机效果
                                    try {
                                        Thread.sleep(typingSpeed);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }
                                processedLines++;
                                processedChars += cleanData.length();
                            }
                        }
                        
                    } else {
                        // 如果不是SSE格式，直接处理原始数据
                        Log.d(TAG, "处理原始数据: " + line);
                        
                        // 同样清理可能存在的data:前缀
                        String cleanLine = line.replaceAll("(?i)data:", "").trim();
                        
                        if (!cleanLine.isEmpty()) {
                            // 将清理后的原始数据按字符逐个显示
                            for (int i = 0; i < cleanLine.length(); i++) {
                                if (!isStreaming) break;
                                
                                char currentChar = cleanLine.charAt(i);
                                currentResponse += currentChar;
                                fullResponse.append(currentChar);
                                

                                
                                // 立即在主线程中更新UI
                                final String currentText = currentResponse;
                                mainHandler.post(() -> {
                                    if (targetListener != null) {
                                        targetListener.onStreamingText(currentText);
                                    }
                                });
                                
                                // 流式播报：每累积一定字符数就触发播报回调
                                if (currentResponse.length() % 50 == 0) {
                                    final String chunkText = currentResponse;
                                    final int totalLength = currentResponse.length();
                                    mainHandler.post(() -> {
                                        if (targetListener != null) {
                                            targetListener.onStreamingChunk(chunkText, totalLength);
                                        }
                                    });
                                }
                                
                                // 添加字符间延迟
                                try {
                                    Thread.sleep(typingSpeed);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                            processedLines++;
                            processedChars += cleanLine.length();
                        }
                    }
                }
                
                Log.d(TAG, "流式响应处理完成，总长度: " + fullResponse.length() + 
                    ", 处理行数: " + processedLines + ", 处理字符数: " + processedChars);
                
                // 流式输出完成
                mainHandler.post(() -> {
                    isStreaming = false;
                    if (targetListener != null) {
                        targetListener.onStreamingComplete(fullResponse.toString());
                    }
                });
                
                reader.close();
                
            } catch (IOException e) {
                Log.e(TAG, "处理流式响应失败", e);
                mainHandler.post(() -> {
                    isStreaming = false;
                    if (targetListener != null) {
                        targetListener.onStreamingError("处理流式响应失败: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 停止流式输出
     */
    public void stopStreaming() {
        isStreaming = false;
    }
    
    /**
     * 释放资源
     */
    public void destroy() {
        stopStreaming();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * 设置流式输出监听器
     */
    public void setOnStreamingListener(OnStreamingListener listener) {
        this.streamingListener = listener;
    }
    
    /**
     * 设置打字机效果速度
     * @param speed 字符间延迟，单位毫秒（建议范围：20-200）
     */
    public void setTypingSpeed(int speed) {
        if (speed >= 0) {
            this.typingSpeed = speed;
            Log.d(TAG, "设置打字机速度: " + speed + "ms");
        }
    }
}
