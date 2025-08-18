package com.swj.shiwujie.blind.ui.ai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swj.shiwujie.R;
import com.swj.shiwujie.common.utils.SpeechRecognitionManager;
import com.swj.shiwujie.common.network.AiChatManager;
import com.swj.shiwujie.common.network.ImageRecognitionManager;
import com.swj.shiwujie.common.utils.TTSManager;
import com.swj.shiwujie.common.utils.CameraPreviewManager;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.ObstacleDetectionRetrofitClient;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.utils.ObstacleDetectionTTSManager;
import com.swj.shiwujie.common.utils.AppListManager;
import com.swj.shiwujie.common.service.AIFloatingBallService;
import com.swj.shiwujie.data.model.ObstacleDetectionData;
import com.swj.shiwujie.data.model.ObstacleDetectionData.UnknownObstacle;
import com.swj.shiwujie.data.model.ObstacleDetectionData.DetectedObject;

import com.swj.shiwujie.data.model.SocketDataV0;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.os.Handler;
import android.os.Looper;
import java.util.HashSet;
import java.util.Set;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;



/**
 * AI助理Fragment
 * 集成讯飞语音听写功能，按照官方文档要求实现
 */
public class AiFragment extends Fragment {
    private static final String TAG = "AiFragment";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 201;
    private static final String PREF_NAME = "ai_conversation_history";
    private static final String KEY_CONVERSATIONS = "conversations";
    private static final String KEY_CURRENT_CONVERSATION = "current_conversation";
    
    private SpeechRecognitionManager speechManager;
    private AiChatManager aiChatManager;
    private ImageRecognitionManager imageRecognitionManager;
    private TTSManager ttsManager;
    private CameraPreviewManager cameraManager;
    private Vibrator vibrator;
    private MaterialButton btnVoice;
    private MaterialButton btnCamera;
    private ImageView btnCollapseMessage;
    private com.google.android.material.button.MaterialButton btnExpandMessage;
    private LinearLayout messagePanel;
    private TextureView cameraPreview;
    private TextView tabCurrentConversation;
    private TextView tabHistory;
    private TextView tvConversationTitle;
    private boolean isListening = false;
    private boolean isMessagePanelExpanded = false;
    private boolean isCurrentTabSelected = true;
    
    // 返回主页TTS优先级控制
    private boolean isReturningToHome = false;
    
    // 图片相关
    private Uri photoUri;
    private File photoFile;
    
    // 添加状态检查定时器
    private android.os.Handler statusCheckHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable statusCheckRunnable = new Runnable() {
        @Override
        public void run() {
            // 检查按钮状态，如果异常则重置
            if (isListening && btnVoice != null && 
                btnVoice.getBackgroundTintList() != ContextCompat.getColorStateList(requireContext(), R.color.red)) {
                // 状态不一致，强制重置
                updateVoiceButtonState(true);
              
            }
            
            // 继续检查
            statusCheckHandler.postDelayed(this, 1000);
        }
    };
    
    // 智能播报系统相关
    private enum StreamingState {
        IDLE,           // 空闲状态
        STREAMING,      // 正在流式输出
        PLAYING         // 正在播报
    }
    
    private StreamingState currentStreamingState = StreamingState.IDLE;
    private android.os.Handler smartTimerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable smartTimerRunnable;
    private android.os.Handler streamingPlaybackHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable streamingPlaybackRunnable;
    private long lastTextTime = 0;      // 最后收到文本的时间
    private StringBuilder streamingContentBuffer = new StringBuilder();
    private int lastPlayedContentLength = 0;  // 上次播报的内容长度
    private long lastPlaybackStartTime = 0;   // 上次播报开始的时间
    private static final long SHORT_WAIT = 200;    // 短等待：0.5秒
    private static final long MAX_WAIT = 500;      // 最大等待：1.5秒
    private static final long IDLE_THRESHOLD = 500; // 空闲阈值：500ms
    private static final long STREAMING_UPDATE_INTERVAL = 3000; // 流式播报更新间隔：3秒（减少更新频率）
    
    // 优化后的AI回复管理
    private com.google.android.material.card.MaterialCardView currentAiResponseCard = null;
    private TextView currentAiResponseTextView = null;
    private boolean isAiResponseStreaming = false;
    
    // 对话管理
    private List<Message> currentConversation;
    private List<Conversation> conversationHistory;
    private String currentConversationId;
    private Gson gson;
    
    // 消息数据类
    public static class Message {
        public String content;
        public boolean isUser;
        public long timestamp;
        
        public Message(String content, boolean isUser) {
            this.content = content;
            this.isUser = isUser;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    // 对话数据类
    public static class Conversation {
        public String id;
        public long timestamp;
        public List<Message> messages;
        public String preview; // 对话预览（第一条用户消息）
        
        public Conversation(String id, List<Message> messages) {
            this.id = id;
            this.messages = messages;
            this.timestamp = System.currentTimeMillis();
            if (!messages.isEmpty()) {
                for (Message msg : messages) {
                    if (msg.isUser) {
                        this.preview = msg.content.length() > 30 ? 
                            msg.content.substring(0, 30) + "..." : msg.content;
                        break;
                    }
                }
            }
        }
    }
    
    // AI避障功能相关
    private ApiService apiService;
    private String sessionId;
    private boolean isDetecting = false;
    private boolean isSessionStarted = false;
    private String lastDetectionHash = ""; // 上次检测结果的哈希值，用于去重
    
    // 应用列表管理器
    private AppListManager appListManager;
    
    // AI悬浮球服务
    private boolean isAIFloatingBallServiceRunning = false;
    
    // 相机状态管理
    private boolean isCameraOpen = false;
    private boolean isCameraInitialized = false;
    
    // AI避障功能状态管理
    private boolean isAIAvoidRunning = false;  // AI避障是否正在运行
    private String mSessionId = null;          // 当前会话ID
    private Handler aiAvoidHandler = null;     // AI避障专用Handler
    private Runnable aiAvoidRunnable = null;   // AI避障定时任务
    
    // AI功能业务状态管理（参考blindhome主页）
    private boolean isAIFunctionActive = false; // AI功能是否处于活跃状态
    
    // WebSocket重连管理
    private Handler webSocketReconnectHandler = null;
    private Runnable webSocketReconnectRunnable = null;
    private static final long WEBSOCKET_RECONNECT_DELAY = 3000; // 3秒后重连
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5; // 最大重连次数
    

    

    
    // 新增：存储历史检测结果的列表，用于在重复检测时播报未播报的信息
    private List<ObstacleDetectionData> detectionHistory = new ArrayList<>();
    private Set<String> broadcastedHashes = new HashSet<>(); // 记录已播报的检测结果哈希值
    
    // WebSocket相关
    private WebSocketManager webSocketManager;
    private WebSocketManager.MessageListener webSocketMessageListener;
    private WebSocketManager.ConnectionStatusListener webSocketConnectionListener;
    

    
    private ObstacleDetectionTTSManager obstacleDetectionTTSManager;
    

    

    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_ai_assistant, container, false);
            
            initViews(view);
            initData();
            initSpeechManager();
            initAiChatManager();
            initImageRecognitionManager();
            initTTSManager();
            // 延迟初始化摄像头，避免在onCreateView中过早调用
            
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error inflating AI fragment layout", e);
            // 返回一个简单的错误视图，而不是空的View
            TextView errorView = new TextView(requireContext());
            errorView.setText("AI页面加载失败，请重试");
            errorView.setGravity(android.view.Gravity.CENTER);
            errorView.setPadding(32, 32, 32, 32);
            return errorView;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 在视图创建完成后初始化摄像头
        initCameraManager();
        
        // 设置消息面板初始状态
        setupMessagePanelInitialState();
        
        // 初始化AI避障功能（只初始化，不启动）
        initObstacleDetection();
        
        // 初始化应用列表管理器
        initAppListManager();
        
        // 初始化WebSocket监听
        initWebSocketListener();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 页面暂停时保存当前对话
        saveCurrentConversation();
        // 停止状态检查定时器
        stopStatusCheck();
        
        // 停止AI避障帧处理
        if (isAIAvoidRunning) {
            stopAIAvoidance();
        }
        
        // 安全停止摄像头预览
        safeStopCamera();
        
        // 在AI页面暂停时显示悬浮球
        showAIFloatingBall();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 页面恢复时加载当前对话
        loadCurrentConversation();
        // 检查是否需要清空当前对话（退出登录后）
        checkAndClearConversationIfNeeded();
        // 检查并重置按钮状态
        checkAndResetButtonState();
        
        // 安全启动摄像头预览
        safeStartCamera();
        
        // 如果正在录音，重新启动状态检查
        if (isListening) {
            startStatusCheck();
        }
        
        // 更新AI协助按钮状态
        updateAIAssistButtonState();
        
        // 在AI页面时隐藏悬浮球
        hideAIFloatingBall();
    }
    

    
    /**
     * 检查是否需要清空当前对话
     */
    private void checkAndClearConversationIfNeeded() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            boolean shouldClear = prefs.getBoolean("should_clear_conversation", false);
            
