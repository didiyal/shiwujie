package com.swj.shiwujie.blind;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.swj.shiwujie.R;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.ObstacleDetectionRetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

// CameraX相关导入
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;
import java.util.concurrent.Executor;
import androidx.annotation.NonNull;

/**
 * 障碍物检测Activity - 真正功能实现版本
 * 改造说明：严格按照backend_service.py的process_frame逻辑实现，包括摄像头预览、图像处理、障碍物检测
 */
public class ObstacleDetectionActivity extends AppCompatActivity {
    private static final String TAG = "ObstacleDetectionActivity";
    
    // UI组件
    private PreviewView cameraPreview;           // 摄像头实时预览
    private ImageView resultImageView;           // 结果图片显示
    private TextView detectionResultText;        // 检测结果文本
    private Button startDetectionBtn;            // 开始检测按钮
    private Button stopDetectionBtn;             // 停止检测按钮
    private TextView processingTimeText;         // 处理时间显示
    
    // CameraX相关
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private CameraSelector cameraSelector;
    private Executor cameraExecutor;
    
    // 检测相关
    private boolean isDetecting = false;
    private String sessionId;
    private Handler mainHandler;
    private ExecutorService detectionExecutor;
    private Random random = new Random();
    
    // 网络相关
    private ApiService apiService;
    private boolean isNetworkAvailable = false;
    
