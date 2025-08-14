package com.swj.shiwujie.blind.ui.ai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
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
import java.util.HashMap;
import java.util.Map;

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
    private ImageView btnExpandMessage;
    private LinearLayout messagePanel;
    private TextureView cameraPreview;
    private TextView tabCurrentConversation;
    private TextView tabHistory;
    private TextView tvConversationTitle;
    private boolean isListening = false;
    private boolean isMessagePanelExpanded = true;
    private boolean isCurrentTabSelected = true;
    
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
    private Handler mainHandler;
    private boolean isSessionStarted = false;
    private String lastDetectionHash = ""; // 上次检测结果的哈希值，用于去重
    
    // 在类中定义一个静态Map，初始化所有类别映射
    private static final Map<String, String> CLASS_NAME_MAP = new HashMap<String, String>() {{
        // 人/交通工具
        put("person", "人");
        put("car", "汽车");
        put("truck", "卡车");
        put("bus", "公交车");
        put("motorcycle", "摩托车");
        put("bicycle", "自行车");
        put("airplane", "飞机");
        put("train", "火车");
        put("boat", "船");
        put("fire truck", "消防车");
        put("ambulance", "救护车");
        put("police car", "警车");
        
        // 动物
        put("dog", "狗");
        put("cat", "猫");
        put("bird", "鸟");
        put("horse", "马");
        put("sheep", "羊");
        put("cow", "牛");
        put("elephant", "大象");
        put("bear", "熊");
        put("zebra", "斑马");
        put("giraffe", "长颈鹿");
        
        // 家具/家电
        put("chair", "椅子");
        put("table", "桌子");
        put("refrigerator", "冰箱");
        put("tv", "电视");
        put("television", "电视");
        put("laptop", "笔记本电脑");
        put("microwave", "微波炉");
        put("oven", "烤箱");
        put("toaster", "烤面包机");
        put("sink", "水槽");
        put("couch", "沙发");
        put("sofa", "沙发");
        put("potted plant", "盆栽植物");
        put("bed", "床");
        put("dining table", "餐桌");
        put("toilet", "马桶");
        
        // 日常物品
        put("cell phone", "手机");
        put("phone", "手机");
        put("book", "书");
        put("cup", "杯子");
        put("bottle", "瓶子");
        put("bowl", "碗");
        put("fork", "叉子");
        put("knife", "刀子");
        put("spoon", "勺子");
        put("clock", "时钟");
        put("vase", "花瓶");
        put("scissors", "剪刀");
        put("teddy bear", "泰迪熊");
        put("hair drier", "吹风机");
        put("toothbrush", "牙刷");
        put("umbrella", "雨伞");
        put("handbag", "手提包");
        put("backpack", "背包");
        put("suitcase", "行李箱");
        put("remote", "遥控器");
        put("keyboard", "键盘");
        put("mouse", "鼠标");
        
        // 运动/户外物品
        put("frisbee", "飞盘");
        put("skis", "滑雪板");
        put("snowboard", "滑雪板");
        put("sports ball", "运动球");
        put("kite", "风筝");
        put("baseball bat", "棒球棒");
        put("baseball glove", "棒球手套");
        put("skateboard", "滑板");
        put("surfboard", "冲浪板");
        put("tennis racket", "网球拍");
        
        // 食物
        put("wine glass", "酒杯");
        put("banana", "香蕉");
        put("apple", "苹果");
        put("sandwich", "三明治");
        put("orange", "橙子");
        put("broccoli", "西兰花");
        put("carrot", "胡萝卜");
        put("hot dog", "热狗");
        put("pizza", "披萨");
        put("donut", "甜甜圈");
        put("cake", "蛋糕");
        
        // 交通设施
        put("fire hydrant", "消防栓");
        put("stop sign", "停车标志");
        put("parking meter", "停车计时器");
        put("bench", "长凳");
        put("traffic light", "交通信号灯");
        
        // 其他物品
        put("tie", "领带");
    }};
    
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
        
        // 初始化AI避障功能
        initObstacleDetection();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 页面暂停时保存当前对话
        saveCurrentConversation();
        // 停止状态检查定时器
        stopStatusCheck();
        
        // 停止摄像头预览
        if (cameraManager != null) {
            try {
                cameraManager.stopPreview();
            } catch (Exception e) {
                Log.e(TAG, "停止摄像头预览失败", e);
            }
        }
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
        
        // 启动摄像头预览
        if (cameraManager != null && cameraPreview != null) {
            try {
                cameraManager.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "启动摄像头预览失败", e);
            }
        }
        
        // 如果正在录音，重新启动状态检查
        if (isListening) {
            startStatusCheck();
        }
        
        // 启动AI避障检测
        if (!isSessionStarted) {
            startObstacleDetection();
        }
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
                Log.d(TAG, "检测到按钮状态不一致，已重置");
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
        Log.d(TAG, "启动状态检查定时器");
    }
    
    /**
     * 停止状态检查定时器
     */
    private void stopStatusCheck() {
        statusCheckHandler.removeCallbacks(statusCheckRunnable);
        Log.d(TAG, "停止状态检查定时器");
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
        
        btnVoice.setOnClickListener(v -> handleVoiceButtonClick());
        btnCamera.setOnClickListener(v -> handleCameraButtonClick());
        btnCollapseMessage.setOnClickListener(v -> collapseMessagePanel());
        btnExpandMessage.setOnClickListener(v -> expandMessagePanel());
        
        // 添加AI协助按钮点击事件
        MaterialButton btnAiAssist = view.findViewById(R.id.btn_ai_assist);
        if (btnAiAssist != null) {
            btnAiAssist.setOnClickListener(v -> handleAiAssistButtonClick());
        }

        // ===== 临时添加：障碍物检测按钮点击事件 - 跳转到障碍物检测页面 =====
        // 改造说明：在AI页面添加临时按钮，方便测试障碍物检测功能
        MaterialButton btnObstacleDetection = view.findViewById(R.id.btn_obstacle_detection);
        if (btnObstacleDetection != null) {
            btnObstacleDetection.setOnClickListener(v -> handleObstacleDetectionButtonClick());
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
            
            // 显示提示
            Toast.makeText(requireContext(), "已返回到当前对话", Toast.LENGTH_SHORT).show();
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
            
            cameraManager = new CameraPreviewManager(requireContext());
            cameraManager.setPreviewView(cameraPreview);
            
            // 检查摄像头权限
            if (checkCameraPermission()) {
                // 权限已授予，启动预览
                cameraManager.startPreview();
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化摄像头管理器失败", e);
        }
    }
    
    /**
     * 设置消息面板初始状态
     */
    private void setupMessagePanelInitialState() {
        if (messagePanel == null) return;
        
        try {
            // 设置初始宽度为屏幕宽度的80%
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int panelWidth = (int) (screenWidth * 0.8);
            
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagePanel.getLayoutParams();
            params.width = panelWidth;
            messagePanel.setLayoutParams(params);
            
            // 设置初始透明度
            messagePanel.setAlpha(0.9f);
            
            Log.d(TAG, "消息面板初始宽度设置为: " + panelWidth);
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
        if (currentStreamingState != StreamingState.PLAYING || ttsManager == null) return;
        
        // 获取当前缓冲区的最新内容
        String currentContent = streamingContentBuffer.toString();
        
        // 优化内容长度判断逻辑
        int contentIncrease = currentContent.length() - lastPlayedContentLength;
        
        // 只有当内容增加超过50个字符，且当前播报进度超过70%时才重新播报
        if (contentIncrease > 50 && getCurrentPlaybackProgress() > 70) {
            // 停止当前播报，重新开始播报完整内容
            String contentToSpeak = parseResponseForTTS(currentContent);
            ttsManager.stopSpeaking();
            ttsManager.startSpeaking(contentToSpeak);
            lastPlayedContentLength = currentContent.length();
        } else if (contentIncrease > 100) {
            // 如果内容增加超过100个字符，强制重新播报
            String contentToSpeak = parseResponseForTTS(currentContent);
            ttsManager.stopSpeaking();
            ttsManager.startSpeaking(contentToSpeak);
            lastPlayedContentLength = currentContent.length();
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
        if (currentStreamingState == StreamingState.PLAYING) return;
        
        currentStreamingState = StreamingState.PLAYING;
        String contentToPlay = streamingContentBuffer.toString();
        
        if (contentToPlay.trim().isEmpty()) {
            currentStreamingState = StreamingState.STREAMING;
            return;
        }
        
        // 解析后端响应，决定播报内容
        String contentToSpeak = parseResponseForTTS(contentToPlay);
        
        ttsManager.startSpeaking(contentToSpeak);
        lastPlayedContentLength = contentToPlay.length();
        lastPlaybackStartTime = System.currentTimeMillis(); // 记录播报开始时间
        
        // 启动流式播报更新定时器
        startStreamingPlaybackUpdateTimer();
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
                Toast.makeText(requireContext(), "语音播放失败: " + error, Toast.LENGTH_SHORT).show();
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
     * 处理AI协助按钮点击
     */
    private void handleAiAssistButtonClick() {
        try {
            // 添加震动反馈
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            
            // 使用WebView打开指定网址
            openWebView("https://192.168.229.248:8080");
        } catch (Exception e) {
            Log.e(TAG, "打开AI协助页面失败", e);
            Toast.makeText(requireContext(), "无法打开AI协助页面", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * ===== 临时添加：处理障碍物检测按钮点击 - 跳转到障碍物检测页面 =====
     * 改造说明：在AI页面添加临时按钮，方便测试障碍物检测功能
     */
    private void handleObstacleDetectionButtonClick() {
        try {
            // 添加震动反馈
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            
            // 跳转到障碍物检测页面
            Intent intent = new Intent(requireContext(), com.swj.shiwujie.blind.ObstacleDetectionActivity.class);
            startActivity(intent);
            
            Log.d(TAG, "跳转到障碍物检测页面");
            
        } catch (Exception e) {
            Log.e(TAG, "跳转到障碍物检测页面失败", e);
            Toast.makeText(requireContext(), "无法打开障碍物检测页面", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 使用WebView打开网址
     */
    private void openWebView(String url) {
        try {
            // 创建WebView
            WebView webView = new WebView(requireContext());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setAllowFileAccess(true);
            webView.getSettings().setAllowContentAccess(true);
            
            // 设置WebViewClient以处理页面加载
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                    // 处理SSL证书错误（开发环境可能需要忽略）
                    Log.w(TAG, "SSL证书错误: " + error.toString());
                    handler.proceed(); // 继续加载（仅用于开发环境）
                }
                
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }
            });
            
            // 创建新的Activity来显示WebView
            Intent intent = new Intent(requireContext(), WebViewActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("title", "AI协助");
            startActivity(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "创建WebView失败", e);
            // 如果WebView创建失败，尝试使用系统浏览器打开
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e2) {
                Log.e(TAG, "使用系统浏览器打开失败", e2);
                Toast.makeText(requireContext(), "无法打开网页", Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "相机权限状态: " + permissionStatus + " (0=已授权, -1=未授权)");
        
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "相机权限未授予，开始请求权限");
            
            // 检查是否应该显示权限说明
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
                Log.d(TAG, "显示相机权限说明对话框");
                // 用户之前拒绝过权限，显示说明对话框
                showCameraPermissionExplanationDialog();
            } else {
                Log.d(TAG, "直接请求相机权限");
                // 直接请求权限
                requestCameraPermission();
            }
            return false;
        }
        
        Log.d(TAG, "相机权限已授予");
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
                    Toast.makeText(requireContext(), "无法跳转设置页面，请手动开启" + permissionName, Toast.LENGTH_LONG).show();
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
                Toast.makeText(requireContext(), "没有摄像头权限无法使用拍照功能", Toast.LENGTH_LONG).show();
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
                Toast.makeText(requireContext(), "录音权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "录音权限被拒绝，无法使用语音识别功能", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "摄像头权限已授予", Toast.LENGTH_SHORT).show();
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
        speechManager.startListening();
    }
    
    /**
     * 停止语音识别
     * 按照官方文档要求调用stopListening
     */
    private void stopVoiceRecognition() {
        speechManager.stopListening();
    }
    
    /**
     * 直接拍照（使用Camera2 API，无需启动系统相机）
     */
    private void takePhotoDirectly() {
        if (cameraManager == null) {
            Log.e(TAG, "CameraPreviewManager未初始化");
            Toast.makeText(requireContext(), "相机管理器未初始化", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!cameraManager.isPreviewActive()) {
            Log.e(TAG, "相机预览未激活");
            Toast.makeText(requireContext(), "相机预览未激活，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (cameraManager.isTakingPhoto()) {
            Toast.makeText(requireContext(), "正在拍照中，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示拍照提示
        Toast.makeText(requireContext(), "正在拍照...", Toast.LENGTH_SHORT).show();
        
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
                    Toast.makeText(requireContext(), "拍照失败: " + error, Toast.LENGTH_SHORT).show();
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
            
            // 延迟发送图片识别请求，确保图片完全显示后再发送
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                sendImageRecognitionRequest(photoFile);
            }, 500); // 延迟500ms，确保图片完全显示
            
        } catch (Exception e) {
            Log.e(TAG, "处理照片数据失败", e);
            Toast.makeText(requireContext(), "处理照片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // 移除旧的handlePhotoResult方法，已被handlePhotoData替代
    
    /**
     * 处理语音识别结果
     * 按照官方文档要求处理识别结果
     */
    private void handleSpeechResult(String result) {
        if (result == null || result.trim().isEmpty()) {
            Toast.makeText(requireContext(), "语音识别结果为空", Toast.LENGTH_SHORT).show();
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
     * 显示AI正在思考的状态
     */
    private void showAiThinkingStatus() {
        if (getView() == null) return;
        
        try {
            // 创建AI思考状态的消息卡片
            com.google.android.material.card.MaterialCardView thinkingCard = createMessageCard(
                "正在思考...", 
                false,  // AI消息
                R.color.blue_50,  // AI消息背景色
                R.color.text_secondary  // 思考状态文字色
            );
            
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer != null) {
                // 添加思考状态卡片
                chatContainer.addView(thinkingCard);
                
                // 滚动到底部
                scrollToBottom();
                
                // 保存思考状态卡片的引用，用于后续更新
                thinkingCard.setTag("thinking_card");
            }
        } catch (Exception e) {
            Log.e(TAG, "显示AI思考状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 显示图片识别状态
     */
    private void showImageRecognitionStatus() {
        if (getView() == null) return;
        
        try {
            // 创建图片识别状态的消息卡片
            com.google.android.material.card.MaterialCardView recognitionCard = createMessageCard(
                "正在识别图片...", 
                false,  // AI消息
                R.color.blue_50,  // AI消息背景色
                R.color.text_secondary  // 识别状态文字色
            );
            
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer != null) {
                // 添加识别状态卡片
                chatContainer.addView(recognitionCard);
                
                // 滚动到底部
                scrollToBottom();
                
                // 保存识别状态卡片的引用，用于后续更新
                recognitionCard.setTag("recognition_card");
            }
        } catch (Exception e) {
            Log.e(TAG, "显示图片识别状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新AI回复的流式输出
     * @param text 当前流式输出的文本
     */
    private void updateAiResponseStreaming(String text) {
        if (getView() == null) return;
        
        try {
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 查找思考状态卡片
            com.google.android.material.card.MaterialCardView thinkingCard = null;
            for (int i = 0; i < chatContainer.getChildCount(); i++) {
                View child = chatContainer.getChildAt(i);
                if (child instanceof com.google.android.material.card.MaterialCardView && 
                    "thinking_card".equals(child.getTag())) {
                    thinkingCard = (com.google.android.material.card.MaterialCardView) child;
                    break;
                }
            }
            
            if (thinkingCard != null) {
                // 更新思考状态卡片的内容为流式输出
                TextView textView = (TextView) thinkingCard.getChildAt(0);
                if (textView != null) {
                    textView.setText(text);
                }
                
                // 滚动到底部
                scrollToBottom();
            }
        } catch (Exception e) {
            Log.e(TAG, "更新AI回复流式输出失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新图片识别结果的流式输出
     * @param text 当前流式输出的文本
     */
    private void updateImageRecognitionStreaming(String text) {
        if (getView() == null) return;
        
        try {
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 查找识别状态卡片
            com.google.android.material.card.MaterialCardView recognitionCard = null;
            for (int i = 0; i < chatContainer.getChildCount(); i++) {
                View child = chatContainer.getChildAt(i);
                if (child instanceof com.google.android.material.card.MaterialCardView && 
                    "recognition_card".equals(child.getTag())) {
                    recognitionCard = (com.google.android.material.card.MaterialCardView) child;
                    break;
                }
            }
            
            if (recognitionCard != null) {
                // 更新识别状态卡片的内容为流式输出
                TextView textView = (TextView) recognitionCard.getChildAt(0);
                if (textView != null) {
                    textView.setText(text);
                }
                
                // 滚动到底部
                scrollToBottom();
            }
        } catch (Exception e) {
            Log.e(TAG, "更新图片识别流式输出失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 完成AI回复
     * @param fullResponse 完整的AI回复
     */
    private void completeAiResponse(String fullResponse) {
        if (getView() == null) return;
        
        try {
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 查找思考状态卡片
            com.google.android.material.card.MaterialCardView thinkingCard = null;
            int thinkingCardIndex = -1;
            for (int i = 0; i < chatContainer.getChildCount(); i++) {
                View child = chatContainer.getChildAt(i);
                if (child instanceof com.google.android.material.card.MaterialCardView && 
                    "thinking_card".equals(child.getTag())) {
                    thinkingCard = (com.google.android.material.card.MaterialCardView) child;
                    thinkingCardIndex = i;
                    break;
                }
            }
            
            if (thinkingCard != null && thinkingCardIndex >= 0) {
                // 移除思考状态卡片
                chatContainer.removeViewAt(thinkingCardIndex);
                
                // 添加完整的AI回复消息
                addAIResponse(fullResponse);
            }
        } catch (Exception e) {
            Log.e(TAG, "完成AI回复失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 完成图片识别
     * @param fullResponse 完整的识别结果
     */
    private void completeImageRecognition(String fullResponse) {
        if (getView() == null) return;
        
        try {
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 查找识别状态卡片
            com.google.android.material.card.MaterialCardView recognitionCard = null;
            int recognitionCardIndex = -1;
            for (int i = 0; i < chatContainer.getChildCount(); i++) {
                View child = chatContainer.getChildAt(i);
                if (child instanceof com.google.android.material.card.MaterialCardView && 
                    "recognition_card".equals(child.getTag())) {
                    recognitionCard = (com.google.android.material.card.MaterialCardView) child;
                    recognitionCardIndex = i;
                    break;
                }
            }
            
            if (recognitionCard != null && recognitionCardIndex >= 0) {
                // 移除识别状态卡片
                chatContainer.removeViewAt(recognitionCardIndex);
                
                // 添加完整的AI识别结果消息
                addAIResponse(fullResponse);
            }
        } catch (Exception e) {
            Log.e(TAG, "完成图片识别失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理AI回复错误
     * @param error 错误信息
     */
    private void handleAiResponseError(String error) {
        if (getView() == null) return;
        
        try {
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 查找思考状态卡片
            com.google.android.material.card.MaterialCardView thinkingCard = null;
            int thinkingCardIndex = -1;
            for (int i = 0; i < chatContainer.getChildCount(); i++) {
                View child = chatContainer.getChildAt(i);
                if (child instanceof com.google.android.material.card.MaterialCardView && 
                    "thinking_card".equals(child.getTag())) {
                    thinkingCard = (com.google.android.material.card.MaterialCardView) child;
                    thinkingCardIndex = i;
                    break;
                }
            }
            
            if (thinkingCard != null && thinkingCardIndex >= 0) {
                // 移除思考状态卡片
                chatContainer.removeViewAt(thinkingCardIndex);
                
                // 显示错误消息
                Toast.makeText(requireContext(), "AI回复失败: " + error, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "处理AI回复错误失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理图片识别错误
     * @param error 错误信息
     */
    private void handleImageRecognitionError(String error) {
        if (getView() == null) return;
        
        try {
            // 获取对话容器
            LinearLayout chatContainer = getView().findViewById(R.id.chat_container);
            if (chatContainer == null) return;
            
            // 查找识别状态卡片
            com.google.android.material.card.MaterialCardView recognitionCard = null;
            int recognitionCardIndex = -1;
            for (int i = 0; i < chatContainer.getChildCount(); i++) {
                View child = chatContainer.getChildAt(i);
                if (child instanceof com.google.android.material.card.MaterialCardView && 
                    "recognition_card".equals(child.getTag())) {
                    recognitionCard = (com.google.android.material.card.MaterialCardView) child;
                    recognitionCardIndex = i;
                    break;
                }
            }
            
            if (recognitionCard != null && recognitionCardIndex >= 0) {
                // 移除识别状态卡片
                chatContainer.removeViewAt(recognitionCardIndex);
                
                // 显示错误消息
                Toast.makeText(requireContext(), "图片识别失败: " + error, Toast.LENGTH_LONG).show();
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
            btnVoice.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.blue_50));
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
        if (getView() == null) return;
        
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
            
            Log.d(TAG, "切换到当前对话标签页");
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
            
            Log.d(TAG, "切换到历史记录标签页");
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
        }
        if (streamingPlaybackHandler != null) {
            streamingPlaybackHandler.removeCallbacksAndMessages(null);
        }
        resetSmartPlaybackState();
        
        // 清理AI避障资源
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
    
    // ===== AI避障功能方法 =====
    
    /**
     * 初始化AI避障功能
     */
    private void initObstacleDetection() {
        try {
            mainHandler = new Handler(Looper.getMainLooper());
            
            // 初始化网络服务
            apiService = ObstacleDetectionRetrofitClient.getInstance().createService(ApiService.class);
            
            Log.d(TAG, "AI避障功能初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "AI避障功能初始化失败", e);
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
                                sessionId = jsonResponse.optString("session_id", "");
                                
                                if (sessionId != null && !sessionId.isEmpty()) {
                                    Log.d(TAG, "AI避障会话创建成功: " + sessionId);
                                    isSessionStarted = true;
                                    
                                    // 开始定时检测
                                    startFrameProcessing();
                                    
                                    // 显示启动成功提示
                                    showDetectionMessage("AI避障检测已启动", "success");
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析会话响应失败", e);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e(TAG, "创建AI避障会话失败", t);
                    showDetectionMessage("AI避障启动失败", "error");
                }
            });
        }
    }
    
    /**
     * 启动帧处理
     */
    private void startFrameProcessing() {
        try {
            Log.d(TAG, "开始启动帧处理...");
            Log.d(TAG, "当前状态 - isDetecting: " + isDetecting + ", sessionId: " + sessionId);
            
            // 立即执行一次检测，然后开始8秒循环
            processCurrentFrame();
            
            Runnable frameProcessor = new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "帧处理循环执行中...");
                        if (isSessionStarted && sessionId != null) {
                            Log.d(TAG, "开始处理当前帧...");
                            // 每8秒处理一帧
                            processCurrentFrame();
                            // 每8秒执行一次
                            mainHandler.postDelayed(this, 5000);
                        } else {
                            Log.d(TAG, "检测已停止或会话无效，帧处理循环退出");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "帧处理循环失败", e);
                    }
                }
            };
            mainHandler.postDelayed(frameProcessor, 5000);
            Log.d(TAG, "帧处理已启动，8秒后开始循环");
        } catch (Exception e) {
            Log.e(TAG, "启动帧处理失败", e);
        }
    }
    
    /**
     * 处理当前帧
     */
    private void processCurrentFrame() {
        try {
            Log.d(TAG, "开始处理当前帧...");
            
            // 检查会话ID
            if (sessionId == null) {
                Log.e(TAG, "会话ID无效，跳过帧处理");
                return;
            }
            
            Log.d(TAG, "条件检查通过，开始获取图像...");
            // 获取当前帧图像并转换为Base64
            String imageBase64 = getCurrentFrameBase64();
            Log.d(TAG, "图像获取成功，长度: " + imageBase64.length() + "，开始调用API...");
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
            // 检查摄像头管理器是否可用
            if (cameraManager == null || !cameraManager.isPreviewActive()) {
                
                throw new RuntimeException("摄像头未准备好");
            }
            return takePhotoSync();
            
        } catch (Exception e) {
            throw new RuntimeException("获取摄像头图像失败: " + e.getMessage());
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
                throw new RuntimeException("拍照失败");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("拍照异常: " + e.getMessage());
        }
    }
    
    /**
     * 调用API处理图像帧
     */
    private void processFrameWithAPI(String imageBase64) {
        if (apiService != null && sessionId != null) {
            try {
                // 使用JSONObject正确构建JSON请求体
                org.json.JSONObject jsonObject = new org.json.JSONObject();
                jsonObject.put("session_id", sessionId);
                jsonObject.put("image", imageBase64);
                
                String jsonBody = jsonObject.toString();
                
                // 添加调试信息
                Log.d(TAG, "请求体大小: " + jsonBody.length() + " 字符");
                Log.d(TAG, "Session ID: " + sessionId);
                Log.d(TAG, "Session ID长度: " + sessionId.length());
                
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(mediaType, jsonBody);
                
                Call<String> processCall = apiService.processFrameForObstacleDetection(requestBody);
                
                processCall.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String responseBody = response.body();
                                Log.d(TAG, "帧处理API响应: " + responseBody);
                                
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
                        Log.e(TAG, "API网络请求失败", t);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "构建JSON请求体失败", e);
            }
        } else {
            Log.e(TAG, "apiService为null或sessionId无效，无法处理图像帧");
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
            if (currentDetectionHash.equals(lastDetectionHash)) {
                Log.d(TAG, "检测结果与上次相同，跳过TTS播报");
                return;
            }
            // 更新上次检测结果哈希
            lastDetectionHash = currentDetectionHash;
            
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
            
            // ===== 直接生成TTS播报文本，只播报距离最近的障碍物 =====
            String ttsText = generateSimpleObstacleSummary(jsonResponse);
            if (ttsText != null && !ttsText.trim().isEmpty()) {
                // 立即启动TTS播报，减少等待时间
                speakObstacleDetectionResult(ttsText);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "解析检测结果失败", e);
        }
    }
    
    /**
     * 生成简化的障碍物检测摘要，只播报距离最近的障碍物
     * @param jsonResponse 检测结果JSON对象
     * @return 中文摘要文本
     */
    private String generateSimpleObstacleSummary(org.json.JSONObject jsonResponse) {
        try {
            // 收集所有障碍物信息
            java.util.List<ObstacleInfo> obstacleInfos = new java.util.ArrayList<>();
            
            // 检查是否有未知障碍物
            if (jsonResponse.has("nearest_unknown_obstacle") && !jsonResponse.isNull("nearest_unknown_obstacle")) {
                try {
                    org.json.JSONObject obs = jsonResponse.getJSONObject("nearest_unknown_obstacle");
                    if (obs.has("distance_m") && obs.has("location_relative")) {
                        double distance = obs.getDouble("distance_m");
                        org.json.JSONArray location = obs.getJSONArray("location_relative");
                        if (location.length() > 0) {
                            double relativeX = location.getDouble(0);
                            String position = getPositionDescription(relativeX);
                            obstacleInfos.add(new ObstacleInfo(distance, position, "未知障碍物"));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析未知障碍物信息失败", e);
                }
            }
            
            // 检查检测到的物体
            if (jsonResponse.has("detected_objects") && !jsonResponse.isNull("detected_objects")) {
                try {
                    org.json.JSONArray detectedObjects = jsonResponse.getJSONArray("detected_objects");
                    if (detectedObjects.length() > 0) {
                        for (int i = 0; i < detectedObjects.length(); i++) {
                            org.json.JSONObject obj = detectedObjects.getJSONObject(i);
                            try {
                                if (obj.has("box_center_relative") && obj.has("class_name") && obj.has("distance_m")) {
                                    org.json.JSONArray boxCenter = obj.getJSONArray("box_center_relative");
                                    if (boxCenter.length() > 0) {
                                        double relativeX = boxCenter.getDouble(0);
                                        String position = getPositionDescription(relativeX);
                                        String className = obj.getString("class_name");
                                        double distance = obj.getDouble("distance_m");
                                        
                                        obstacleInfos.add(new ObstacleInfo(distance, position, translateClassName(className)));
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "解析检测物体信息失败", e);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析检测物体列表失败", e);
                }
            }
            
            // 只选择距离最近的障碍物
            if (!obstacleInfos.isEmpty()) {
                obstacleInfos.sort((a, b) -> Double.compare(a.distance, b.distance));
                ObstacleInfo nearest = obstacleInfos.get(0);
                
                if ("未知障碍物".equals(nearest.className)) {
                    return String.format("在您%s方，大约%.1f米处，有一个未知障碍物。", nearest.position, nearest.distance);
                } else {
                    return String.format("在您%s方，大约%.1f米处，有一个%s。", nearest.position, nearest.distance, nearest.className);
                }
            }
            
            // 如果没有检测到任何障碍物或物体，不播报
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "生成障碍物检测摘要失败", e);
            return null;
        }
    }
    
    /**
     * 障碍物信息内部类
     */
    private static class ObstacleInfo {
        double distance;
        String position;
        String className;
        
        ObstacleInfo(double distance, String position, String className) {
            this.distance = distance;
            this.position = position;
            this.className = className;
        }
    }
    
    /**
     * 根据相对X坐标获取位置描述
     * 参考JavaScript版本的getPosition函数逻辑
     * @param relativeX 相对X坐标（0-1之间）
     * @return 位置描述（左、正前、右）
     */
    private String getPositionDescription(double relativeX) {
        if (relativeX < 0.33) {
            return "左";
        } else if (relativeX > 0.66) {
            return "右";
        } else {
            return "正前";
        }
    }
    
    /**
     * 翻译物体类别名称（优化后）
     * 将英文类别名翻译为中文
     * @param className 英文类别名
     * @return 中文类别名（未匹配时返回"未知物体"）
     */
    private String translateClassName(String className) {
        if (className == null) {
            return "未知物体";
        }
        // 转为小写后查询映射，未找到则返回"未知物体"
        return CLASS_NAME_MAP.getOrDefault(className.toLowerCase(), "未知物体");
    }
    
    /**
     * 使用TTS播报障碍物检测结果（优化版）
     * @param text 要播报的中文文本
     */
    private void speakObstacleDetectionResult(String text) {
        try {
            if (ttsManager != null && text != null && !text.trim().isEmpty()) {
                Log.d(TAG, "开始TTS播报障碍物检测结果: " + text);
                
                // 如果TTS正在播报，立即停止并开始新的播报
                if (ttsManager.isSpeaking()) {
                    ttsManager.stopSpeaking();
                    // 减少等待时间，从100ms减少到50ms
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // 立即开始播报新的检测结果
                ttsManager.startSpeaking(text);
                
                Log.d(TAG, "TTS播报已启动");
            } else {
                Log.w(TAG, "TTS管理器未初始化或播报文本为空");
            }
        } catch (Exception e) {
            Log.e(TAG, "TTS播报障碍物检测结果失败", e);
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
                    
                    // 这里可以添加更复杂的弹幕显示逻辑
                    // 比如在页面上创建一个浮动的TextView来显示检测结果
                    Log.d(TAG, "AI避障检测结果: " + message);
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
} 