            if (shouldClear) {
                // 清空当前对话
                clearCurrentConversation();
                
                // 重置标记
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("should_clear_conversation", false);
                editor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "检查清理对话失败", e);
        }
    }
    
    /**
     * 清空对话UI
     */
    private void clearConversationUI() {
        if (getView() == null) return;
        
        try {
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer != null) {
                // 保留前两个子视图（建议卡片和AI欢迎消息）
                int childCount = chatContainer.getChildCount();
                if (childCount > 2) {
                    chatContainer.removeViews(2, childCount - 2);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "清空对话UI失败", e);
        }
    }
    
    /**
     * 检查并重置按钮状态，确保状态一致性
     */
    private void checkAndResetButtonState() {
        try {
            if (btnVoice != null && !isListening && 
                btnVoice.getBackgroundTintList() == ContextCompat.getColorStateList(requireContext(), R.color.red)) {
                // 状态不一致，强制重置为正常状态
                updateVoiceButtonState(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "检查按钮状态失败", e);
        }
    }
    
    /**
     * 启动状态检查定时器
     */
    private void startStatusCheck() {
        statusCheckHandler.post(statusCheckRunnable);
    }
    

    
    /**
     * 停止状态检查定时器
     */
    private void stopStatusCheck() {
        statusCheckHandler.removeCallbacks(statusCheckRunnable);
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        btnVoice = view.findViewById(R.id.btn_voice);
        btnCamera = view.findViewById(R.id.btn_camera);
        btnCollapseMessage = view.findViewById(R.id.btn_collapse_message);
        btnExpandMessage = view.findViewById(R.id.btn_expand_message);
        messagePanel = view.findViewById(R.id.message_panel);
        cameraPreview = view.findViewById(R.id.camera_preview);
        
        // 初始化震动器
        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        
        btnVoice.setOnClickListener(v -> {
            if (isAIAvoidRunning) {
                stopAIAvoidance(); // 停止AI避障
            }
            
            // 震动提示：开始录音
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            
            handleVoiceButtonClick();
        });
        
        btnCamera.setOnClickListener(v -> {
            if (isAIAvoidRunning) {
                stopAIAvoidance(); // 停止AI避障
            }
            
            // TTS播报：正在拍照
            if (ttsManager != null) {
                ttsManager.startSpeaking("正在拍照");
            }
            
            handleCameraButtonClick();
        });
        btnCollapseMessage.setOnClickListener(v -> collapseMessagePanel());
        btnExpandMessage.setOnClickListener(v -> expandMessagePanel());
        
        // 返回主页按钮点击事件
        MaterialButton btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // 设置返回主页TTS优先级标志
                isReturningToHome = true;
                
                // TTS播报：正在返回主页
                if (ttsManager != null) {
                    ttsManager.startSpeaking("正在返回主页");
                }
                
                // 使用Navigation导航到主页
                try {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_home);
                    
                    // 延迟播报：已返回主页
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (ttsManager != null) {
                            ttsManager.startSpeaking("已返回主页");
                        }
                        // 清除优先级标志
                        isReturningToHome = false;
                    }, 500); // 延迟500ms，确保导航完成
                    
                } catch (Exception e) {
                    Log.e(TAG, "导航到主页失败", e);
                    // 如果导航失败，显示提示
                    Toast.makeText(requireContext(), "返回主页失败，请重试", Toast.LENGTH_SHORT).show();
                    // 清除优先级标志
                    isReturningToHome = false;
                }
            });
        }
        
        // AI协助按钮 - 控制AI避障功能
        MaterialButton btnAiAssist = view.findViewById(R.id.btn_ai_assist);
        if (btnAiAssist != null) {
            btnAiAssist.setOnClickListener(v -> {
                if (!isAIAvoidRunning) {
                    // 立即播报启动提示
                    if (ttsManager != null) {
                        ttsManager.startSpeaking("正在启用AI避障功能，检测过程中请保持机身平稳");
                    }
                    // 启动AI避障功能
                    startAIAvoidance();
                } else {
                    // 立即播报关闭提示
                    if (ttsManager != null) {
                        ttsManager.startSpeaking("关闭AI避障功能");
                    }
                    // 停止AI避障功能
                    stopAIAvoidance();
                }
            });
        }


    }
    
    /**
     * 初始化数据
     */
    private void initData() {
        gson = new Gson();
        currentConversation = new ArrayList<>();
        currentConversationId = UUID.randomUUID().toString();
        loadConversationHistory();
    }
    
    /**
     * 加载对话历史
     */
    private void loadConversationHistory() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_CONVERSATIONS, "[]");
            Type type = new TypeToken<ArrayList<Conversation>>(){}.getType();
            conversationHistory = gson.fromJson(json, type);
            if (conversationHistory == null) {
                conversationHistory = new ArrayList<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "加载对话历史失败", e);
            conversationHistory = new ArrayList<>();
        }
    }
    
    /**
     * 保存对话历史
     */
    private void saveConversationHistory() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String json = gson.toJson(conversationHistory);
            editor.putString(KEY_CONVERSATIONS, json);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "保存对话历史失败", e);
        }
    }
    
    /**
     * 保存当前对话
     */
    private void saveCurrentConversation() {
        try {
            if (currentConversation != null && !currentConversation.isEmpty()) {
                SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                String json = gson.toJson(currentConversation);
                editor.putString(KEY_CURRENT_CONVERSATION, json);
                editor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "保存当前对话失败", e);
        }
    }
    
    /**
     * 加载当前对话
     */
    private void loadCurrentConversation() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_CURRENT_CONVERSATION, "[]");
            Type type = new TypeToken<ArrayList<Message>>(){}.getType();
            List<Message> savedConversation = gson.fromJson(json, type);
            
            if (savedConversation != null && !savedConversation.isEmpty()) {
                currentConversation.clear();
                currentConversation.addAll(savedConversation);
                
                // 恢复UI显示
                restoreConversationUI();
            }
        } catch (Exception e) {
            Log.e(TAG, "加载当前对话失败", e);
        }
    }
    
    /**
     * 恢复对话UI
     */
    private void restoreConversationUI() {
        if (getView() == null) return;
        
        try {
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 清除现有的对话UI（保留建议卡片和欢迎消息）
            int childCount = chatContainer.getChildCount();
            if (childCount > 2) {
                chatContainer.removeViews(2, childCount - 2);
            }
            
            // 重新添加所有消息
            for (Message msg : currentConversation) {
                com.google.android.material.card.MaterialCardView messageCard = createMessageCard(
                    msg.content,
                    msg.isUser,
                    msg.isUser ? R.color.primary_blue : R.color.blue_50,
                    msg.isUser ? R.color.white : R.color.text_primary
                );
                chatContainer.addView(messageCard);
            }
            
            // 滚动到底部
            scrollToBottom();
        } catch (Exception e) {
            Log.e(TAG, "恢复对话UI失败", e);
        }
    }
    

    
    /**
     * 创建历史记录项视图
     */
    private View createHistoryItemView(Conversation conversation) {
        LinearLayout itemView = new LinearLayout(requireContext());
        itemView.setOrientation(LinearLayout.VERTICAL);
        itemView.setPadding(16, 12, 16, 12);
        itemView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_round_white));
        itemView.setClickable(true);
        itemView.setFocusable(true);
        itemView.setForeground(ContextCompat.getDrawable(requireContext(), android.R.attr.selectableItemBackground));
        
        // 预览文本
        TextView previewText = new TextView(requireContext());
        previewText.setText(conversation.preview != null ? conversation.preview : "对话内容");
        previewText.setTextSize(16);
        previewText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        previewText.setMaxLines(2);
        previewText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        // 时间信息
        TextView timeText = new TextView(requireContext());
        timeText.setText(formatTime(conversation.timestamp));
        timeText.setTextSize(12);
        timeText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        timeText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        
        itemView.addView(previewText);
        itemView.addView(timeText);
        
        // 添加点击事件，返回到当前对话
        itemView.setOnClickListener(v -> {
            // 切换到当前对话标签页
            switchToCurrentTab();
            
            
        });
        
        return itemView;
    }
    
    /**
     * 格式化时间
     */
    private String formatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60000) { // 1分钟内
            return "刚刚";
        } else if (diff < 3600000) { // 1小时内
            return (diff / 60000) + "分钟前";
        } else if (diff < 86400000) { // 24小时内
            return (diff / 3600000) + "小时前";
        } else {
            return (diff / 86400000) + "天前";
        }
    }
    
    /**
     * 初始化摄像头预览管理器
     */
    private void initCameraManager() {
        try {
            if (cameraPreview == null) {
                Log.e(TAG, "cameraPreview为null，无法初始化摄像头");
                return;
            }
            
            if (cameraManager != null) {
                // 如果已经初始化，先关闭旧的
                try {
                    cameraManager.stopPreview();
                    cameraManager = null;
                } catch (Exception e) {
                    Log.w(TAG, "关闭旧摄像头管理器失败", e);
                }
            }
            
            cameraManager = new CameraPreviewManager(requireContext());
            cameraManager.setPreviewView(cameraPreview);
            
            // 检查摄像头权限
            if (checkCameraPermission()) {
                // 权限已授予，启动预览
                cameraManager.startPreview();
                isCameraInitialized = true;
                isCameraOpen = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化摄像头管理器失败", e);
            isCameraInitialized = false;
            isCameraOpen = false;
        }
    }
    
    /**
     * 安全停止相机
     */
    private void safeStopCamera() {
        try {
            if (cameraManager != null && isCameraOpen) {
                cameraManager.stopPreview();
                isCameraOpen = false;

            }
        } catch (IllegalStateException e) {

            isCameraOpen = false;
        } catch (Exception e) {
            Log.e(TAG, "停止相机失败", e);
            isCameraOpen = false;
        }
    }
    
    /**
     * 安全启动相机
     */
    private void safeStartCamera() {
        try {
            if (cameraManager != null && cameraPreview != null && isCameraInitialized && !isCameraOpen) {
                cameraManager.startPreview();
                isCameraOpen = true;

            } else if (!isCameraInitialized) {
                // 如果相机未初始化，重新初始化

                initCameraManager();
            }
        } catch (IllegalStateException e) {

            isCameraOpen = false;
            initCameraManager();
        } catch (Exception e) {
            Log.e(TAG, "启动相机失败", e);
            isCameraOpen = false;
        }
    }
    
    /**
     * 检查相机是否可用
     */
    private boolean isCameraAvailable() {
        return cameraManager != null && isCameraInitialized && isCameraOpen;
    }
    
    /**
     * 启动AI避障功能
     */
    private void startAIAvoidance() {
        if (isAIAvoidRunning) {
    
            return;
        }
        

        isAIAvoidRunning = true;
        
        // 设置AI功能业务状态到WebSocketManager，保持长连接
        if (webSocketManager != null) {
            webSocketManager.setMatchingStatus(true);
            isAIFunctionActive = true;
            Log.d(TAG, "AI避障启动，设置业务状态为活跃");
            // 重置重连计数
            resetWebSocketReconnectCount();
        }
        
        // 更新按钮状态
        updateAIAssistButtonState();
        
        // 启动检测会话
        startDetectionSession();
    }
    
    /**
     * 停止AI避障功能
     */
    private void stopAIAvoidance() {
        if (!isAIAvoidRunning) {
    
            return;
        }
        

        isAIAvoidRunning = false;
        
        // 更新按钮状态
        updateAIAssistButtonState();
        
        // 立即停止定时任务
        stopAIFrameProcessing();
        
        // 立即停止所有TTS播报
        stopAllTTSPlayback();
        
        // 清理会话
        clearAIAvoidSession();
        
        // 强制停止所有正在进行的检测
        forceStopAllDetection();
        
        // 重置AI功能业务状态，允许WebSocket重连
        if (webSocketManager != null) {
            webSocketManager.setMatchingStatus(false);
            isAIFunctionActive = false;
            Log.d(TAG, "AI避障停止，重置业务状态为非活跃");
        }
        
        // 显示停止提示
        showDetectionMessage("AI避障检测已停止", "info");
        

    }
    
    /**
     * 停止AI避障功能（不影响WebSocket连接）
     */
    private void stopAIAvoidanceWithoutWebSocketChange() {
        if (!isAIAvoidRunning) {
            return;
        }
        
        Log.d(TAG, "开始停止AI避障功能（保持WebSocket连接）...");
        
        isAIAvoidRunning = false;
        
        // 更新按钮状态
        updateAIAssistButtonState();
        
        // 立即停止定时任务
        stopAIFrameProcessing();
        
        // 立即停止所有TTS播报
        stopAllTTSPlayback();
        
        // 清理会话
        clearAIAvoidSession();
        
        // 强制停止所有正在进行的检测
        forceStopAllDetection();
        
        // 不设置WebSocket状态，保持连接
        Log.d(TAG, "AI避障停止，但保持WebSocket连接");
        
        // 显示停止提示
        showDetectionMessage("AI避障检测已停止", "info");
    }
    
    /**
     * 页面跳转前预清理资源，避免与新页面启动冲突
     */
    private void preCleanupResourcesForNavigation() {
        Log.d(TAG, "=== 开始预清理资源，准备页面跳转 ===");
        
        try {
            // 1. 立即停止所有Handler消息发送
            if (smartTimerHandler != null) {
                smartTimerHandler.removeCallbacksAndMessages(null);
                Log.d(TAG, "智能播报Handler消息已清理");
            }
            
            if (streamingPlaybackHandler != null) {
                streamingPlaybackHandler.removeCallbacksAndMessages(null);
                Log.d(TAG, "流式播报Handler消息已清理");
            }
            
            if (aiAvoidHandler != null) {
                aiAvoidHandler.removeCallbacksAndMessages(null);
                Log.d(TAG, "AI避障Handler消息已清理");
            }
            
            if (statusCheckHandler != null) {
                statusCheckHandler.removeCallbacksAndMessages(null);
                Log.d(TAG, "状态检查Handler消息已清理");
            }
            
            // 2. 停止摄像头预览
            if (cameraManager != null) {
                try {
                    cameraManager.stopPreview();
                    Log.d(TAG, "摄像头预览已停止");
                } catch (Exception e) {
                    Log.w(TAG, "停止摄像头预览失败", e);
                }
            }
            
            // 3. 停止TTS播报
            if (ttsManager != null) {
                try {
                    ttsManager.stopSpeaking();
                    Log.d(TAG, "TTS播报已停止");
                } catch (Exception e) {
                    Log.w(TAG, "停止TTS播报失败", e);
                }
            }
            
            // 4. 停止AI聊天流式输出
            if (aiChatManager != null) {
                try {
                    aiChatManager.stopStreaming();
                    Log.d(TAG, "AI聊天流式输出已停止");
                } catch (Exception e) {
                    Log.w(TAG, "停止AI聊天流式输出失败", e);
                }
            }
            
            // 5. 停止语音识别
            if (speechManager != null) {
                try {
                    speechManager.stopListening();
                    Log.d(TAG, "语音识别已停止");
                } catch (Exception e) {
                    Log.w(TAG, "停止语音识别失败", e);
                }
            }
            
            // 6. 记录资源清理状态
            Log.d(TAG, "=== 资源预清理完成 ===");
            Log.d(TAG, "Handler状态: 所有消息已清理");
            Log.d(TAG, "摄像头状态: 预览已停止");
            Log.d(TAG, "TTS状态: 播报已停止");
            Log.d(TAG, "AI聊天状态: 流式输出已停止");
            Log.d(TAG, "语音识别状态: 已停止");
            Log.d(TAG, "WebSocket状态: 保持连接");
            Log.d(TAG, "准备页面跳转...");
            
        } catch (Exception e) {
            Log.e(TAG, "资源预清理失败", e);
        }
    }
    
    /**
     * 清理AI避障会话
     */
    private void clearAIAvoidSession() {
        try {
            mSessionId = null;
            isSessionStarted = false;
            Log.d(TAG, "AI避障会话已清理");
        } catch (Exception e) {
            Log.e(TAG, "清理AI避障会话失败", e);
        }
    }
    
    /**
     * 停止所有TTS播报
     */
    private void stopAllTTSPlayback() {
        try {
            Log.d(TAG, "开始停止所有TTS播报...");
            
            // 停止主TTSManager的播报
            if (ttsManager != null) {
                if (ttsManager.isSpeaking()) {
                    Log.d(TAG, "停止主TTS播报");
                    ttsManager.stopSpeaking();
                }
            }
            
            // 停止ObstacleDetectionTTSManager的播报
            if (obstacleDetectionTTSManager != null) {
                Log.d(TAG, "停止障碍物检测TTS播报");
                // 通过destroy来彻底停止
                obstacleDetectionTTSManager.destroy();
                // 重新初始化
                obstacleDetectionTTSManager = new ObstacleDetectionTTSManager(requireContext());
            }
            
            Log.d(TAG, "所有TTS播报已停止");
        } catch (Exception e) {
            Log.e(TAG, "停止TTS播报失败", e);
        }
    }
    
    /**
     * 强制停止所有正在进行的检测
     */
    private void forceStopAllDetection() {
        try {
            Log.d(TAG, "强制停止所有检测...");
            
            // 重置检测状态
            isDetecting = false;
            isSessionStarted = false;
            
            // 清理检测历史
            if (detectionHistory != null) {
                detectionHistory.clear();
            }
            if (broadcastedHashes != null) {
                broadcastedHashes.clear();
            }
            lastDetectionHash = null;
            
            // 强制停止所有Handler
            if (aiAvoidHandler != null) {
                aiAvoidHandler.removeCallbacksAndMessages(null);
                Log.d(TAG, "AI避障Handler已清理");
            }
            
            Log.d(TAG, "所有检测已强制停止");
        } catch (Exception e) {
            Log.e(TAG, "强制停止检测失败", e);
        }
    }
    
    /**
     * 更新AI协助按钮状态
     */
    private void updateAIAssistButtonState() {
        try {
            if (getView() != null) {
                MaterialButton btnAiAssist = getView().findViewById(R.id.btn_ai_assist);
                if (btnAiAssist != null) {
                    if (isAIAvoidRunning) {
                        // AI避障运行中，按钮显示停止状态（仅更新图标，不改动无障碍标签，避免与TTS冲突）
                        btnAiAssist.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_stop_24));
                    } else {
                        // AI避障未运行，按钮显示启动状态（仅更新图标，不改动无障碍标签，避免与TTS冲突）
                        btnAiAssist.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ai));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "更新AI协助按钮状态失败", e);
        }
    }
    
    /**
     * 设置消息面板初始状态
     */
    private void setupMessagePanelInitialState() {
        if (messagePanel == null) return;
        
        try {
            if (isMessagePanelExpanded) {
                // 如果面板应该展开，设置初始宽度为屏幕宽度的80%
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int panelWidth = (int) (screenWidth * 0.8);
                
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagePanel.getLayoutParams();
                params.width = panelWidth;
                messagePanel.setLayoutParams(params);
                
                // 设置初始透明度
                messagePanel.setAlpha(0.9f);
                messagePanel.setVisibility(View.VISIBLE);
                btnExpandMessage.setVisibility(View.GONE);
            } else {
                // 如果面板应该收起，设置宽度为0并隐藏
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagePanel.getLayoutParams();
                params.width = 0;
                messagePanel.setLayoutParams(params);
                
                messagePanel.setVisibility(View.GONE);
                btnExpandMessage.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "设置消息面板初始状态失败", e);
        }
    }
    
    /**
     * 初始化语音识别管理器
     * 按照官方文档要求配置
     */
    private void initSpeechManager() {
        speechManager = new SpeechRecognitionManager(requireContext());
        speechManager.setOnRecognitionListener(new SpeechRecognitionManager.OnRecognitionListener() {
            @Override
            public void onResult(String result) {
                // 识别完成，强制更新按钮状态
                updateVoiceButtonState(false);
                handleSpeechResult(result);
            }
            
            @Override
            public void onPartialResult(String result) {
                // 处理中间结果，实时更新UI
                handlePartialSpeechResult(result);
            }
            
            @Override
            public void onError(String error) {
                // 错误时强制更新按钮状态
                updateVoiceButtonState(false);
                handleSpeechError(error);
            }
            
            @Override
            public void onStart() {
                updateVoiceButtonState(true);
                showRecordingStatus("正在录音...");
            }
            
            @Override
            public void onEnd() {
                // 确保按钮状态更新
                updateVoiceButtonState(false);
                hideRecordingStatus();
                
                // TTS播报：语音输入已结束
                if (ttsManager != null) {
                    ttsManager.startSpeaking("语音输入已结束");
                }
            }
            
            @Override
            public void onBeginOfSpeech() {
                showRecordingStatus("开始说话...");
            }
            
            @Override
            public void onEndOfSpeech() {
                showRecordingStatus("正在识别...");
            }
            
            @Override
            public void onVolumeChanged(int volume) {
                updateVolumeIndicator(volume);
            }
        });
    }
    
    /**
     * 初始化AI对话管理器
     */
    private void initAiChatManager() {
        aiChatManager = new AiChatManager(requireContext());
        
        // 配置打字机效果速度（可选）
        aiChatManager.setTypingSpeed(50); // 50ms延迟，可以根据需要调整
        
        aiChatManager.setOnStreamingListener(new AiChatManager.OnStreamingListener() {
            @Override
            public void onStreamingStart() {
                // AI开始回复，显示"正在思考..."状态
                showAiThinkingStatus();
                // 启动智能播报系统
                startSmartPlaybackSystem();
            }
            
            @Override
            public void onStreamingText(String text) {
                // 实时更新AI回复的流式输出
                updateAiResponseStreaming(text);
                // 处理智能播报逻辑
                handleSmartPlayback(text);
            }
            
            @Override
            public void onStreamingChunk(String chunk, int totalLength) {
                // 流式数据块回调，用于真正的流式播报
                handleStreamingChunk(chunk, totalLength);
            }
            
            @Override
            public void onStreamingComplete(String fullResponse) {
                // 流式输出完成，保存完整回复
                completeAiResponse(fullResponse);
                // 完成智能播报
                completeSmartPlayback(fullResponse);
            }
            
            @Override
            public void onStreamingError(String error) {
                // AI回复出错，显示错误信息
                handleAiResponseError(error);
                // 重置智能播报状态
                resetSmartPlaybackState();
                // 重置AI回复状态
                resetAiResponseState();
            }
        });
    }
    
    /**
     * 初始化图片识别管理器
     */
    private void initImageRecognitionManager() {
        imageRecognitionManager = new ImageRecognitionManager(requireContext());
        
        // 配置打字机效果速度
        imageRecognitionManager.setTypingSpeed(50);
        
        imageRecognitionManager.setOnStreamingListener(new ImageRecognitionManager.OnStreamingListener() {
            @Override
            public void onStreamingStart() {
                // 图片识别开始，显示"正在识别图片..."状态
                showImageRecognitionStatus();
                // 启动智能播报系统
                startSmartPlaybackSystem();
            }
            
            @Override
            public void onStreamingText(String text) {
                // 实时更新图片识别结果的流式输出
                updateImageRecognitionStreaming(text);
                // 处理智能播报逻辑
                handleSmartPlayback(text);
            }
            
            @Override
            public void onStreamingChunk(String chunk, int totalLength) {
                // 流式数据块回调，用于真正的流式播报
                handleStreamingChunk(chunk, totalLength);
            }
            
            @Override
            public void onStreamingComplete(String fullResponse) {
                // 图片识别完成，保存完整回复
                completeImageRecognition(fullResponse);
                // 完成智能播报
                completeSmartPlayback(fullResponse);
            }
            
            @Override
            public void onStreamingError(String error) {
                // 图片识别出错，显示错误信息
                handleImageRecognitionError(error);
                // 重置智能播报状态
                resetSmartPlaybackState();
                // 重置AI回复状态
                resetAiResponseState();
            }
        });
    }
    
    /**
     * 启动智能播报系统
     */
    private void startSmartPlaybackSystem() {
        currentStreamingState = StreamingState.STREAMING;
        streamingContentBuffer.setLength(0);
        lastTextTime = System.currentTimeMillis();
        
        // 启动智能计时器
        startSmartTimer();
    }
    
    /**
     * 处理智能播报逻辑
     */
    private void handleSmartPlayback(String text) {
        if (currentStreamingState != StreamingState.STREAMING) return;
        
        // 更新最后收到文本的时间
        lastTextTime = System.currentTimeMillis();
        
        // 累积内容到缓冲区
        streamingContentBuffer.append(text);
        

        
        // 如果正在播报，动态更新播报内容
        if (currentStreamingState == StreamingState.PLAYING) {
            updatePlaybackContent();
        }
    }
    
    /**
     * 处理流式数据块，实现真正的流式播报
     */
    private void handleStreamingChunk(String chunk, int totalLength) {
        if (currentStreamingState != StreamingState.STREAMING) return;
        
        // 如果还没开始播报，且内容足够长，立即开始播报
        if (currentStreamingState == StreamingState.STREAMING && chunk.length() >= 80) { // 增加触发阈值
            startSmartPlayback();
        }
    }
    
    /**
     * 启动智能计时器
     */
    private void startSmartTimer() {
        // 取消之前的计时器
        if (smartTimerRunnable != null) {
            smartTimerHandler.removeCallbacks(smartTimerRunnable);
        }
        
        smartTimerRunnable = () -> {
            // 检查是否应该开始播报
            long currentTime = System.currentTimeMillis();
            long timeSinceLastText = currentTime - lastTextTime;
            
            if (timeSinceLastText >= IDLE_THRESHOLD) {
                // 超过空闲阈值，开始播报
                startSmartPlayback();
            } else {
                // 继续等待，重新启动计时器
                startSmartTimer();
            }
        };
        
        // 启动计时器，等待时间根据当前状态动态调整
        long waitTime = streamingContentBuffer.length() < 50 ? SHORT_WAIT : MAX_WAIT;
        smartTimerHandler.postDelayed(smartTimerRunnable, waitTime);
    }
    
    /**
     * 动态更新播报内容
     */
    private void updatePlaybackContent() {
        // 检查Fragment状态
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment状态异常，跳过播报内容更新");
            return;
        }
        
        if (currentStreamingState != StreamingState.PLAYING || ttsManager == null) return;
        
        // 获取当前缓冲区的最新内容
        String currentContent = streamingContentBuffer.toString();
        
        // 优化内容长度判断逻辑
        int contentIncrease = currentContent.length() - lastPlayedContentLength;
        
        // 只有当内容增加超过50个字符，且当前播报进度超过70%时才重新播报
        if (contentIncrease > 50 && getCurrentPlaybackProgress() > 70) {
            // 停止当前播报，重新开始播报完整内容
            String contentToSpeak = parseResponseForTTS(currentContent);
            try {
                // 检查是否正在返回主页，如果是则跳过AI对话结果的TTS播报
                if (isReturningToHome) {
                    Log.d(TAG, "正在返回主页，跳过AI对话结果TTS播报");
                    return;
                }
                
                ttsManager.stopSpeaking();
                ttsManager.startSpeaking(contentToSpeak);
                lastPlayedContentLength = currentContent.length();
            } catch (Exception e) {
                Log.e(TAG, "更新播报内容失败", e);
            }
        } else if (contentIncrease > 100) {
            // 如果内容增加超过100个字符，强制重新播报
            String contentToSpeak = parseResponseForTTS(currentContent);
            try {
                // 检查是否正在返回主页，如果是则跳过AI对话结果的TTS播报
                if (isReturningToHome) {
                    Log.d(TAG, "正在返回主页，跳过AI对话结果TTS播报");
                    return;
                }
                
                ttsManager.stopSpeaking();
                ttsManager.startSpeaking(contentToSpeak);
                lastPlayedContentLength = currentContent.length();
            } catch (Exception e) {
                Log.e(TAG, "强制更新播报内容失败", e);
            }
        }
    }
    
    /**
     * 获取当前播报进度（估算值）
     */
    private int getCurrentPlaybackProgress() {
        // 基于时间估算播报进度
        if (lastPlayedContentLength == 0) return 0;
        
        // 假设播报速度约为每分钟200个字符
        long estimatedPlaybackTime = (lastPlayedContentLength * 60) / 200; // 秒
        long elapsedTime = System.currentTimeMillis() - lastPlaybackStartTime;
        
        if (estimatedPlaybackTime <= 0) return 0;
        
        int progress = (int) ((elapsedTime / 1000.0 / estimatedPlaybackTime) * 100);
        return Math.min(progress, 100);
    }
    
    /**
     * 开始智能播报
     */
    private void startSmartPlayback() {
        // 检查Fragment状态
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment状态异常，跳过智能播报");
            return;
        }
        
        if (currentStreamingState == StreamingState.PLAYING) return;
        
        currentStreamingState = StreamingState.PLAYING;
        String contentToPlay = streamingContentBuffer.toString();
        
        if (contentToPlay.trim().isEmpty()) {
            currentStreamingState = StreamingState.STREAMING;
            return;
        }
        
        // 检查TTS管理器状态
        if (ttsManager == null) {
            Log.w(TAG, "TTS管理器未初始化，跳过播报");
            currentStreamingState = StreamingState.STREAMING;
            return;
        }
        
        // 解析后端响应，决定播报内容
        String contentToSpeak = parseResponseForTTS(contentToPlay);
        
        try {
            // 检查是否正在返回主页，如果是则跳过AI对话结果的TTS播报
            if (isReturningToHome) {
                Log.d(TAG, "正在返回主页，跳过AI对话结果TTS播报");
                currentStreamingState = StreamingState.STREAMING;
                return;
            }
            
            ttsManager.startSpeaking(contentToSpeak);
            lastPlayedContentLength = contentToPlay.length();
            lastPlaybackStartTime = System.currentTimeMillis(); // 记录播报开始时间
            
            // 启动流式播报更新定时器
            startStreamingPlaybackUpdateTimer();
        } catch (Exception e) {
            Log.e(TAG, "TTS播报失败", e);
            currentStreamingState = StreamingState.STREAMING;
        }
    }
    
    /**
     * 解析后端响应，决定播报内容
     * 只有当code=1时才播报data内容，否则播报description内容
     */
    private String parseResponseForTTS(String response) {
        try {
            // 尝试解析为JSON格式
            if (response.trim().startsWith("{")) {
                // 使用Gson解析JSON
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.google.gson.JsonElement jsonElement = gson.fromJson(response, com.google.gson.JsonElement.class);
                
                if (jsonElement.isJsonObject()) {
                    com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
                    
                    // 检查是否有code字段
                    if (jsonObject.has("code")) {
                        int code = jsonObject.get("code").getAsInt();
                        
                        if (code == 1) {
                            // code=1，播报data内容
                            if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                                String data = jsonObject.get("data").getAsString();
                                if (data != null && !data.trim().isEmpty()) {
                                    return data;
                                }
                            }
                        } else {
                            // code!=1，播报description内容
                            if (jsonObject.has("description") && !jsonObject.get("description").isJsonNull()) {
                                String description = jsonObject.get("description").getAsString();
                                if (description != null && !description.trim().isEmpty()) {
                                    return description;
                                }
                            }
                            // 如果没有description字段，播报message字段
                            if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                                String message = jsonObject.get("message").getAsString();
                                if (message != null && !message.trim().isEmpty()) {
                                    return message;
                                }
                            }
                        }
                    }
                }
            }
            
            // 如果不是JSON格式或解析失败，直接播报原始内容
            return response;
            
        } catch (Exception e) {
            Log.e(TAG, "解析响应失败，播报原始内容", e);
            return response;
        }
    }
    
    /**
     * 启动流式播报更新定时器
     */
    private void startStreamingPlaybackUpdateTimer() {
        if (streamingPlaybackRunnable != null) {
            streamingPlaybackHandler.removeCallbacks(streamingPlaybackRunnable);
        }
        
        streamingPlaybackRunnable = () -> {
            if (currentStreamingState == StreamingState.PLAYING) {
                // 检查是否有新内容需要播报
                updatePlaybackContent();
                
                // 继续定时器
                streamingPlaybackHandler.postDelayed(streamingPlaybackRunnable, STREAMING_UPDATE_INTERVAL);
            }
        };
        
        // 启动定时器
        streamingPlaybackHandler.postDelayed(streamingPlaybackRunnable, STREAMING_UPDATE_INTERVAL);
    }
    
    /**
     * 完成智能播报
     */
    private void completeSmartPlayback(String fullResponse) {
        // 检查是否正在返回主页，如果是则跳过AI对话结果的TTS播报
        if (isReturningToHome) {
            Log.d(TAG, "正在返回主页，跳过AI对话结果TTS播报");
            resetSmartPlaybackState();
            return;
        }
        
        // 解析后端响应，决定播报内容
        String contentToSpeak = parseResponseForTTS(fullResponse);
        
        // 如果还在播报，确保播报完整内容
        if (currentStreamingState == StreamingState.PLAYING) {
            // 停止当前播报，重新播报完整内容
            ttsManager.stopSpeaking();
            ttsManager.startSpeaking(contentToSpeak);
        } else if (currentStreamingState == StreamingState.STREAMING) {
            // 如果还没开始播报，直接播报完整内容
            ttsManager.startSpeaking(contentToSpeak);
        }
        
        // 重置状态
        resetSmartPlaybackState();
    }
    
    /**
     * 重置智能播报状态
     */
    private void resetSmartPlaybackState() {
        currentStreamingState = StreamingState.IDLE;
        streamingContentBuffer.setLength(0);
        lastTextTime = 0;
        lastPlayedContentLength = 0;
        lastPlaybackStartTime = 0;
        
        // 取消智能计时器
        if (smartTimerRunnable != null) {
            smartTimerHandler.removeCallbacks(smartTimerRunnable);
            smartTimerRunnable = null;
        }
        
        // 取消流式播报更新定时器
        if (streamingPlaybackRunnable != null) {
            streamingPlaybackHandler.removeCallbacks(streamingPlaybackRunnable);
            streamingPlaybackRunnable = null;
        }
    }
    
    /**
     * 重置AI回复状态
     */
    private void resetAiResponseState() {
        isAiResponseStreaming = false;
        currentAiResponseCard = null;
        currentAiResponseTextView = null;
    }
    
    /**
     * 初始化语音合成管理器
     */
    private void initTTSManager() {
        ttsManager = new TTSManager(requireContext());
        // 设置TTS语速为1.5倍速
        ttsManager.setSpeed(75);
        ttsManager.setOnTTSListener(new TTSManager.OnTTSListener() {
            @Override
            public void onTTSStart() {
                // 语音开始播放
            }
            
            @Override
            public void onTTSProgress(int progress, int beginPos, int endPos) {
                // 播放进度更新
            }
            
            @Override
            public void onTTSComplete() {
                // 语音播放完成
            }
            
            @Override
            public void onTTSError(String error) {
                // 语音播放出错
                Log.e(TAG, "TTS播放失败: " + error);
                
                // 检查Fragment状态后再显示Toast
                if (isAdded() && getContext() != null) {
                    try {
                        // Toast.makeText(requireContext(), "语音播放失败: " + error, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.w(TAG, "显示TTS错误提示失败", e);
                    }
                }
            }
            
            @Override
            public void onTTSPause() {
                // 语音暂停
            }
            
            @Override
            public void onTTSResume() {
                // 语音恢复
            }
            
            @Override
            public void onTTSBufferProgress(int percent, int beginPos, int endPos, String info) {
                // 缓冲进度更新
            }
        });
    }
    
    /**
     * 显示录音状态提示
     */
    private void showRecordingStatus(String status) {
        if (getView() != null) {
            try {
                TextView statusText = getView().findViewById(R.id.tv_recording_status);
                if (statusText != null) {
                    statusText.setText(status);
                    statusText.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "显示录音状态失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 隐藏录音状态提示
     */
    private void hideRecordingStatus() {
        if (getView() != null) {
            try {
                TextView statusText = getView().findViewById(R.id.tv_recording_status);
                if (statusText != null) {
                    statusText.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "隐藏录音状态失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 更新音量指示器
     */
    private void updateVolumeIndicator(int volume) {
        // 这里可以实现音量条显示
    }
    
    /**
     * 处理语音按钮点击事件
     * 按照官方文档要求实现语音识别控制
     */
    private void handleVoiceButtonClick() {
        // 添加震动反馈
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        
        if (checkAudioPermission()) {
            if (!isListening) {
                startVoiceRecognition();
                startStatusCheck(); // 启动状态检查
            } else {
                stopVoiceRecognition();
                stopStatusCheck(); // 停止状态检查
                // 强制更新按钮状态
                updateVoiceButtonState(false);
            }
        }
    }
    

    



    
    /**
     * 处理拍照按钮点击事件
     */
    private void handleCameraButtonClick() {
        // 添加震动反馈
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        
        // 检查相机权限
        if (checkCameraPermission()) {
            takePhotoDirectly();
        }
    }
    

    

    
    /**
     * 检查录音权限
     * 按照官方文档要求检查必要权限
     */
    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            
            showPermissionExplanationDialog();
            
            ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{Manifest.permission.RECORD_AUDIO}, 
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }
    
    /**
     * 检查摄像头权限
     */
    private boolean checkCameraPermission() {
        int permissionStatus = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA);
        
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            // 检查是否应该显示权限说明
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
                // 用户之前拒绝过权限，显示说明对话框
                showCameraPermissionExplanationDialog();
            } else {
                // 直接请求权限
                requestCameraPermission();
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * 请求相机权限
     */
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(), 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST_CODE);
    }
    
    /**
     * 检查所有必要权限是否已授予
     */
    private boolean checkAllRequiredPermissions() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
        
        return hasCameraPermission; // 新的拍照方案只需要相机权限
    }
    
    /**
     * 显示权限被拒绝的对话框
     */
    private void showPermissionDeniedDialog(String permissionName, String message) {
        new AlertDialog.Builder(requireContext())
            .setTitle("权限被拒绝")
            .setMessage(message)
            .setPositiveButton("去设置", (dialog, which) -> {
                // 跳转到应用设置页面
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "跳转设置页面失败", e);
                
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    // 移除存储权限检查，因为新的拍照方案使用应用私有目录，无需存储权限
    
    // 移除存储权限请求方法
    
    /**
     * 显示权限说明对话框
     */
    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("需要录音权限")
            .setMessage("语音识别功能需要录音权限才能正常工作。请在接下来的对话框中允许录音权限。")
            .setPositiveButton("确定", null)
            .show();
    }
    
    /**
     * 显示摄像头权限说明对话框
     */
    private void showCameraPermissionExplanationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("需要摄像头权限")
            .setMessage("拍照功能需要摄像头权限才能正常工作。请在接下来的对话框中允许摄像头权限。")
            .setPositiveButton("确定", (dialog, which) -> {
                // 用户点击确定后，立即请求权限
                requestCameraPermission();
            })
            .setNegativeButton("取消", (dialog, which) -> {
                // 用户取消，显示提示
                // Toast.makeText(requireContext(), "没有摄像头权限无法使用拍照功能", Toast.LENGTH_LONG).show();
            })
            .setCancelable(false)
            .show();
    }
    
    // 移除存储权限说明对话框
    
    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Toast.makeText(requireContext(), "录音权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                // Toast.makeText(requireContext(), "录音权限被拒绝，无法使用语音识别功能", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Toast.makeText(requireContext(), "摄像头权限已授予", Toast.LENGTH_SHORT).show();
                // 权限授予后启动摄像头预览
                if (cameraManager != null) {
                    cameraManager.startPreview();
                }
                // 权限授予后，检查是否所有权限都已获得，如果是则自动重试拍照
                if (checkAllRequiredPermissions()) {
                    // 延迟一下再拍照，确保权限完全生效
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        takePhotoDirectly();
                    }, 500);
                }
            } else {
                showPermissionDeniedDialog("摄像头权限", "拍照功能需要摄像头权限才能正常工作。请在设置中开启摄像头权限。");
            }
        }
        // 移除存储权限处理，因为新的拍照方案不需要存储权限
    }
    
    // 移除onActivityResult方法，因为新的拍照方案不需要启动系统相机
    
    /**
     * 开始语音识别
     * 按照官方文档要求调用startListening
     */
    private void startVoiceRecognition() {
        // 在启动语音识别前，先停止正在进行的TTS播报，避免干扰语音识别
        stopAllTTSPlayback();
        
        speechManager.startListening();
    }
    
    /**
     * 停止语音识别
     * 按照官方文档要求调用stopListening
     */
    private void stopVoiceRecognition() {
        speechManager.stopListening();
        
        // TTS播报：语音输入已结束
        if (ttsManager != null) {
            ttsManager.startSpeaking("语音输入已结束");
        }
    }
    

    
    /**
     * 直接拍照（使用Camera2 API，无需启动系统相机）
     */
    private void takePhotoDirectly() {
        if (cameraManager == null) {
            Log.e(TAG, "CameraPreviewManager未初始化");
            // Toast.makeText(requireContext(), "相机管理器未初始化", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!cameraManager.isPreviewActive()) {
            Log.e(TAG, "相机预览未激活");
            // Toast.makeText(requireContext(), "相机预览未激活，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (cameraManager.isTakingPhoto()) {
            // Toast.makeText(requireContext(), "正在拍照中，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示拍照提示
        // Toast.makeText(requireContext(), "正在拍照...", Toast.LENGTH_SHORT).show();
        
        // 调用CameraPreviewManager的直接拍照方法
        cameraManager.takePhoto(new com.swj.shiwujie.common.utils.CameraPreviewManager.TakePhotoCallback() {
            @Override
            public void onPhotoTaken(byte[] data) {
                // 在主线程中处理照片数据
                requireActivity().runOnUiThread(() -> {
                    handlePhotoData(data);
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "拍照失败: " + error);
                
                // 在主线程中显示错误
                requireActivity().runOnUiThread(() -> {
                    // Toast.makeText(requireContext(), "拍照失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 处理拍照得到的字节数据
     */
    private void handlePhotoData(byte[] data) {
        try {
            // 保存为临时文件（应用私有目录，无需存储权限）
            File tempFile = new File(requireContext().getExternalCacheDir(), "temp_photo_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            fos.write(data);
            fos.close();
            
            // 保存文件引用
            photoFile = tempFile;
            
            // 显示拍摄的图片到对话界面
            addImageMessage(photoFile);
            
            // TTS播报：照片已上传，正在执行图像分析
            if (ttsManager != null) {
                ttsManager.startSpeaking("拍照完成，正在执行图像分析");
            }
            
            // 延迟发送图片识别请求，确保图片完全显示后再发送
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                sendImageRecognitionRequest(photoFile);
            }, 500); // 延迟500ms，确保图片完全显示
            
        } catch (Exception e) {
            Log.e(TAG, "处理照片数据失败", e);
            // Toast.makeText(requireContext(), "处理照片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // 移除旧的handlePhotoResult方法，已被handlePhotoData替代
    
    /**
     * 处理语音识别结果
     * 按照官方文档要求处理识别结果
     */
    private void handleSpeechResult(String result) {
        if (result == null || result.trim().isEmpty()) {
            // Toast.makeText(requireContext(), "语音识别结果为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 将语音识别结果添加到对话界面
        addUserMessage(result);
        
        // 延迟发送AI请求，确保用户消息完全显示后再发送
        // 这样可以避免AI回复比用户问题出现更早的问题
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            sendAiRequest(result);
        }, 500); // 延迟500ms，确保用户消息完全显示
    }
    
    /**
     * 处理中间识别结果，实时更新UI
     */
    private void handlePartialSpeechResult(String result) {
        if (result == null || result.trim().isEmpty()) {
            return;
        }
        
        // 实时更新录音状态，显示当前识别的内容
        showRecordingStatus("正在识别: " + result);
    }
    
    /**
     * 处理语音识别错误
     * 按照官方文档要求处理错误情况
     */
    private void handleSpeechError(String error) {
        hideRecordingStatus();
        
        String errorMessage = "语音识别失败";
        if (error != null) {
            if (error.contains("网络")) {
                errorMessage = "网络连接失败，请检查网络设置";
            } else if (error.contains("权限")) {
                errorMessage = "录音权限被拒绝，请在设置中开启";
            } else if (error.contains("超时")) {
                errorMessage = "识别超时，请重试";
            } else if (error.contains("没有说话")) {
                errorMessage = "没有检测到语音，请说话后重试";
            } else {
                errorMessage = "识别失败：" + error;
            }
        }
        
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
    }
    
    /**
     * 发送AI请求
     * @param userMessage 用户消息
     */
    private void sendAiRequest(String userMessage) {
        if (aiChatManager != null) {
            Log.d(TAG, "开始发送AI请求，用户消息: " + userMessage);
            
            // 显示AI正在思考的状态
            showAiThinkingStatus();
            
            // 发送消息到AI接口（使用预先设置的监听器）
            aiChatManager.sendMessage(userMessage);
        } else {
            Log.e(TAG, "AiChatManager未初始化");
            Toast.makeText(requireContext(), "AI对话管理器未初始化", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 发送图片识别请求
     * @param imageFile 图片文件
     */
    private void sendImageRecognitionRequest(File imageFile) {
        if (imageRecognitionManager != null) {
            // 显示图片识别状态
            showImageRecognitionStatus();
            
            // 发送图片到AI接口（使用预先设置的监听器）
            imageRecognitionManager.sendImage(imageFile);
        } else {
            Log.e(TAG, "ImageRecognitionManager未初始化");
            Toast.makeText(requireContext(), "图片识别管理器未初始化", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示AI正在思考的状态 - 优化版本
     * 直接创建AI回复卡片，避免后续重复创建
     */
    private void showAiThinkingStatus() {
        if (getView() == null) return;
        
        try {
            // 直接创建AI回复卡片，初始显示"正在思考..."
            currentAiResponseCard = createMessageCard(
                "正在思考...", 
                false,  // AI消息
                R.color.blue_50,  // AI消息背景色
                R.color.text_secondary  // 思考状态文字色
            );
            
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer != null) {
                // 添加AI回复卡片
                chatContainer.addView(currentAiResponseCard);
                
                // 保存文本视图引用，用于流式更新
                if (currentAiResponseCard.getChildCount() > 0) {
                    currentAiResponseTextView = (TextView) currentAiResponseCard.getChildAt(0);
                }
                
                // 滚动到底部
                scrollToBottom();
                
                // 标记为流式状态
                isAiResponseStreaming = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "显示AI思考状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 显示图片识别状态 - 优化版本
     * 直接创建AI回复卡片，避免后续重复创建
     */
    private void showImageRecognitionStatus() {
        if (getView() == null) return;
        
        try {
            // 直接创建AI回复卡片，初始显示"正在识别图片..."
            currentAiResponseCard = createMessageCard(
                "正在识别图片...", 
                false,  // AI消息
                R.color.blue_50,  // AI消息背景色
                R.color.text_secondary  // 识别状态文字色
            );
            
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer != null) {
                // 添加AI回复卡片
                chatContainer.addView(currentAiResponseCard);
                
                // 保存文本视图引用，用于流式更新
                if (currentAiResponseCard.getChildCount() > 0) {
                    currentAiResponseTextView = (TextView) currentAiResponseCard.getChildAt(0);
                }
                
                // 滚动到底部
                scrollToBottom();
                
                // 标记为流式状态
                isAiResponseStreaming = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "显示图片识别状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新AI回复的流式输出 - 优化版本
     * 直接在现有的AI回复卡片上更新内容，避免重复查找
     * @param text 当前流式输出的文本
     */
    private void updateAiResponseStreaming(String text) {
        if (getView() == null || !isAiResponseStreaming) return;
        
        try {
            // 直接使用保存的文本视图引用进行更新
            if (currentAiResponseTextView != null) {
                currentAiResponseTextView.setText(text);
                
                // 滚动到底部
                scrollToBottom();
            }
        } catch (Exception e) {
            Log.e(TAG, "更新AI回复流式输出失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新图片识别结果的流式输出 - 优化版本
     * 直接在现有的AI回复卡片上更新内容，避免重复查找
     * @param text 当前流式输出的文本
     */
    private void updateImageRecognitionStreaming(String text) {
        if (getView() == null || !isAiResponseStreaming) return;
        
        try {
            // 直接使用保存的文本视图引用进行更新
            if (currentAiResponseTextView != null) {
                currentAiResponseTextView.setText(text);
                
                // 滚动到底部
                scrollToBottom();
            }
        } catch (Exception e) {
            Log.e(TAG, "更新图片识别流式输出失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 完成AI回复 - 优化版本
     * 直接在现有的AI回复卡片上更新为最终内容，避免重复创建
     * @param fullResponse 完整的AI回复
     */
    private void completeAiResponse(String fullResponse) {
        // 检查Fragment状态
        if (!isAdded() || getContext() == null || getView() == null) {
            Log.w(TAG, "Fragment状态异常，跳过AI回复处理");
            return;
        }
        
        try {
            // 直接使用现有的AI回复卡片，更新为最终内容
            if (currentAiResponseTextView != null && isAiResponseStreaming) {
                currentAiResponseTextView.setText(fullResponse);
                
                // 更新文字颜色为最终状态
                currentAiResponseTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                
                // 标记流式状态结束
                isAiResponseStreaming = false;
                
                // 创建AI消息对象并添加到对话历史
                Message aiMsg = new Message(fullResponse, false);
                currentConversation.add(aiMsg);
                
                // 保存对话到历史记录
                saveCurrentConversationToHistory();
                
                // 重置AI回复状态
                resetAiResponseState();
            }
        } catch (Exception e) {
            Log.e(TAG, "完成AI回复失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 完成图片识别 - 优化版本
     * 直接在现有的AI回复卡片上更新为最终内容，避免重复创建
     * @param fullResponse 完整的识别结果
     */
    private void completeImageRecognition(String fullResponse) {
        if (getView() == null) return;
        
        try {
            // 直接使用现有的AI回复卡片，更新为最终内容
            if (currentAiResponseTextView != null && isAiResponseStreaming) {
                currentAiResponseTextView.setText(fullResponse);
                
                // 更新文字颜色为最终状态
                currentAiResponseTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                
                // 标记流式状态结束
                isAiResponseStreaming = false;
                
                // 创建AI消息对象并添加到对话历史
                Message aiMsg = new Message(fullResponse, false);
                currentConversation.add(aiMsg);
                
                // 保存对话到历史记录
                saveCurrentConversationToHistory();
                
                // 重置AI回复状态
                resetAiResponseState();
            }
        } catch (Exception e) {
            Log.e(TAG, "完成图片识别失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理AI回复错误 - 优化版本
     * 直接在现有的AI回复卡片上显示错误信息，避免重复创建
     * @param error 错误信息
     */
    private void handleAiResponseError(String error) {
        if (getView() == null) return;
        
        try {
            // 直接使用现有的AI回复卡片，显示错误信息
            if (currentAiResponseTextView != null && isAiResponseStreaming) {
                String errorMessage = "抱歉，AI回复出现错误：" + error;
                currentAiResponseTextView.setText(errorMessage);
                
                // 更新文字颜色为错误状态
                currentAiResponseTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                
                // 标记流式状态结束
                isAiResponseStreaming = false;
                
                // 创建错误消息对象并添加到对话历史
                Message errorMsg = new Message(errorMessage, false);
                currentConversation.add(errorMsg);
                
                // 保存对话到历史记录
                saveCurrentConversationToHistory();
                
                // 重置AI回复状态
                resetAiResponseState();
            }
        } catch (Exception e) {
            Log.e(TAG, "处理AI回复错误失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理图片识别错误
     * @param error 错误信息
     */
    /**
     * 处理图片识别错误 - 优化版本
     * 直接在现有的AI回复卡片上显示错误信息，避免重复创建
     * @param error 错误信息
     */
    private void handleImageRecognitionError(String error) {
        if (getView() == null) return;
        
        try {
            // 直接使用现有的AI回复卡片，显示错误信息
            if (currentAiResponseTextView != null && isAiResponseStreaming) {
                String errorMessage = "抱歉，图片识别出现错误：" + error;
                currentAiResponseTextView.setText(errorMessage);
                
                // 更新文字颜色为错误状态
                currentAiResponseTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                
                // 标记流式状态结束
                isAiResponseStreaming = false;
                
                // 创建错误消息对象并添加到对话历史
                Message errorMsg = new Message(errorMessage, false);
                currentConversation.add(errorMsg);
                
                // 保存对话到历史记录
                saveCurrentConversationToHistory();
                
                // 重置AI回复状态
                resetAiResponseState();
            }
        } catch (Exception e) {
            Log.e(TAG, "处理图片识别错误失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新语音按钮状态
     * 提供用户反馈
     */
    private void updateVoiceButtonState(boolean listening) {
        isListening = listening;
        if (listening) {
            btnVoice.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
        } else {
            btnVoice.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary_blue));
        }
    }
    

    
    /**
     * 添加用户消息到对话界面
     * 动态添加消息的逻辑
     */
    private void addUserMessage(String message) {
        if (getView() == null) return;
        
        try {
            // 创建用户消息
            Message userMsg = new Message(message, true);
            currentConversation.add(userMsg);
            
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 创建用户消息卡片
            com.google.android.material.card.MaterialCardView userMessageCard = createMessageCard(
                message, 
                true,  // 用户消息
                R.color.primary_blue,  // 用户消息背景色
                R.color.white  // 用户消息文字色
            );
            
            // 添加到对话容器
            chatContainer.addView(userMessageCard);
            
            // 滚动到底部
            scrollToBottom();
            
            // 添加消息显示完成的回调，确保UI完全更新后再触发后续操作
            userMessageCard.post(() -> {
                // 消息卡片完全显示后的回调
                onUserMessageDisplayed(message);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "添加用户消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 添加图片消息到对话界面
     */
    private void addImageMessage(File imageFile) {
        if (getView() == null) return;
        
        try {
            // 创建图片消息
            Message imageMsg = new Message("[图片]", true);
            currentConversation.add(imageMsg);
            
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 创建图片消息卡片
            com.google.android.material.card.MaterialCardView imageMessageCard = createImageMessageCard(imageFile);
            
            // 添加到对话容器
            chatContainer.addView(imageMessageCard);
            
            // 滚动到底部
            scrollToBottom();
            
        } catch (Exception e) {
            Log.e(TAG, "添加图片消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 用户消息显示完成后的回调
     * 在这里可以确保用户消息完全显示后再进行后续操作
     */
    private void onUserMessageDisplayed(String message) {
        // 可以在这里添加额外的逻辑，比如动画效果等
        Log.d(TAG, "用户消息已完全显示: " + message);
    }
    
    /**
     * 添加AI回复消息
     */
    public void addAIResponse(String response) {
        // 检查Fragment状态
        if (!isAdded() || getContext() == null || getView() == null) {
            Log.w(TAG, "Fragment状态异常，跳过添加AI回复");
            return;
        }
        
        try {
            // 创建AI消息
            Message aiMsg = new Message(response, false);
            currentConversation.add(aiMsg);
            
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 创建AI回复卡片
            com.google.android.material.card.MaterialCardView aiMessageCard = createMessageCard(
                response, 
                false,  // AI消息
                R.color.blue_50,  // AI消息背景色
                R.color.text_primary  // AI消息文字色
            );
            
            // 添加到对话容器
            chatContainer.addView(aiMessageCard);
            
            // 滚动到底部
            scrollToBottom();
            
            // 保存对话到历史记录
            saveCurrentConversationToHistory();
            
        } catch (Exception e) {
            Log.e(TAG, "添加AI回复失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 保存当前对话到历史记录
     */
    private void saveCurrentConversationToHistory() {
        if (currentConversation.size() >= 2) { // 至少有一问一答
            Conversation conversation = new Conversation(currentConversationId, new ArrayList<>(currentConversation));
            conversationHistory.add(0, conversation); // 添加到开头
            saveConversationHistory();
            
            // 创建新的对话ID
            currentConversationId = UUID.randomUUID().toString();
            currentConversation.clear();
        }
    }
    
    /**
     * 清空当前对话（退出登录时调用）
     */
    public void clearCurrentConversation() {
        try {
            // 清空当前对话
            currentConversation.clear();
            currentConversationId = UUID.randomUUID().toString();
            
            // 清除存储的当前对话
            try {
                SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(KEY_CURRENT_CONVERSATION);
                editor.apply();
            } catch (Exception e) {
                Log.e(TAG, "清除当前对话存储失败", e);
            }
            
            // 清空对话UI
            clearConversationUI();
            
            Log.d(TAG, "当前对话已清除");
        } catch (Exception e) {
            Log.e(TAG, "清除对话失败", e);
        }
    }
    
    /**
     * 创建消息卡片
     */
    private com.google.android.material.card.MaterialCardView createMessageCard(String message, boolean isUser, int bgColor, int textColor) {
        // 检查Fragment状态
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment状态异常，无法创建消息卡片");
            return null;
        }
        
        // 创建卡片容器
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(requireContext());
        
        // 设置卡片属性
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        
        if (isUser) {
            // 用户消息靠右显示
            cardParams.gravity = android.view.Gravity.END;
            cardParams.setMargins(56, 8, 16, 8);
        } else {
            // AI消息靠左显示
            cardParams.gravity = android.view.Gravity.START;
            cardParams.setMargins(16, 8, 56, 8);
        }
        
        card.setLayoutParams(cardParams);
        card.setRadius(18);
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), bgColor));
        card.setStrokeWidth(0);
        card.setCardElevation(2);
        
        // 创建文本视图
        TextView textView = new TextView(requireContext());
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textView.setLayoutParams(textParams);
        textView.setText(message);
        textView.setTextColor(ContextCompat.getColor(requireContext(), textColor));
        textView.setTextSize(15);
        
        // 设置对话框宽度为屏幕宽度的75%
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int dialogWidth = (int) (screenWidth * 0.75);
        textView.setMaxWidth(dialogWidth);
        
        textView.setPadding(12, 12, 12, 12);
        textView.setLineSpacing(2, 1);
        
        // 将文本添加到卡片
        card.addView(textView);
        
        return card;
    }
    
    /**
     * 创建图片消息卡片
     */
    private com.google.android.material.card.MaterialCardView createImageMessageCard(File imageFile) {
        // 创建卡片容器
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(requireContext());
        
        // 设置卡片属性
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        
        // 用户图片消息靠右显示
        cardParams.gravity = android.view.Gravity.END;
        cardParams.setMargins(56, 8, 16, 8);
        
        card.setLayoutParams(cardParams);
        card.setRadius(18);
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_blue));
        card.setStrokeWidth(0);
        card.setCardElevation(2);
        
        // 创建垂直布局容器
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(12, 12, 12, 12);
        
        // 创建图片视图
        ImageView imageView = new ImageView(requireContext());
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        imageView.setLayoutParams(imageParams);
        
        // 设置图片
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap != null) {
                // 压缩图片以适应显示
                int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.6);
                int maxHeight = 400;
                
                float scale = Math.min(
                    (float) maxWidth / bitmap.getWidth(),
                    (float) maxHeight / bitmap.getHeight()
                );
                
                if (scale < 1) {
                    int newWidth = Math.round(bitmap.getWidth() * scale);
                    int newHeight = Math.round(bitmap.getHeight() * scale);
                    bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                }
                
                imageView.setImageBitmap(bitmap);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        } catch (Exception e) {
            Log.e(TAG, "加载图片失败", e);
            // 如果加载失败，显示占位符
            imageView.setImageResource(R.drawable.icon_camera_switch);
        }
        
        // 添加图片到容器
        container.addView(imageView);
        
        // 将容器添加到卡片
        card.addView(container);
        
        return card;
    }
    
    /**
     * 切换到当前对话标签页
     */
    private void switchToCurrentTab() {
        if (isCurrentTabSelected) return;
        
        isCurrentTabSelected = true;
        updateTabAppearance();
        showCurrentConversation();
    }
    
    /**
     * 切换到历史记录标签页
     */
    private void switchToHistoryTab() {
        if (!isCurrentTabSelected) return;
        
        isCurrentTabSelected = false;
        updateTabAppearance();
        showHistoryConversation();
    }
    
    /**
     * 更新标签页外观
     */
    private void updateTabAppearance() {
        if (tabCurrentConversation != null && tabHistory != null) {
            if (isCurrentTabSelected) {
                tabCurrentConversation.setBackgroundResource(R.drawable.bg_tab_selected);
                tabCurrentConversation.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue));
                tabHistory.setBackgroundResource(R.drawable.bg_tab_unselected);
                tabHistory.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            } else {
                tabCurrentConversation.setBackgroundResource(R.drawable.bg_tab_unselected);
                tabCurrentConversation.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                tabHistory.setBackgroundResource(R.drawable.bg_tab_selected);
                tabHistory.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue));
            }
        }
    }
    
    /**
     * 显示当前对话内容
     */
    private void showCurrentConversation() {
        if (getView() == null) return;
        
        try {
            // 获取滚动容器
            androidx.core.widget.NestedScrollView scrollView = getView().findViewById(R.id.scroll_content);
            if (scrollView == null) return;
            
            // 检查是否有历史记录容器，如果有则移除
            View historyContainer = null;
            for (int i = 0; i < scrollView.getChildCount(); i++) {
                View child = scrollView.getChildAt(i);
                if ("history_container".equals(child.getTag())) {
                    historyContainer = child;
                    break;
                }
            }
            
            if (historyContainer != null) {
                scrollView.removeView(historyContainer);
            }
            
            // 恢复原有的对话内容
            Object tag = scrollView.getTag(R.id.scroll_content);
            if (tag instanceof View) {
                View originalContent = (View) tag;
                scrollView.addView(originalContent);
                scrollView.setTag(R.id.scroll_content, null); // 清除引用
            }
            
            // 显示当前对话内容
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer != null) {
                // 显示建议卡片和示例对话
                chatContainer.setVisibility(View.VISIBLE);
            }
            
            // 更新标题
            if (tvConversationTitle != null) {
                tvConversationTitle.setText("当前对话");
            }
        } catch (Exception e) {
            Log.e(TAG, "显示当前对话失败", e);
        }
    }
    
    /**
     * 显示历史对话内容
     */
    private void showHistoryConversation() {
        if (getView() == null) return;
        
        try {
            // 隐藏当前对话内容
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer != null) {
                chatContainer.setVisibility(View.GONE);
            }
            
            // 显示历史记录内容
            showHistoryContent();
            
            // 更新标题
            if (tvConversationTitle != null) {
                tvConversationTitle.setText("历史记录");
            }
            

        } catch (Exception e) {
            Log.e(TAG, "显示历史对话失败", e);
        }
    }
    
    /**
     * 显示历史记录内容
     */
    private void showHistoryContent() {
        if (getView() == null) return;
        
        try {
            // 获取滚动容器
            androidx.core.widget.NestedScrollView scrollView = getView().findViewById(R.id.scroll_content);
            if (scrollView == null) return;
            
            // 保存原有的对话内容
            View originalContent = null;
            if (scrollView.getChildCount() > 0) {
                originalContent = scrollView.getChildAt(0);
                scrollView.removeView(originalContent);
            }
            
            // 创建历史记录容器
            LinearLayout historyContainer = new LinearLayout(requireContext());
            historyContainer.setOrientation(LinearLayout.VERTICAL);
            historyContainer.setPadding(16, 12, 12, 12);
            historyContainer.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
            historyContainer.setTag("history_container"); // 标记为历史记录容器
            
            if (conversationHistory.isEmpty()) {
                // 没有历史记录时显示提示
                TextView noHistoryText = new TextView(requireContext());
                noHistoryText.setText("暂无对话记录");
                noHistoryText.setTextSize(16);
                noHistoryText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                noHistoryText.setGravity(android.view.Gravity.CENTER);
                noHistoryText.setPadding(32, 64, 32, 64);
                historyContainer.addView(noHistoryText);
            } else {
                // 显示历史记录列表
                for (Conversation conv : conversationHistory) {
                    View historyItem = createHistoryItemView(conv);
                    historyContainer.addView(historyItem);
                    
                    // 添加分割线
                    if (conversationHistory.indexOf(conv) < conversationHistory.size() - 1) {
                        View divider = new View(requireContext());
                        divider.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1);
                        params.setMargins(0, 16, 0, 16);
                        divider.setLayoutParams(params);
                        historyContainer.addView(divider);
                    }
                }
            }
            
            // 将历史记录容器添加到滚动视图
            scrollView.addView(historyContainer);
            
            // 保存原始内容的引用，以便后续恢复
            scrollView.setTag(R.id.scroll_content, originalContent);
            
        } catch (Exception e) {
            Log.e(TAG, "显示历史记录内容失败", e);
        }
    }
    
    /**
     * 收起消息面板
     */
    private void collapseMessagePanel() {
        if (messagePanel == null) return;
        
        // 创建收起动画 - 透明度从1变为0
        android.view.animation.AlphaAnimation alphaAnimation = new android.view.animation.AlphaAnimation(1.0f, 0.0f);
        alphaAnimation.setDuration(300);
        alphaAnimation.setFillAfter(true);
        
        alphaAnimation.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {}
            
            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                messagePanel.setVisibility(View.GONE);
                btnExpandMessage.setVisibility(View.VISIBLE);
                isMessagePanelExpanded = false;
                
                // 重置宽度为0
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagePanel.getLayoutParams();
                params.width = 0;
                messagePanel.setLayoutParams(params);
            }
            
            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {}
        });
        
        messagePanel.startAnimation(alphaAnimation);
    }
    
    /**
     * 展开消息面板
     */
    private void expandMessagePanel() {
        if (messagePanel == null) return;
        
        btnExpandMessage.setVisibility(View.GONE);
        messagePanel.setVisibility(View.VISIBLE);
        
        // 动态设置宽度为屏幕宽度的80%
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int panelWidth = (int) (screenWidth * 0.8);
        
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagePanel.getLayoutParams();
        params.width = panelWidth;
        messagePanel.setLayoutParams(params);
        
        // 创建展开动画 - 透明度从0变为1
        android.view.animation.AlphaAnimation alphaAnimation = new android.view.animation.AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(300);
        alphaAnimation.setFillAfter(true);
        
        messagePanel.startAnimation(alphaAnimation);
        isMessagePanelExpanded = true;
        
        // 确保当前标签页内容正确显示
        if (isCurrentTabSelected) {
            showCurrentConversation();
        } else {
            showHistoryConversation();
        }
    }
    
    /**
     * 滚动到底部
     */
    private void scrollToBottom() {
        try {
            if (getView() != null) {
                androidx.core.widget.NestedScrollView scrollView = getView().findViewById(R.id.scroll_content);
                if (scrollView != null) {
                    scrollView.post(() -> {
                        scrollView.fullScroll(android.view.View.FOCUS_DOWN);
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "滚动到底部失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止状态检查定时器
        stopStatusCheck();
        
        // 按照官方文档要求，销毁时释放语音识别资源
        if (speechManager != null) {
            speechManager.destroy();
        }
        
        // 释放AI对话管理器资源
        if (aiChatManager != null) {
            aiChatManager.destroy();
        }
        
        // 释放图片识别管理器资源
        if (imageRecognitionManager != null) {
            imageRecognitionManager.destroy();
        }
        
        // 释放TTS资源
        if (ttsManager != null) {
            ttsManager.destroy();
        }
        
        // 释放摄像头资源
        if (cameraManager != null) {
            try {
                cameraManager.release();
            } catch (Exception e) {
                Log.e(TAG, "释放摄像头资源失败", e);
            }
        }
        
        // 清理智能播报资源
        if (smartTimerHandler != null) {
            smartTimerHandler.removeCallbacksAndMessages(null);
            smartTimerHandler = null;
            Log.d(TAG, "智能播报Handler已清理");
        }
        if (streamingPlaybackHandler != null) {
            streamingPlaybackHandler.removeCallbacksAndMessages(null);
            streamingPlaybackHandler = null;
            Log.d(TAG, "流式播报Handler已清理");
        }
        resetSmartPlaybackState();
        
        // 清理AI避障Handler
        if (aiAvoidHandler != null) {
            aiAvoidHandler.removeCallbacksAndMessages(null);
            aiAvoidHandler = null;
            Log.d(TAG, "AI避障Handler已清理");
        }
        
        // 清理状态检查Handler
        if (statusCheckHandler != null) {
            statusCheckHandler.removeCallbacksAndMessages(null);
            statusCheckHandler = null;
            Log.d(TAG, "状态检查Handler已清理");
        }
        
        // 清理AI避障资源（但不影响WebSocket连接）
        if (isAIAvoidRunning) {
            // 只停止AI避障功能，不设置WebSocket状态
            stopAIAvoidanceWithoutWebSocketChange();
        }
        
        // 清理WebSocket监听器（保持连接）
        if (webSocketManager != null) {
            try {
                // 移除消息监听器
                if (webSocketMessageListener != null) {
                    webSocketManager.removeMessageListener(webSocketMessageListener);
                    Log.d(TAG, "WebSocket消息监听器已清理");
                }
                
                // 移除连接状态监听器
                if (webSocketConnectionListener != null) {
                    webSocketManager.removeConnectionStatusListener(webSocketConnectionListener);
                    Log.d(TAG, "WebSocket连接状态监听器已清理");
                }
                
                // 清理重连相关资源
                if (webSocketReconnectHandler != null && webSocketReconnectRunnable != null) {
                    webSocketReconnectHandler.removeCallbacks(webSocketReconnectRunnable);
                    Log.d(TAG, "WebSocket重连资源已清理");
                }
                
                // 不设置setMatchingStatus(false)，保持WebSocket长连接
                Log.d(TAG, "WebSocket连接保持活跃，不设置业务状态");
                
            } catch (Exception e) {
                Log.e(TAG, "清理WebSocket监听器失败", e);
            }
        }
        
        // 清理障碍物检测TTS管理器
        if (obstacleDetectionTTSManager != null) {
            try {
                obstacleDetectionTTSManager.destroy();
                Log.d(TAG, "障碍物检测TTS管理器已清理");
            } catch (Exception e) {
                Log.e(TAG, "清理障碍物检测TTS管理器失败", e);
            }
        }
    }
    
    /**
     * 安全停止所有活动并释放资源
     * 采用渐进式停止策略，避免强制关闭导致软件崩溃
     */
    private void stopAllActivitiesAndReleaseResources() {
        Log.d(TAG, "开始安全停止所有活动并释放资源...");
        
        try {
            // 1. 安全停止AI避障功能（不销毁，只停止）
            if (isAIAvoidRunning) {
                Log.d(TAG, "安全停止AI避障功能");
                try {
                    // 只停止检测，不销毁资源
                    isAIAvoidRunning = false;
                    Log.d(TAG, "AI避障功能已停止");
                } catch (Exception e) {
                    Log.w(TAG, "停止AI避障功能失败", e);
                }
            }
            
            // 2. 安全停止TTS播报（不销毁，只停止）
            if (obstacleDetectionTTSManager != null) {
                Log.d(TAG, "安全停止障碍物检测TTS管理器");
                try {
                    // 只停止播报，不销毁管理器
                    obstacleDetectionTTSManager.stopSpeaking();
                    Log.d(TAG, "TTS播报已停止");
                } catch (Exception e) {
                    Log.w(TAG, "停止TTS播报失败", e);
                }
            }
            
            // 3. 安全停止AI对话管理器（不关闭线程池，只停止流式输出）
            if (aiChatManager != null) {
                Log.d(TAG, "安全停止AI对话管理器");
                try {
                    // 只停止流式输出，不销毁线程池
                    aiChatManager.stopStreaming();
                    Log.d(TAG, "AI对话流式输出已停止");
                } catch (Exception e) {
                    Log.w(TAG, "停止AI对话管理器失败", e);
                }
            }
            
            // 4. 安全停止摄像头预览
            if (cameraManager != null) {
                Log.d(TAG, "安全停止摄像头预览");
                try {
                    cameraManager.stopPreview();
                    Log.d(TAG, "摄像头预览已停止");
                } catch (Exception e) {
                    Log.w(TAG, "停止摄像头预览失败", e);
                }
            }
            
            // 5. 保持WebSocket连接（不设置状态，保持长连接）
            if (webSocketManager != null) {
                Log.d(TAG, "保持WebSocket连接状态");
                try {
                    // 不设置业务状态，保持WebSocket长连接
                    Log.d(TAG, "WebSocket连接保持活跃状态");
                } catch (Exception e) {
                    Log.w(TAG, "检查WebSocket状态失败", e);
                }
            }
            
            // 6. 等待所有停止操作完成
            Log.d(TAG, "等待所有停止操作完成...");
            try {
                Thread.sleep(500); // 等待500ms，让所有停止操作完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 7. 检查停止状态
            Log.d(TAG, "检查各功能停止状态...");
            Log.d(TAG, "AI避障状态: " + (isAIAvoidRunning ? "运行中" : "已停止"));
            Log.d(TAG, "TTS播报状态: " + (obstacleDetectionTTSManager != null ? "管理器存在" : "管理器不存在"));
            Log.d(TAG, "AI对话状态: " + (aiChatManager != null ? "管理器存在" : "管理器不存在"));
            Log.d(TAG, "WebSocket状态: " + (isAIFunctionActive ? "活跃" : "非活跃"));
            
            Log.d(TAG, "所有活动已安全停止，资源状态已设置，等待跳转完成");
            
        } catch (Exception e) {
            Log.e(TAG, "安全停止活动时发生异常", e);
        }
    }
    
    // ===== AI避障功能方法 =====
    
    /**
     * 初始化AI避障功能
     */
    private void initObstacleDetection() {
        try {
            // 初始化API服务
            apiService = ObstacleDetectionRetrofitClient.getInstance().createService(ApiService.class);
            
            // 初始化障碍物检测TTS管理器
            obstacleDetectionTTSManager = new ObstacleDetectionTTSManager(requireContext());
            
            Log.d(TAG, "障碍物检测功能初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化障碍物检测功能失败", e);
        }
    }
    
    /**
     * 启动AI避障检测
     */
    private void startObstacleDetection() {
        if (isSessionStarted) {
            Log.d(TAG, "AI避障会话已开启，跳过重复开启");
            return;
        }
        
        Log.d(TAG, "开始AI避障检测...");
        startDetectionSession();
    }
    
    /**
     * 开启检测会话
     */
    private void startDetectionSession() {
        if (apiService != null) {
            Log.d(TAG, "开始调用startObstacleDetectionSession API...");
            Call<String> sessionCall = apiService.startObstacleDetectionSession();
            
            sessionCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            // 使用JSONObject正确解析JSON
                            org.json.JSONObject jsonResponse = new org.json.JSONObject(response.body());
                            
                            if (jsonResponse.optBoolean("success", false)) {
                                mSessionId = jsonResponse.optString("session_id", "");
                                
                                if (mSessionId != null && !mSessionId.isEmpty()) {
                                    Log.d(TAG, "AI避障会话创建成功: " + mSessionId);
                                    isSessionStarted = true;
                                    
                                    // 立即执行第一次检测
                                    processCurrentFrame();
                                    
                                    // 开始定时检测
                                    startAIFrameProcessing();
                                    
                                    // 显示启动成功提示
                                    showDetectionMessage("AI避障检测已启动", "success");
                                    
                                    // 播报启动成功提示
                                    if (ttsManager != null && isAdded() && getContext() != null) {
                                        try {
                                            Log.d(TAG, "开始播报启动提示...");
                                            ttsManager.startSpeaking("启动AI避障功能成功，实时检测过程中请保持机身平稳");
                                        } catch (Exception e) {
                                            Log.w(TAG, "TTS播报启动提示失败", e);
                                        }
                                    } else {
                                        Log.e(TAG, "ttsManager为null或Fragment状态异常，无法播报启动提示");
                                    }
                                } else {
                                    Log.e(TAG, "会话ID为空，启动失败");
                                    isAIAvoidRunning = false;
                                }
                            } else {
                                Log.e(TAG, "API返回失败，启动失败");
                                isAIAvoidRunning = false;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析会话响应失败", e);
                            isAIAvoidRunning = false;
                        }
                    } else {
                        Log.e(TAG, "API响应无效，启动失败");
                        isAIAvoidRunning = false;
                    }
                }
                
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e(TAG, "创建AI避障会话失败", t);
                    isAIAvoidRunning = false;
                    showDetectionMessage("AI避障启动失败", "error");
                }
            });
        } else {
            Log.e(TAG, "API服务未初始化，启动失败");
            isAIAvoidRunning = false;
        }
    }
    
    /**
     * 启动AI避障专用帧处理
     */
    private void startAIFrameProcessing() {
        try {
            // 确保aiAvoidHandler已初始化
            if (aiAvoidHandler == null) {
                aiAvoidHandler = new Handler(Looper.getMainLooper());
            }
            
            aiAvoidRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (isAIAvoidRunning && mSessionId != null) {
                            // 每6.5秒处理一帧
                            processCurrentFrame();
                            // 继续循环
                            if (aiAvoidHandler != null && isAIAvoidRunning) {
                                aiAvoidHandler.postDelayed(this, 6500);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "AI避障帧处理循环失败", e);
                    }
                }
            };
            
            if (aiAvoidHandler != null) {
                aiAvoidHandler.postDelayed(aiAvoidRunnable, 6500);
            }
        } catch (Exception e) {
            Log.e(TAG, "启动AI避障帧处理失败", e);
        }
    }
    
    /**
     * 停止AI避障帧处理
     */
    private void stopAIFrameProcessing() {
        try {
            if (aiAvoidHandler != null && aiAvoidRunnable != null) {
                aiAvoidHandler.removeCallbacks(aiAvoidRunnable);
                aiAvoidRunnable = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "停止AI避障帧处理失败", e);
        }
    }
    

    
    /**
     * 处理当前帧
     */
    private void processCurrentFrame() {
        try {
            // 检查AI避障是否在运行
            if (!isAIAvoidRunning) {
                return;
            }
            
            // 检查会话ID
            if (mSessionId == null) {
                Log.e(TAG, "会话ID无效，跳过帧处理");
                return;
            }
            
            // 双重检查：确保状态一致性
            if (!isSessionStarted) {
                return;
            }
            
            // 获取当前帧图像并转换为Base64
            String imageBase64 = getCurrentFrameBase64();
            
            if (imageBase64 == null) {
                Log.w(TAG, "图像获取失败，跳过帧处理");
                return;
            }
            
            // 调用真实API进行障碍物检测
            processFrameWithAPI(imageBase64);
            
        } catch (Exception e) {
            Log.e(TAG, "帧处理失败", e);
            // 显示错误信息
            showDetectionMessage("获取图像数据失败: " + e.getMessage(), "error");
        }
    }
    
    /**
     * 获取当前帧的Base64编码
     */
    private String getCurrentFrameBase64() {
        try {
            // 检查摄像头状态
            if (!isCameraAvailable()) {
                return null;
            }
            
            // 检查摄像头管理器是否可用
            if (cameraManager == null || !cameraManager.isPreviewActive()) {
                return null;
            }
            
            return takePhotoSync();
            
        } catch (IllegalStateException e) {
            isCameraOpen = false;
            return null;
        } catch (Exception e) {
            Log.e(TAG, "获取摄像头图像失败", e);
            return null;
        }
    }
    
    /**
     * 同步拍照并返回Base64
     */
    private String takePhotoSync() {
        final Object photoLock = new Object();
        final String[] result = new String[1];
        final boolean[] isCompleted = {false};
        
        try {
            // 再次检查相机状态
            if (!isCameraAvailable()) {
                return null;
            }
            
            cameraManager.takePhoto(new com.swj.shiwujie.common.utils.CameraPreviewManager.TakePhotoCallback() {
                @Override
                public void onPhotoTaken(byte[] data) {
                    try {
                        String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(data, Base64.DEFAULT);
                        result[0] = base64Image;
                    } catch (Exception e) {
                        result[0] = null;
                    }
                    synchronized (photoLock) {
                        isCompleted[0] = true;
                        photoLock.notify();
                    }
                }
                
                @Override
                public void onError(String error) {
                    result[0] = null;
                    synchronized (photoLock) {
                        isCompleted[0] = true;
                        photoLock.notify();
                    }
                }
            });
            
            synchronized (photoLock) {
                if (!isCompleted[0]) {
                    photoLock.wait(1500);
                }
            }
            
            if (result[0] != null) {
                return result[0];
            } else {
                return null;
            }
            
        } catch (IllegalStateException e) {
            isCameraOpen = false;
            return null;
        } catch (Exception e) {
            Log.e(TAG, "拍照异常", e);
            return null;
        }
    }
    
    /**
     * 调用API处理图像帧
     */
    private void processFrameWithAPI(String imageBase64) {
        // 再次检查状态，确保在API调用过程中状态没有改变
        if (!isAIAvoidRunning || !isSessionStarted) {
            return;
        }
        
        if (apiService != null && mSessionId != null) {
            try {
                // 使用JSONObject正确构建JSON请求体
                org.json.JSONObject jsonObject = new org.json.JSONObject();
                jsonObject.put("session_id", mSessionId);
                jsonObject.put("image", imageBase64);
                
                String jsonBody = jsonObject.toString();
                
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(mediaType, jsonBody);
                
                Call<String> processCall = apiService.processFrameForObstacleDetection(requestBody);
                
                processCall.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        // 检查AI避障是否仍在运行
                        if (!isAIAvoidRunning || !isSessionStarted) {
                            return;
                        }
                        
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String responseBody = response.body();
                                
                                // 再次检查状态
                                if (!isAIAvoidRunning || !isSessionStarted) {
                                    return;
                                }
                                
                                // 检查是否成功
                                if (responseBody.contains("\"success\":true")) {
                                    // 解析检测结果并显示
                                    parseAndDisplayResult(responseBody);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "解析API响应失败", e);
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        // 检查AI避障是否仍在运行
                        if (!isAIAvoidRunning || !isSessionStarted) {
                            return;
                        }
                        Log.e(TAG, "API网络请求失败", t);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "构建JSON请求体失败", e);
            }
        } else {
            Log.e(TAG, "apiService为null或mSessionId无效，无法处理图像帧");
        }
    }
    
    /**
     * 解析并显示检测结果
     */
    private void parseAndDisplayResult(String responseBody) {
        try {
            org.json.JSONObject jsonResponse = new org.json.JSONObject(responseBody);
            
            // ===== 检测结果去重逻辑 =====
            String currentDetectionHash = generateDetectionHash(jsonResponse);
            
            // 转换当前检测结果为ObstacleDetectionData对象
            ObstacleDetectionData currentDetectionData = convertJsonToObstacleDetectionData(jsonResponse);
            if (currentDetectionData == null) {
                Log.w(TAG, "无法转换检测结果数据");
                return;
            }
            
            // 将当前检测结果添加到历史记录中
            detectionHistory.add(currentDetectionData);
            
            // 保持历史记录在合理范围内（最多保存10条）
            if (detectionHistory.size() > 10) {
                detectionHistory.remove(0);
            }
            
            if (currentDetectionHash.equals(lastDetectionHash)) {
                Log.d(TAG, "检测结果与上次相同，尝试播报最近两条未播报的信息");
                
                // 播报最近两条未播报的信息
                broadcastRecentUnbroadcastedInfo();
                return;
            }
            
            // 更新上次检测结果哈希
            lastDetectionHash = currentDetectionHash;
            
            // 标记当前检测结果已播报
            broadcastedHashes.add(currentDetectionHash);
            
            // 解析检测到的对象
            String detectedObjects = "未检测到障碍物";
            if (jsonResponse.has("detected_objects")) {
                Object objects = jsonResponse.get("detected_objects");
                if (objects instanceof org.json.JSONArray) {
                    org.json.JSONArray array = (org.json.JSONArray) objects;
                    if (array.length() > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < array.length(); i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(array.getString(i));
                        }
                        detectedObjects = "检测到: " + sb.toString();
                    }
                }
            }
            
            // 解析最近障碍物
            String nearestObstacle = "";
            if (jsonResponse.has("nearest_unknown_obstacle")) {
                nearestObstacle = jsonResponse.getString("nearest_unknown_obstacle");
            }
            
            // ===== 使用优化版ObstacleDetectionTTSManager =====
            // 只有在新检测结果时才播报，避免与broadcastRecentUnbroadcastedInfo重复
            if (currentDetectionData != null) {
                obstacleDetectionTTSManager.processAndSpeak(currentDetectionData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "解析检测结果失败", e);
        }
    }
    

    

    

    

    

    
    /**
     * 显示检测消息（弹幕效果）
     */
    private void showDetectionMessage(String message, String type) {
        try {
            // 在页面左侧中间显示弹幕效果
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // 使用Toast显示检测结果
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "显示检测消息失败", e);
        }
    }
    
    /**
     * 生成检测结果的哈希值，用于去重
     * @param jsonResponse 检测结果JSON对象
     * @return 哈希值字符串
     */
    private String generateDetectionHash(org.json.JSONObject jsonResponse) {
        try {
            StringBuilder hashBuilder = new StringBuilder();
            
            // 提取关键检测信息
            if (jsonResponse.has("detected_objects")) {
                    org.json.JSONArray detectedObjects = jsonResponse.getJSONArray("detected_objects");
                hashBuilder.append("objects:");
                        for (int i = 0; i < detectedObjects.length(); i++) {
                    try {
                        org.json.JSONObject obj = detectedObjects.getJSONObject(i);
                        String className = obj.optString("class_name", "");
                        double distance = obj.optDouble("distance_m", 0.0);
                        // 距离精度到0.1米，避免微小变化
                        hashBuilder.append(className).append(":").append(String.format("%.1f", distance)).append(",");
                            } catch (Exception e) {
                        // 忽略单个对象解析错误
                    }
                }
            }
            
            if (jsonResponse.has("nearest_unknown_obstacle")) {
                try {
                    org.json.JSONObject obs = jsonResponse.getJSONObject("nearest_unknown_obstacle");
                    double distance = obs.optDouble("distance_m", 0.0);
                    // 距离精度到0.1米
                    hashBuilder.append("unknown:").append(String.format("%.1f", distance));
                            } catch (Exception e) {
                    // 忽略解析错误
                }
            }
            
            // 如果没有检测到任何内容，返回特殊标识
            if (hashBuilder.length() == 0) {
                return "no_detection";
            }
            
            return hashBuilder.toString();
            
                } catch (Exception e) {
            Log.e(TAG, "生成检测结果哈希失败", e);
            return "error_hash";
        }
    }
    
    /**
     * 初始化WebSocket监听
     * 
     * AI页面只需要监听后端消息，不需要发送任何socket消息
     * 监听的消息类型：
     * - 5001: REQUEST_TYPE_AI_PHOTO_RECOGNITION - 启动AI页面的拍照识别按钮和功能
     * - 5002: REQUEST_TYPE_JUMP_TO_BLINDHOME - 跳转到blindhome页面并开启连线志愿者按钮和功能
     * - 5003: REQUEST_TYPE_EMERGENCY_HELP - 紧急求助，跳转到blindhome页面并开启紧急求助功能
     * - 5004: REQUEST_TYPE_APP_JUMP - APP跳转 (已注释)
     * - 5005: REQUEST_TYPE_EDIT_PROFILE - 跳转到用户信息修改页面
     */
    private void initWebSocketListener() {
        try {
            webSocketManager = WebSocketManager.getInstance();
            
            // 创建WebSocket消息监听器
            webSocketMessageListener = new WebSocketManager.MessageListener() {
                @Override
                public void onMessageReceived(SocketDataV0 data) {
                    handleWebSocketMessage(data);
                }
            };
            
            // 创建WebSocket连接状态监听器
            webSocketConnectionListener = new WebSocketManager.ConnectionStatusListener() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "WebSocket连接成功");
                    
                    // 在主线程中显示连接成功提示
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                              /*  Toast.makeText(requireContext(), "网络连接已恢复", Toast.LENGTH_SHORT).show();*/
                            } catch (Exception e) {
                                Log.w(TAG, "显示连接成功提示失败", e);
                            }
                        });
                    }
                    
                    // 连接成功后，如果AI功能正在运行，立即更新业务状态
                    if (isAIAvoidRunning && webSocketManager != null) {
                        webSocketManager.setMatchingStatus(true);
                        Log.d(TAG, "WebSocket重连成功，恢复AI功能业务状态");
                    }
                    // 重置重连计数
                    resetWebSocketReconnectCount();
                }
                
                @Override
                public void onDisconnected(int code, String reason) {
                    Log.w(TAG, "WebSocket连接断开: code=" + code + ", reason=" + reason);
                    // 连接断开时，如果AI功能正在运行，尝试重连
                    if (isAIAvoidRunning) {
                        Log.d(TAG, "AI功能运行中，WebSocket断开，尝试重连...");
                        scheduleWebSocketReconnect();
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "WebSocket连接错误", e);
                    // 连接错误时，如果AI功能正在运行，尝试重连
                    if (isAIAvoidRunning) {
                        Log.d(TAG, "AI功能运行中，WebSocket错误，尝试重连...");
                        scheduleWebSocketReconnect();
                    }
                }
                
                @Override
                public void onReconnectNeeded() {
                    Log.w(TAG, "WebSocket需要重连");
                    // 需要重连时，如果AI功能正在运行，立即尝试重连
                    if (isAIAvoidRunning) {
                        Log.d(TAG, "AI功能运行中，WebSocket需要重连，立即尝试...");
                        scheduleWebSocketReconnect();
                    }
                }
            };
            
            // 添加全局WebSocket消息监听
            webSocketManager.addMessageListener(webSocketMessageListener);
            
            // 添加WebSocket连接状态监听
            webSocketManager.addConnectionStatusListener(webSocketConnectionListener);
            
            Log.d(TAG, "WebSocket监听器初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "WebSocket监听器初始化失败", e);
        }
    }
    

    
    /**
     * 调度WebSocket重连
     */
    private void scheduleWebSocketReconnect() {
        try {
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                Log.w(TAG, "WebSocket重连次数已达上限(" + MAX_RECONNECT_ATTEMPTS + ")，停止重连");
                return;
            }
            
            if (webSocketReconnectHandler == null) {
                webSocketReconnectHandler = new Handler(Looper.getMainLooper());
            }
            
            if (webSocketReconnectRunnable == null) {
                webSocketReconnectRunnable = new Runnable() {
                    @Override
                    public void run() {
                        attemptWebSocketReconnect();
                    }
                };
            }
            
            reconnectAttempts++;
            Log.d(TAG, "调度WebSocket重连，第" + reconnectAttempts + "次尝试，延迟" + WEBSOCKET_RECONNECT_DELAY + "ms");
            
            webSocketReconnectHandler.postDelayed(webSocketReconnectRunnable, WEBSOCKET_RECONNECT_DELAY);
            
        } catch (Exception e) {
            Log.e(TAG, "调度WebSocket重连失败", e);
        }
    }
    
    /**
     * 尝试WebSocket重连
     */
    private void attemptWebSocketReconnect() {
        try {
            Log.d(TAG, "开始尝试WebSocket重连...");
            
            if (webSocketManager != null) {
                // 尝试重新建立连接
                // 这里可以调用WebSocketManager的重连方法
                Log.d(TAG, "WebSocket重连尝试完成");
            } else {
                Log.w(TAG, "WebSocketManager为空，无法重连");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "WebSocket重连失败", e);
        }
    }
    
    /**
     * 重置WebSocket重连计数
     */
    private void resetWebSocketReconnectCount() {
        reconnectAttempts = 0;
        Log.d(TAG, "WebSocket重连计数已重置");
    }
    

    
    /**
     * 处理WebSocket消息
     */
    private void handleWebSocketMessage(SocketDataV0 data) {
        if (data == null) {
            Log.w(TAG, "收到空的WebSocket消息");
            return;
        }
        
        // 简化的WebSocket消息日志
        Log.d(TAG, "收到后端消息: 类型=" + data.getRequestType() + ", 内容=" + data.getContent());
        
        // 检查Fragment是否还attached到context
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment未attached到context，跳过消息处理");
            return;
        }
        
        // 检查Fragment是否还attached到Activity
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            Log.w(TAG, "Fragment的Activity状态异常，跳过消息处理");
            return;
        }
        
        // 根据requesttype执行对应操作
        switch (data.getRequestType()) {
            case SocketDataV0.REQUEST_TYPE_AI_PHOTO_RECOGNITION:
                // 5001: 启动AI页面的拍照识别按钮和对应的功能
                Log.d(TAG, "处理5001: AI拍照识别请求");
                handlePhotoRecognitionRequest();
                break;
                
            case SocketDataV0.REQUEST_TYPE_JUMP_TO_BLINDHOME:
                // 5002: 切换到主页Fragment并开启连线志愿者按钮和功能
                Log.d(TAG, "处理5002: 切换到主页并开启连线志愿者请求");
                handleJumpToBlindhomeRequest();
                break;
                
            case SocketDataV0.REQUEST_TYPE_EMERGENCY_HELP:
                // 5003: 紧急求助，切换到主页Fragment并开启紧急求助功能
                Log.d(TAG, "处理5003: 紧急求助请求");
                handleEmergencyHelpRequest();
                break;
                
            case SocketDataV0.REQUEST_TYPE_APP_JUMP:
                // 5004: APP跳转
                Log.d(TAG, "处理5004: APP跳转请求");
                handleAppJumpRequest(data);
                break;
                
            case SocketDataV0.REQUEST_TYPE_EDIT_PROFILE:
                // 5005: 跳转到用户信息修改页面
                Log.d(TAG, "处理5005: 编辑用户信息请求");
                handleEditProfileRequest();
                break;
                
            default:
                Log.d(TAG, "收到未处理的WebSocket消息类型: " + data.getRequestType() + "，消息内容: " + data.getContent());
                break;
        }
    }
    
    /**
     * 处理5001: 启动AI页面的拍照识别按钮和对应的功能
     */
    private void handlePhotoRecognitionRequest() {
        Log.d(TAG, "收到启动AI拍照识别请求");
        
        // 确保在主线程中执行UI操作
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    // 启用拍照按钮
                    if (btnCamera != null) {
                        btnCamera.setEnabled(true);
                        btnCamera.setVisibility(View.VISIBLE);
                        Log.d(TAG, "拍照按钮已启用");
                    }
                    
                    // 启用语音按钮（如果需要的话）
                    if (btnVoice != null) {
                        btnVoice.setEnabled(true);
                        btnVoice.setVisibility(View.VISIBLE);
                        Log.d(TAG, "语音按钮已启用");
                    }
                    
                    // 初始化摄像头管理器（如果还没有初始化）
                    if (cameraManager == null) {
                        initCameraManager();
                        Log.d(TAG, "摄像头管理器已初始化");
                    }
                    
                    // 启动摄像头预览
                    if (cameraManager != null && cameraPreview != null) {
                        try {
                            cameraManager.startPreview();
                            Log.d(TAG, "摄像头预览已启动");
                        } catch (Exception e) {
                            Log.e(TAG, "启动摄像头预览失败", e);
                        }
                    }
                    
                    // 显示提示信息
                    if (isAdded() && getContext() != null) {
                        try {
                            Toast.makeText(requireContext(), "AI拍照识别功能已启动", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.w(TAG, "显示Toast提示失败", e);
                        }
                    }
                    
                    // 添加震动反馈，提示用户功能已启动
                    if (vibrator != null && isAdded() && getContext() != null) {
                        try {
                            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                        } catch (Exception e) {
                            Log.w(TAG, "震动反馈失败", e);
                        }
                    }
                    
                    // 可以在这里添加其他拍照识别相关的初始化逻辑
                    Log.d(TAG, "AI拍照识别功能启动完成");
                } catch (Exception e) {
                    Log.e(TAG, "启动AI拍照识别功能失败", e);
                }
            });
        }
    }
    
    /**
     * 处理5002: 切换到主页Fragment并开启连线志愿者按钮和功能
     */
    private void handleJumpToBlindhomeRequest() {
        Log.d(TAG, "收到切换到主页并开启连线志愿者请求");
        
        try {
            // 页面切换前预清理资源，避免与新页面启动冲突
            preCleanupResourcesForNavigation();
            
            // 直接切换到底部导航的主页Fragment，不做复杂处理
            Log.d(TAG, "开始切换到底部导航主页...");
            
            // 获取导航控制器，切换到主页Fragment
            if (getActivity() != null) {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                if (navController != null) {
                    // 使用Bundle传递参数，然后导航到主页Fragment
                    Bundle args = new Bundle();
                    args.putBoolean("from_ai_volunteer_connection", true);
                    args.putString("businessType", "volunteer_connection");
                    
                    // 导航到主页Fragment并传递参数
                    navController.navigate(R.id.navigation_home, args);
                    
                    Log.d(TAG, "切换成功，已切换到主页Fragment，并传递了连线志愿者参数");
                    
                    // 显示切换提示
                    Toast.makeText(requireContext(), "已切换到主页，连线志愿者功能已启动", Toast.LENGTH_SHORT).show();
                    
                    // 添加震动反馈，提示用户已切换
                    if (vibrator != null && isAdded() && getContext() != null) {
                        try {
                            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                        } catch (Exception e) {
                            Log.w(TAG, "震动反馈失败", e);
                        }
                    }
                    
                } else {
                    Log.e(TAG, "无法获取导航控制器");
                    Toast.makeText(requireContext(), "页面切换失败：导航控制器异常", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Fragment未attached到Activity，无法切换");
                Toast.makeText(requireContext(), "页面切换失败：页面状态异常", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "切换主页Fragment失败", e);
            Toast.makeText(requireContext(), "页面切换失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 处理5003: 紧急求助，切换到主页Fragment并开启紧急求助功能
     */
    private void handleEmergencyHelpRequest() {
        Log.d(TAG, "收到紧急求助请求");
        
        try {
            // 页面切换前预清理资源，避免与新页面启动冲突
            preCleanupResourcesForNavigation();
            
            // 直接切换到底部导航的主页Fragment，不做复杂处理
            Log.d(TAG, "开始切换到底部导航主页...");
            
            // 获取导航控制器，切换到主页Fragment
            if (getActivity() != null) {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                if (navController != null) {
                    // 使用Bundle传递参数，然后导航到主页Fragment
                    Bundle args = new Bundle();
                    args.putBoolean("from_ai_emergency_help", true);
                    args.putString("businessType", "emergency_help");
                    
                    // 导航到主页Fragment并传递参数
                    navController.navigate(R.id.navigation_home, args);
                    
                    Log.d(TAG, "切换成功，已切换到主页Fragment");
                    
                    // 显示切换提示
                    Toast.makeText(requireContext(), "已切换到主页，紧急求助功能已启动", Toast.LENGTH_SHORT).show();
                    
                    // 添加震动反馈，提示用户已切换
                    if (vibrator != null && isAdded() && getContext() != null) {
                        try {
                            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                        } catch (Exception e) {
                            Log.w(TAG, "震动反馈失败", e);
                        }
                    }
                    
                } else {
                    Log.e(TAG, "无法获取导航控制器");
                    Toast.makeText(requireContext(), "页面切换失败：导航控制器异常", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Fragment未attached到Activity，无法切换");
                Toast.makeText(requireContext(), "页面切换失败：页面状态异常", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "切换主页Fragment失败", e);
            Toast.makeText(requireContext(), "页面切换失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 处理5004: APP跳转
     */
    private void handleAppJumpRequest(SocketDataV0 data) {
        Log.d(TAG, "收到APP跳转请求");
        
        try {
            // 获取目标应用名称
            String targetAppName = data.getVolunteerPhone();
            if (targetAppName == null || targetAppName.trim().isEmpty()) {
                Log.w(TAG, "APP跳转请求中volunteerPhone字段为空");
                Toast.makeText(requireContext(), "APP跳转请求参数错误", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d(TAG, "目标应用名称: " + targetAppName);
            
            // 检查应用列表管理器是否已初始化
            if (appListManager == null || !appListManager.isInitialized()) {
                Log.w(TAG, "应用列表管理器未初始化，无法检查应用");
                Toast.makeText(requireContext(), "应用列表未准备好，请稍后重试", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查应用是否已安装
            boolean isAppInstalled = appListManager.isAppInstalled(targetAppName);
            Log.d(TAG, "应用 '" + targetAppName + "' 是否已安装: " + (isAppInstalled ? "是" : "否"));
            
            if (isAppInstalled) {
                // 应用已安装，执行跳转
                Log.d(TAG, "开始跳转到应用: " + targetAppName);
                
                // 播报跳转提示
                if (ttsManager != null) {
                    ttsManager.startSpeaking("正在打开" + targetAppName);
                }
                
                // 等待TTS播报完成
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 执行应用跳转
                boolean jumpSuccess = appListManager.launchApp(targetAppName);
                if (jumpSuccess) {
                    Log.d(TAG, "应用跳转成功: " + targetAppName);
                    Toast.makeText(requireContext(), "已打开" + targetAppName, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "应用跳转失败: " + targetAppName);
                    Toast.makeText(requireContext(), "打开" + targetAppName + "失败", Toast.LENGTH_SHORT).show();
                }
                
            } else {
                // 应用未安装，显示提示
                Log.d(TAG, "应用未安装: " + targetAppName);
                
                // 播报提示信息
                if (ttsManager != null) {
                    ttsManager.startSpeaking("当前未下载" + targetAppName + "，无法执行跳转");
                }
                
                // 显示弹窗提示
                showAppNotInstalledDialog(targetAppName);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "APP跳转失败", e);
            Toast.makeText(requireContext(), "APP跳转失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 处理5005: 跳转到用户信息修改页面
     */
    private void handleEditProfileRequest() {
        Log.d(TAG, "收到跳转用户信息修改页面请求");
        
        try {
            // 页面跳转前预清理资源，避免与新页面启动冲突
            preCleanupResourcesForNavigation();
            
            // 显示跳转提示
            Toast.makeText(requireContext(), "正在跳转到用户信息修改页面...", Toast.LENGTH_SHORT).show();
            
            // 添加震动反馈，提示用户即将跳转
            if (vibrator != null && isAdded() && getContext() != null) {
                try {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                } catch (Exception e) {
                    Log.w(TAG, "震动反馈失败", e);
                }
            }
            
            // 延迟执行跳转，让用户看到提示信息
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    // 直接跳转到编辑页面，不需要先切换到个人中心Fragment
                    if (getActivity() != null) {
                        Intent intent = new Intent(requireContext(), com.swj.shiwujie.blind.EditProfileActivity.class);
                        intent.putExtra("source", "ai");
                        startActivity(intent);
                        
                        Log.d(TAG, "已直接跳转到编辑页面");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "跳转用户信息修改页面失败", e);
                    Toast.makeText(requireContext(), "处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, 800); // 延迟0.8秒执行跳转
            
        } catch (Exception e) {
            Log.e(TAG, "处理用户信息修改请求失败", e);
            Toast.makeText(requireContext(), "处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 将JSON响应转换为ObstacleDetectionData对象
     * @param jsonResponse JSON响应对象
     * @return ObstacleDetectionData对象，转换失败返回null
     */
    private ObstacleDetectionData convertJsonToObstacleDetectionData(org.json.JSONObject jsonResponse) {
        try {
            ObstacleDetectionData data = new ObstacleDetectionData();
            
            // 转换未知障碍物
            if (jsonResponse.has("nearest_unknown_obstacle") && !jsonResponse.isNull("nearest_unknown_obstacle")) {
                try {
                    org.json.JSONObject obs = jsonResponse.getJSONObject("nearest_unknown_obstacle");
                    if (obs.has("distance_m") && obs.has("location_relative")) {
                        UnknownObstacle unknownObs = new UnknownObstacle();
                        unknownObs.setDistanceM(obs.getDouble("distance_m"));
                        unknownObs.setLocationRelative(parseLocationArray(obs.getJSONArray("location_relative")));
                        data.setNearestUnknownObstacle(unknownObs);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "转换未知障碍物数据失败", e);
                }
            }
            
            // 转换检测到的物体
            if (jsonResponse.has("detected_objects") && !jsonResponse.isNull("detected_objects")) {
                try {
                org.json.JSONArray detectedObjects = jsonResponse.getJSONArray("detected_objects");
                    if (detectedObjects.length() > 0) {
                        java.util.List<DetectedObject> objects = new java.util.ArrayList<>();
                for (int i = 0; i < detectedObjects.length(); i++) {
                        org.json.JSONObject obj = detectedObjects.getJSONObject(i);
                            try {
                                if (obj.has("box_center_relative") && obj.has("class_name") && obj.has("distance_m")) {
                                    DetectedObject detectedObj = new DetectedObject();
                                    detectedObj.setClassName(obj.getString("class_name"));
                                    detectedObj.setDistanceM(obj.getDouble("distance_m"));
                                    detectedObj.setBoxCenterRelative(parseLocationArray(obj.getJSONArray("box_center_relative")));
                                    objects.add(detectedObj);
                                }
                    } catch (Exception e) {
                                Log.e(TAG, "转换检测物体数据失败", e);
                            }
                        }
                        data.setDetectedObjects(objects);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "转换检测物体列表失败", e);
                }
            }
            
            return data;
            
        } catch (Exception e) {
            Log.e(TAG, "转换JSON到ObstacleDetectionData失败", e);
            return null;
        }
    }
    
    /**
     * 解析位置数组
     * @param locationArray 位置数组JSON
     * @return Double列表
     */
    private java.util.List<Double> parseLocationArray(org.json.JSONArray locationArray) {
        java.util.List<Double> location = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < locationArray.length(); i++) {
                location.add(locationArray.getDouble(i));
            }
        } catch (Exception e) {
            Log.e(TAG, "解析位置数组失败", e);
        }
        return location;
    }
    
    /**
     * 播报最近两条未播报的检测信息
     */
    private void broadcastRecentUnbroadcastedInfo() {
        try {
            if (detectionHistory.isEmpty()) {
                Log.d(TAG, "没有历史检测记录可播报");
                return;
            }
            
            // 从最近的记录开始，找到未播报的信息
            List<ObstacleDetectionData> unbroadcastedData = new ArrayList<>();
            
            // 从最新到最旧的顺序遍历历史记录
            for (int i = detectionHistory.size() - 1; i >= 0; i--) {
                ObstacleDetectionData data = detectionHistory.get(i);
                String hash = generateDetectionHashFromData(data);
                
                if (!broadcastedHashes.contains(hash)) {
                    unbroadcastedData.add(data);
                    if (unbroadcastedData.size() >= 2) {
                        break; // 最多取2条
                    }
                }
            }
            
            if (unbroadcastedData.isEmpty()) {
                Log.d(TAG, "没有未播报的检测信息");
                return;
            }
            
            Log.d(TAG, "找到 " + unbroadcastedData.size() + " 条未播报的检测信息，开始播报");
            
            // 播报未播报的信息
            for (ObstacleDetectionData data : unbroadcastedData) {
                String hash = generateDetectionHashFromData(data);
                broadcastedHashes.add(hash); // 标记为已播报
                
                // 使用TTS播报
                if (obstacleDetectionTTSManager != null) {
                    obstacleDetectionTTSManager.processAndSpeak(data);
                }
                
                Log.d(TAG, "播报未播报的检测信息: " + hash);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "播报未播报检测信息失败", e);
        }
    }
    
    /**
     * 从ObstacleDetectionData生成哈希值
     */
    private String generateDetectionHashFromData(ObstacleDetectionData data) {
        try {
            StringBuilder hashBuilder = new StringBuilder();
            
            // 提取关键检测信息
            List<ObstacleDetectionData.DetectedObject> detectedObjects = data.getDetectedObjects();
            if (detectedObjects != null && !detectedObjects.isEmpty()) {
                hashBuilder.append("objects:");
                for (ObstacleDetectionData.DetectedObject obj : detectedObjects) {
                    String className = obj.getClassName();
                    double distance = obj.getDistanceM();
                    // 距离精度到0.1米，避免微小变化
                    hashBuilder.append(className).append(":").append(String.format("%.1f", distance)).append(",");
                }
            }
            
            // 处理未知障碍物
            if (data.getNearestUnknownObstacle() != null) {
                ObstacleDetectionData.UnknownObstacle obs = data.getNearestUnknownObstacle();
                double distance = obs.getDistanceM();
                // 距离精度到0.1米
                hashBuilder.append("unknown:").append(String.format("%.1f", distance));
            }
            
            // 如果没有检测到任何内容，返回特殊标识
            if (hashBuilder.length() == 0) {
                return "no_detection";
            }
            
            return hashBuilder.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "从检测数据生成哈希失败", e);
            return "error_hash";
        }
    }
    
    /**
     * 初始化应用列表管理器
     */
    private void initAppListManager() {
        try {
            Log.d(TAG, "开始初始化应用列表管理器...");
            appListManager = new AppListManager(requireContext());
            
            // 设置应用列表准备完成的监听器
            appListManager.setOnAppListReadyListener(new AppListManager.OnAppListReadyListener() {
                @Override
                public void onAppListReady() {
                    Log.d(TAG, "应用列表准备完成");
                }
            });
            
            // 在子线程中加载应用列表
            new Thread(() -> {
                try {
                    Log.d(TAG, "在子线程中开始获取应用列表...");
                    appListManager.loadInstalledApps();
                    Log.d(TAG, "应用列表获取完成，缓存数量: " + appListManager.getCachedAppCount());
                } catch (Exception e) {
                    Log.e(TAG, "在子线程中获取应用列表失败", e);
                    Log.e(TAG, "错误详情: " + e.getMessage());
                }
            }).start();
            
            Log.d(TAG, "应用列表管理器初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化应用列表管理器失败", e);
            Log.e(TAG, "错误详情: " + e.getMessage());
        }
    }
    
    /**
     * 启动AI悬浮球服务
     */
    private void startAIFloatingBallService() {
        try {
            // 移除Fragment内的服务启动，交由首页统一管理，避免重复与跨身份
      } catch (Exception e) {
            Log.e(TAG, "启动AI悬浮球服务失败", e);
        }
    }
    
    /**
     * 显示AI悬浮球
     */
    private void showAIFloatingBall() {
        try {
            Log.d(TAG, "显示AI悬浮球");
            // 通过广播通知悬浮球服务显示悬浮球
            Intent intent = new Intent("com.swj.shiwujie.SHOW_AI_FLOATING_BALL");
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "显示AI悬浮球失败", e);
        }
    }
    
    /**
     * 隐藏AI悬浮球
     */
    private void hideAIFloatingBall() {
        try {
            Log.d(TAG, "隐藏AI悬浮球");
            // 通过广播通知悬浮球服务隐藏悬浮球
            Intent intent = new Intent("com.swj.shiwujie.HIDE_AI_FLOATING_BALL");
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "隐藏AI悬浮球失败", e);
        }
    }
    
    /**
     * 显示应用未安装的提示对话框
     */
    private void showAppNotInstalledDialog(String appName) {
        try {
            if (getActivity() == null || !isAdded()) {
                Log.w(TAG, "Fragment未attached到Activity，无法显示对话框");
                return;
            }
            
            // 在主线程中显示对话框
            getActivity().runOnUiThread(() -> {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("应用未安装")
                           .setMessage("当前未下载" + appName + "，无法执行跳转。\n\n是否前往应用商店下载？")
                           .setPositiveButton("前往下载", (dialog, which) -> {
                               // 获取应用商店链接
                               String packageName = appListManager.getPackageName(appName);
                               if (packageName != null) {
                                   String storeLink = appListManager.getAppStoreLink(packageName);
                                   if (storeLink != null) {
                                       try {
                                           Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(storeLink));
                                           startActivity(intent);
                                       } catch (Exception e) {
                                           Log.e(TAG, "打开应用商店失败", e);
                                           Toast.makeText(requireContext(), "打开应用商店失败", Toast.LENGTH_SHORT).show();
                                       }
                                   }
                               } else {
                                   Toast.makeText(requireContext(), "无法获取应用下载链接", Toast.LENGTH_SHORT).show();
                               }
                           })
                           .setNegativeButton("取消", (dialog, which) -> {
                               dialog.dismiss();
                           })
                           .setCancelable(true);
                    
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    
                } catch (Exception e) {
                    Log.e(TAG, "显示应用未安装对话框失败", e);
                    Toast.makeText(requireContext(), "显示提示对话框失败", Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "准备显示应用未安装对话框失败", e);
        }
    }
    

} 