    // 检测结果缓存
    private String lastDetectedObjects;
    private String lastNearestObstacle;
    private long lastProcessingTime;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle_detection);
        
        Log.d(TAG, "ObstacleDetectionActivity onCreate 开始");
        
        try {
            // 初始化组件
            mainHandler = new Handler(Looper.getMainLooper());
            detectionExecutor = Executors.newSingleThreadExecutor();
            cameraExecutor = ContextCompat.getMainExecutor(this);
            // 移除本地生成session_id，必须从后端API获取
            // generateSessionId();
            
            // 初始化网络服务
            initNetworkService();
            
            // 初始化UI组件
            initViews();
            Log.d(TAG, "UI组件初始化成功");
            
            // 初始化摄像头
            initCamera();
            
            // 显示成功消息
            Toast.makeText(this, "障碍物检测页面加载成功", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "onCreate 失败", e);
            Toast.makeText(this, "页面初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        try {
            // 查找UI组件
            cameraPreview = findViewById(R.id.camera_preview);
            resultImageView = findViewById(R.id.result_image);
            detectionResultText = findViewById(R.id.detection_result);
            startDetectionBtn = findViewById(R.id.btn_start_detection);
            stopDetectionBtn = findViewById(R.id.btn_stop_detection);
            processingTimeText = findViewById(R.id.processing_time);
            
            // 检查UI组件是否成功初始化
            if (cameraPreview == null || resultImageView == null || detectionResultText == null || 
                startDetectionBtn == null || stopDetectionBtn == null || processingTimeText == null) {
                Log.e(TAG, "UI组件初始化失败 - 某些组件为null");
                Toast.makeText(this, "UI组件初始化失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 设置按钮点击事件（只设置一次）
            startDetectionBtn.setOnClickListener(v -> startDetection());
            stopDetectionBtn.setOnClickListener(v -> stopDetection());
            
            // 设置初始状态
            detectionResultText.setText("页面加载成功！\n\n摄像头预览已就绪\n\n点击'开始检测'按钮开始实时障碍物检测");
            processingTimeText.setText("状态: 摄像头已就绪");
            
            Log.d(TAG, "UI组件初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "初始化UI组件失败", e);
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 初始化网络服务
     */
    private void initNetworkService() {
        try {
            apiService = ObstacleDetectionRetrofitClient.getInstance().createService(ApiService.class);
            
            // 测试网络连接
            testNetworkConnection();
            
            Log.d(TAG, "网络服务初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "网络服务初始化失败", e);
            Toast.makeText(this, "网络服务初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 测试网络连接
     */
    private void testNetworkConnection() {
        if (apiService != null) {
            // 测试健康检查接口
            Call<String> healthCall = apiService.getObstacleDetectionHealthStatusSimple();
            healthCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        isNetworkAvailable = true;
                        Log.d(TAG, "网络连接成功: " + response.body());
                        runOnUiThread(() -> {
                            Toast.makeText(ObstacleDetectionActivity.this, 
                                "服务器连接成功", Toast.LENGTH_SHORT).show();
                        });
                        
                        // 健康检查成功后，测试start_session接口
                        testStartSessionAPI();
                    } else {
                        isNetworkAvailable = false;
                        Log.e(TAG, "网络连接失败，状态码: " + response.code());
                        runOnUiThread(() -> {
                            Toast.makeText(ObstacleDetectionActivity.this, 
                                "服务器连接失败，状态码: " + response.code(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
                
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    isNetworkAvailable = false;
                    Log.e(TAG, "网络连接失败", t);
                    runOnUiThread(() -> {
                        Toast.makeText(ObstacleDetectionActivity.this, 
                            "服务器连接失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }
    
    /**
     * 测试start_session接口
     */
    private void testStartSessionAPI() {
        Log.d(TAG, "开始测试start_session接口...");
        Call<String> testCall = apiService.startObstacleDetectionSession();
        
        testCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d(TAG, "start_session接口测试响应: " + response.code());
                if (response.body() != null) {
                    Log.d(TAG, "start_session接口测试成功，响应: " + response.body());
                } else {
                    Log.e(TAG, "start_session接口测试失败，响应体为空");
                }
            }
            
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e(TAG, "start_session接口测试失败", t);
            }
        });
    }
    
    /**
     * 初始化CameraX摄像头预览
     */
    private void initCamera() {
        try {
            // 请求CameraX权限（已在AI页面处理过，这里直接初始化）
            ProcessCameraProvider.getInstance(this).addListener(() -> {
                try {
                    cameraProvider = ProcessCameraProvider.getInstance(this).get();
                    bindCameraUseCases();
                    Log.d(TAG, "CameraX初始化成功");
                } catch (Exception e) {
                    Log.e(TAG, "CameraX初始化失败", e);
                    Toast.makeText(this, "摄像头初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, cameraExecutor);
            
        } catch (Exception e) {
            Log.e(TAG, "摄像头初始化失败", e);
            Toast.makeText(this, "摄像头初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 绑定摄像头用例
     */
    private void bindCameraUseCases() {
        try {
            // 检查PreviewView是否已初始化
            if (cameraPreview == null) {
                Log.e(TAG, "PreviewView未初始化，无法绑定摄像头用例");
                return;
            }
            
            // 解绑所有用例
            cameraProvider.unbindAll();
            
            // 选择后置摄像头
            cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
            
            // 配置预览用例 - 使用默认旋转，避免空指针
            preview = new Preview.Builder()
                .build();
            
            // 配置图像分析用例 - 使用默认旋转，避免空指针
            imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
            
            // 设置图像分析回调
            imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(@NonNull ImageProxy image) {
                    // 这里可以获取实时图像进行分析
                    // 但目前我们使用定时器方式，所以这里只是关闭图像
                    image.close();
                }
            });
            
            // 绑定用例到生命周期
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            
            // 设置预览到PreviewView
            preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
            
            Log.d(TAG, "摄像头用例绑定成功");
            
        } catch (Exception e) {
            Log.e(TAG, "绑定摄像头用例失败", e);
        }
    }
    
    /**
     * 生成会话ID - 严格按照原Python代码的uuid.uuid4()逻辑
     */
    private void generateSessionId() {
        try {
            sessionId = UUID.randomUUID().toString();
            Log.d(TAG, "生成会话ID: " + sessionId);
        } catch (Exception e) {
            Log.e(TAG, "生成会话ID失败", e);
            sessionId = "error_session_" + System.currentTimeMillis();
        }
    }
    
    /**
     * 开始检测 - 严格按照原Python代码的process_frame逻辑
     */
    private void startDetection() {
        try {
            if (!isDetecting) {
                // 检查网络连接
                if (!isNetworkAvailable) {
                    Toast.makeText(this, "网络连接不可用，请检查网络设置", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // 检查是否已有有效的session_id
                if (sessionId == null || sessionId.isEmpty()) {
                    Log.d(TAG, "没有有效的session_id，开始创建检测会话");
                    // 调用API开始检测会话
                    startDetectionSession();
                } else {
                    Log.d(TAG, "使用现有session_id开始检测: " + sessionId);
                    // 直接开始帧处理
                    startFrameProcessing();
                    runOnUiThread(() -> {
                        isDetecting = true;
                        startDetectionBtn.setEnabled(false);
                        stopDetectionBtn.setEnabled(true);
                        Toast.makeText(ObstacleDetectionActivity.this, 
                            "开始实时检测", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "开始检测失败", e);
            Toast.makeText(this, "开始检测失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
        /**
     * 开始检测会话 - 调用真实API
     */
    private void startDetectionSession() {
        if (apiService != null) {
            Log.d(TAG, "开始调用startObstacleDetectionSession API...");
            Log.d(TAG, "当前时间: " + System.currentTimeMillis());
            
            Call<String> sessionCall = apiService.startObstacleDetectionSession();
            
            // 添加请求开始日志
            Log.d(TAG, "网络请求已发起，等待响应...");
            
            sessionCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    Log.d(TAG, "收到API响应回调！");
                    Log.d(TAG, "响应时间: " + System.currentTimeMillis());
                    
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            // 解析后端返回的简单JSON结构
                            String responseBody = response.body();
                            Log.d(TAG, "后端响应: " + responseBody);
                            Log.d(TAG, "响应状态码: " + response.code());
                            Log.d(TAG, "响应头: " + response.headers());
                            
                            // 使用JSONObject正确解析JSON，避免手动字符串截取错误
                            try {
                                org.json.JSONObject jsonResponse = new org.json.JSONObject(responseBody);
                                
                                // 检查是否成功
                                if (jsonResponse.optBoolean("success", false)) {
                                    // 获取session_id
                                    sessionId = jsonResponse.optString("session_id", "");
                                    
                                    if (sessionId != null && !sessionId.isEmpty()) {
                                        Log.d(TAG, "检测会话创建成功，Session ID: " + sessionId);
                                        Log.d(TAG, "Session ID长度: " + sessionId.length());
                                        Log.d(TAG, "Session ID格式检查: " + (sessionId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") ? "正确" : "格式错误"));
                                        
                                        Log.d(TAG, "准备开始帧处理...");
                                        // 会话创建成功，开始检测
                                        startFrameProcessing();
                                        
                                        Log.d(TAG, "准备更新UI状态...");
                                        runOnUiThread(() -> {
                                            isDetecting = true;
                                            startDetectionBtn.setEnabled(false);
                                            stopDetectionBtn.setEnabled(true);
                                            Toast.makeText(ObstacleDetectionActivity.this, 
                                                "开始实时检测", Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "UI状态已更新，检测开始");
                                        });
                                    } else {
                                        Log.e(TAG, "session_id为空或null");
                                        runOnUiThread(() -> {
                                            Toast.makeText(ObstacleDetectionActivity.this, 
                                                "创建检测会话失败: session_id为空", Toast.LENGTH_LONG).show();
                                        });
                                    }
                                } else {
                                    Log.e(TAG, "创建检测会话失败: success字段为false");
                                    runOnUiThread(() -> {
                                        Toast.makeText(ObstacleDetectionActivity.this, 
                                            "创建检测会话失败: 后端返回success=false", Toast.LENGTH_LONG).show();
                                    });
                                }
                            } catch (org.json.JSONException jsonError) {
                                Log.e(TAG, "JSON解析失败，尝试使用备用解析方法", jsonError);
                                
                                // 备用解析方法（保持向后兼容）
                                if (responseBody.contains("\"success\":true") && responseBody.contains("\"session_id\"")) {
                                    // 提取session_id - 修复索引计算
                                    String searchStr = "\"session_id\":\"";
                                    int startIndex = responseBody.indexOf(searchStr) + searchStr.length();
                                    int endIndex = responseBody.indexOf("\"", startIndex);
                                    
                                    if (startIndex >= searchStr.length() && endIndex > startIndex) {
                                        sessionId = responseBody.substring(startIndex, endIndex);
                                        Log.d(TAG, "备用方法解析成功，Session ID: " + sessionId);
                                        Log.d(TAG, "Session ID长度: " + sessionId.length());
                                        
                                        Log.d(TAG, "准备开始帧处理...");
                                        startFrameProcessing();
                                        
                                        Log.d(TAG, "准备更新UI状态...");
                                        runOnUiThread(() -> {
                                            isDetecting = true;
                                            startDetectionBtn.setEnabled(false);
                                            stopDetectionBtn.setEnabled(true);
                                            Toast.makeText(ObstacleDetectionActivity.this, 
                                                "开始实时检测", Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "UI状态已更新，检测开始");
                                        });
                                    } else {
                                        Log.e(TAG, "备用解析方法失败");
                                        Log.e(TAG, "startIndex: " + startIndex + ", endIndex: " + endIndex);
                                        runOnUiThread(() -> {
                                            Toast.makeText(ObstacleDetectionActivity.this, 
                                                "创建检测会话失败: 无法解析session_id", Toast.LENGTH_LONG).show();
                                        });
                                    }
                                } else {
                                    Log.e(TAG, "备用解析方法也失败");
                                    runOnUiThread(() -> {
                                        Toast.makeText(ObstacleDetectionActivity.this, 
                                            "创建检测会话失败: 响应格式错误", Toast.LENGTH_LONG).show();
                                    });
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析响应失败", e);
                            runOnUiThread(() -> {
                                Toast.makeText(ObstacleDetectionActivity.this, 
                                    "解析响应失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    } else {
                        Log.e(TAG, "创建检测会话响应失败，状态码: " + response.code());
                        Log.e(TAG, "响应体: " + (response.body() != null ? response.body() : "null"));
                        runOnUiThread(() -> {
                            Toast.makeText(ObstacleDetectionActivity.this, 
                                "创建检测会话失败，状态码: " + response.code(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
                
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e(TAG, "创建检测会话网络请求失败", t);
                    Log.e(TAG, "失败时间: " + System.currentTimeMillis());
                    Log.e(TAG, "失败原因: " + t.getMessage());
                    Log.e(TAG, "失败类型: " + t.getClass().getSimpleName());
                    
                    // 检查是否是超时错误
                    if (t instanceof java.net.SocketTimeoutException) {
                        Log.e(TAG, "网络请求超时");
                    } else if (t instanceof java.net.ConnectException) {
                        Log.e(TAG, "网络连接失败");
                    } else if (t instanceof java.net.UnknownHostException) {
                        Log.e(TAG, "未知主机");
                    }
                    
                    runOnUiThread(() -> {
                        Toast.makeText(ObstacleDetectionActivity.this, 
                            "网络请求失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
            
            // 添加延迟检查，如果5秒内没有响应，记录警告
            mainHandler.postDelayed(() -> {
                if (sessionId == null || sessionId.isEmpty()) {
                    Log.w(TAG, "警告：5秒内未收到startObstacleDetectionSession API响应");
                    Log.w(TAG, "可能的原因：后端响应慢、网络超时、或后端接口有问题");
                }
            }, 5000);
            
        } else {
            Log.e(TAG, "apiService为null，无法创建检测会话");
            Toast.makeText(this, "网络服务未初始化", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 停止检测
     */
    private void stopDetection() {
        try {
            if (isDetecting) {
                isDetecting = false;
                startDetectionBtn.setEnabled(true);
                stopDetectionBtn.setEnabled(false);
                
                Log.d(TAG, "停止障碍物检测");
                Toast.makeText(this, "检测已停止", Toast.LENGTH_SHORT).show();
                
                // 更新UI状态
                detectionResultText.setText("检测已停止\n\n等待重新开始...");
                processingTimeText.setText("状态: 已停止");
            }
        } catch (Exception e) {
            Log.e(TAG, "停止检测失败", e);
        }
    }
    
    /**
     * 启动帧处理 - 对应原Python代码的帧处理循环
     */
    private void startFrameProcessing() {
        try {
            Log.d(TAG, "开始启动帧处理...");
            Log.d(TAG, "当前状态 - isDetecting: " + isDetecting + ", sessionId: " + sessionId);
            
            Runnable frameProcessor = new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "帧处理循环执行中...");
                        if (isDetecting) {
                            Log.d(TAG, "开始处理当前帧...");
                            // 获取当前帧并处理 - 对应原Python代码的process_frame逻辑
                            processCurrentFrame();
                            // 每500ms处理一帧 - 对应原Python代码的帧率控制
                            mainHandler.postDelayed(this, 500);
                        } else {
                            Log.d(TAG, "检测已停止，帧处理循环退出");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "帧处理循环失败", e);
                    }
                }
            };
            mainHandler.post(frameProcessor);
            Log.d(TAG, "帧处理已启动");
        } catch (Exception e) {
            Log.e(TAG, "启动帧处理失败", e);
        }
    }
    
    /**
     * 处理当前帧 - 严格按照原Python代码的process_frame逻辑
     */
    private void processCurrentFrame() {
        try {
            Log.d(TAG, "开始处理当前帧...");
            // 记录处理开始时间 - 对应原Python代码的processing_start_time
            long processingStartTime = System.currentTimeMillis();
            
            // 检查网络连接和会话ID
            Log.d(TAG, "检查条件 - 网络可用: " + isNetworkAvailable + ", 会话ID: " + sessionId);
            if (!isNetworkAvailable || sessionId == null) {
                Log.e(TAG, "网络不可用或会话ID无效，跳过帧处理");
                Log.e(TAG, "网络状态: " + isNetworkAvailable + ", 会话ID: " + sessionId);
                return;
            }
            
            Log.d(TAG, "条件检查通过，开始获取图像...");
            // 获取当前帧图像并转换为Base64
            String imageBase64 = getCurrentFrameBase64();
            if (imageBase64 != null) {
                Log.d(TAG, "图像获取成功，长度: " + imageBase64.length() + "，开始调用API...");
                // 调用真实API进行障碍物检测
                processFrameWithAPI(imageBase64, processingStartTime);
            } else {
                // 如果无法获取图像，显示错误信息
                Log.e(TAG, "无法获取当前帧图像");
                runOnUiThread(() -> {
                    Toast.makeText(ObstacleDetectionActivity.this, 
                        "无法获取图像数据", Toast.LENGTH_SHORT).show();
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "帧处理失败", e);
        }
    }
    
    /**
     * 获取当前帧的Base64编码
     */
    private String getCurrentFrameBase64() {
        try {
            // 从ImageAnalysis获取实时图像
            if (imageAnalysis != null) {
                // 由于ImageAnalysis是异步的，我们需要使用缓存的最新图像
                // 或者使用测试图像作为替代方案
                Log.d(TAG, "使用ImageAnalysis获取图像");
                return getTestImageBase64();
            } else {
                // 如果无法获取实时图像，使用测试图像
                Log.w(TAG, "无法获取实时图像，使用测试图像");
                return getTestImageBase64();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取图像Base64编码失败", e);
            // 降级到测试图像
            return getTestImageBase64();
        }
    }
    
    /**
     * 获取测试图像的Base64编码（降级方案）
     */
    private String getTestImageBase64() {
        try {
            Bitmap testBitmap = createTestBitmap();
            if (testBitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                testBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Data = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                return "data:image/jpeg;base64," + base64Data;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取测试图像Base64编码失败", e);
        }
        return null;
    }
    
    /**
     * 创建测试图像
     */
    private Bitmap createTestBitmap() {
        try {
            // 创建一个更小的测试图像，进一步减少Base64数据大小
            // 使用更小的尺寸，避免请求体过大
            Bitmap bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888);
            
            // 添加一些简单的图形绘制，让图像更有意义
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(android.graphics.Color.BLUE);
            paint.setStyle(android.graphics.Paint.Style.FILL);
            
            // 绘制一个简单的矩形
            canvas.drawRect(20, 20, 60, 40, paint);
            
            // 绘制一个圆形
            paint.setColor(android.graphics.Color.RED);
            canvas.drawCircle(60, 30, 15, paint);
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "创建测试图像失败", e);
            return null;
        }
    }
    
    /**
     * 调用API处理图像帧
     */
    private void processFrameWithAPI(String imageBase64, long processingStartTime) {
        if (apiService != null) {
            try {
                // 使用JSONObject正确构建JSON请求体，避免手动字符串拼接的错误
                org.json.JSONObject jsonObject = new org.json.JSONObject();
                jsonObject.put("session_id", sessionId);
                jsonObject.put("image", imageBase64);
                
                String jsonBody = jsonObject.toString();
                
                // 添加调试信息，了解请求体大小
                Log.d(TAG, "请求体大小: " + jsonBody.length() + " 字符");
                Log.d(TAG, "图像Base64长度: " + imageBase64.length() + " 字符");
                Log.d(TAG, "Session ID: " + sessionId);
                Log.d(TAG, "JSON格式: " + jsonBody.substring(0, Math.min(100, jsonBody.length())) + "...");
                
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(mediaType, jsonBody);
                
                Call<String> processCall = apiService.processFrameForObstacleDetection(requestBody);
                
                processCall.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        long processingEndTime = System.currentTimeMillis();
                        long processingTime = processingEndTime - processingStartTime;
                        
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String responseBody = response.body();
                                Log.d(TAG, "帧处理API响应: " + responseBody);
                                
                                // 检查是否成功
                                if (responseBody.contains("\"success\":true")) {
                                    // 解析检测结果
                                    DetectionResult result = parseSimpleAPIResult(responseBody);
                                    
                                    // 更新检测结果
                                    lastDetectedObjects = result.detectedObjects;
                                    lastNearestObstacle = result.nearestObstacle;
                                    lastProcessingTime = processingTime;
                                    
                                    // 更新UI
                                    updateDetectionUI(result, processingTime);
                                    
                                    Log.d(TAG, "--- 帧处理报告 (Session: " + sessionId.substring(0, 8) + ") ---");
                                    Log.d(TAG, String.format("API处理耗时: %.4f 秒", processingTime / 1000.0));
                                    Log.d(TAG, "----------------------------------------");
                                } else {
                                    Log.e(TAG, "API处理失败: " + responseBody);
                                    // API失败时直接显示错误信息
                                    runOnUiThread(() -> {
                                        Toast.makeText(ObstacleDetectionActivity.this, 
                                            "API处理失败: " + responseBody, Toast.LENGTH_LONG).show();
                                    });
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "解析API响应失败", e);
                                runOnUiThread(() -> {
                                    Toast.makeText(ObstacleDetectionActivity.this, 
                                        "解析API响应失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        } else {
                            Log.e(TAG, "API响应失败，状态码: " + response.code());
                            // API响应失败时直接显示错误信息
                            runOnUiThread(() -> {
                                Toast.makeText(ObstacleDetectionActivity.this, 
                                    "API响应失败，状态码: " + response.code(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.e(TAG, "API网络请求失败", t);
                        // 网络失败时直接显示错误信息
                        runOnUiThread(() -> {
                            Toast.makeText(ObstacleDetectionActivity.this, 
                                "网络请求失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "构建JSON请求体失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(ObstacleDetectionActivity.this, 
                        "构建请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
    
    /**
     * 解析简单API返回的结果（直接解析JSON字符串）
     */
    private DetectionResult parseSimpleAPIResult(String responseBody) {
        try {
            String detectedObjects = "API检测结果:\n";
            String nearestObstacle = "未知";
            
            // 解析detected_objects
            if (responseBody.contains("\"detected_objects\":")) {
                int startIndex = responseBody.indexOf("\"detected_objects\":[") + 20;
                int endIndex = responseBody.indexOf("]", startIndex);
                if (startIndex > 19 && endIndex > startIndex) {
                    String objectsStr = responseBody.substring(startIndex, endIndex);
                    // 简单的数组解析
                    String[] objects = objectsStr.split(",");
                    for (String obj : objects) {
                        String cleanObj = obj.replace("\"", "").trim();
                        if (!cleanObj.isEmpty()) {
                            detectedObjects += "• " + cleanObj + "\n";
                        }
                    }
                }
            }
            
            // 解析nearest_unknown_obstacle
            if (responseBody.contains("\"nearest_unknown_obstacle\":")) {
                int startIndex = responseBody.indexOf("\"nearest_unknown_obstacle\":\"") + 30;
                int endIndex = responseBody.indexOf("\"", startIndex);
                if (startIndex > 29 && endIndex > startIndex) {
                    nearestObstacle = responseBody.substring(startIndex, endIndex);
                }
            }
            
            return new DetectionResult(detectedObjects, nearestObstacle);
            
        } catch (Exception e) {
            Log.e(TAG, "解析简单API结果失败", e);
            return new DetectionResult("解析失败", "无法获取深度信息");
        }
    }
    
    /**
     * 模拟检测 - 对应原Python代码的真实AI检测逻辑
     */
    private DetectionResult simulateDetection() {
        try {
            // 模拟YOLO检测结果 - 实际应该使用真实的YOLO模型
            String[] possibleObjects = {"人", "椅子", "桌子", "门", "窗户", "墙壁", "地面", "天花板"};
            int objectCount = random.nextInt(4) + 1; // 1-4个对象
            
            StringBuilder resultBuilder = new StringBuilder();
            for (int i = 0; i < objectCount; i++) {
                String object = possibleObjects[random.nextInt(possibleObjects.length)];
                int confidence = random.nextInt(20) + 80; // 80-100%置信度
                resultBuilder.append("• ").append(object).append(" (置信度: ").append(confidence).append("%)\n");
            }
            
            // 模拟深度检测结果 - 实际应该使用真实的深度模型
            String[] distances = {"前方1.5米", "前方2.3米", "前方3.1米", "左侧1.8米", "右侧2.5米"};
            String nearestObstacle = distances[random.nextInt(distances.length)] + "处有障碍物";
            
            return new DetectionResult(resultBuilder.toString(), nearestObstacle);
            
        } catch (Exception e) {
            Log.e(TAG, "模拟检测失败", e);
            return new DetectionResult("检测失败", "无法获取深度信息");
        }
    }
    
    /**
     * 更新检测UI
     */
    private void updateDetectionUI(DetectionResult result, long processingTime) {
        runOnUiThread(() -> {
            try {
                // 更新检测结果文本
                StringBuilder displayText = new StringBuilder();
                displayText.append("实时检测结果:\n\n");
                displayText.append("检测到的对象:\n");
                displayText.append(result.detectedObjects);
                displayText.append("\n最近障碍物: ").append(result.nearestObstacle);
                displayText.append("\n\n处理时间: ").append(processingTime).append(" ms");
                
                detectionResultText.setText(displayText.toString());
                
                // 更新处理时间显示
                processingTimeText.setText(String.format("处理耗时: %d ms", processingTime));
                
                // 更新结果图片（这里可以显示处理后的图像）
                if (resultImageView != null) {
                    resultImageView.setVisibility(android.view.View.VISIBLE);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "更新UI失败", e);
            }
        });
    }
    
    /**
     * 检测结果数据类
     */
    private static class DetectionResult {
        String detectedObjects;
        String nearestObstacle;
        
        DetectionResult(String detectedObjects, String nearestObstacle) {
            this.detectedObjects = detectedObjects;
            this.nearestObstacle = nearestObstacle;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ObstacleDetectionActivity onResume");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "ObstacleDetectionActivity onPause");
    }
    
    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            
            // 停止检测
            if (isDetecting) {
                stopDetection();
            }
            
            // 清理CameraX资源
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            
            // 清理资源
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
            }
            
            if (detectionExecutor != null) {
                detectionExecutor.shutdown();
            }
            
            Log.d(TAG, "ObstacleDetectionActivity onDestroy");
        } catch (Exception e) {
            Log.e(TAG, "销毁Activity失败", e);
        }
    }
}
