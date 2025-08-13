package com.swj.shiwujie.blind.ui.ai;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swj.shiwujie.R;
import com.swj.shiwujie.common.utils.SpeechRecognitionManager;
import com.swj.shiwujie.common.network.AiChatManager;
import com.swj.shiwujie.common.utils.TTSManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI助理Fragment
 * 集成讯飞语音听写功能，按照官方文档要求实现
 */
public class AiFragment extends Fragment {
    private static final String TAG = "AiFragment";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String PREF_NAME = "ai_conversation_history";
    private static final String KEY_CONVERSATIONS = "conversations";
    private static final String KEY_CURRENT_CONVERSATION = "current_conversation";
    
    private SpeechRecognitionManager speechManager;
    private AiChatManager aiChatManager;
    private TTSManager ttsManager;
    private Vibrator vibrator;
    private MaterialButton btnVoice;
    private FloatingActionButton fabHistory;
    private boolean isListening = false;
    
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
                Log.d(TAG, "检测到按钮状态不一致，已重置为录音状态");
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
    private static final long SHORT_WAIT = 500;    // 短等待：0.5秒
    private static final long MAX_WAIT = 1500;      // 最大等待：1.5秒
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
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_ai_assistant, container, false);
            
            initViews(view);
            initData();
            initSpeechManager();
            initAiChatManager();
            initTTSManager();
            
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error inflating AI fragment layout", e);
            return new View(requireContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 页面暂停时保存当前对话
        saveCurrentConversation();
        // 停止状态检查定时器
        stopStatusCheck();
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
        
        // 如果正在录音，重新启动状态检查
        if (isListening) {
            startStatusCheck();
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
                
                // 清空UI中的对话内容
                clearConversationUI();
                
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
        fabHistory = view.findViewById(R.id.fab_history);
        
        // 初始化震动器
        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        
        btnVoice.setOnClickListener(v -> handleVoiceButtonClick());
        fabHistory.setOnClickListener(v -> showHistoryDialog());
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
     * 显示历史记录对话框
     */
    private void showHistoryDialog() {
        if (conversationHistory.isEmpty()) {
            Toast.makeText(requireContext(), "暂无对话记录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("对话历史");
        
        // 创建历史记录列表
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 16, 32, 16);
        
        for (Conversation conv : conversationHistory) {
            View itemView = createHistoryItemView(conv);
            container.addView(itemView);
            
            // 添加分割线
            if (conversationHistory.indexOf(conv) < conversationHistory.size() - 1) {
                View divider = new View(requireContext());
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
                params.setMargins(0, 16, 0, 16);
                divider.setLayoutParams(params);
                container.addView(divider);
            }
        }
        
        builder.setView(container);
        builder.setPositiveButton("关闭", null);
        builder.show();
    }
    
    /**
     * 创建历史记录项视图
     */
    private View createHistoryItemView(Conversation conversation) {
        LinearLayout itemView = new LinearLayout(requireContext());
        itemView.setOrientation(LinearLayout.VERTICAL);
        itemView.setPadding(16, 12, 16, 12);
        itemView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_round_white));
        
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
                Log.d(TAG, "收到流式数据块，长度: " + chunk.length() + ", 总长度: " + totalLength);
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
     * 启动智能播报系统
     */
    private void startSmartPlaybackSystem() {
        Log.d(TAG, "启动智能播报系统");
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
        
        Log.d(TAG, "智能播报处理文本: " + text + ", 缓冲区长度: " + streamingContentBuffer.length());
        
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
        
        Log.d(TAG, "处理流式数据块，当前块长度: " + chunk.length() + ", 总长度: " + totalLength);
        
        // 如果还没开始播报，且内容足够长，立即开始播报
        if (currentStreamingState == StreamingState.STREAMING && chunk.length() >= 80) { // 增加触发阈值
            Log.d(TAG, "内容足够长，立即开始播报，长度: " + chunk.length());
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
                Log.d(TAG, "达到空闲阈值，开始播报");
                startSmartPlayback();
            } else {
                // 继续等待，重新启动计时器
                Log.d(TAG, "继续等待，重新启动计时器");
                startSmartTimer();
            }
        };
        
        // 启动计时器，等待时间根据当前状态动态调整
        long waitTime = streamingContentBuffer.length() < 50 ? SHORT_WAIT : MAX_WAIT;
        smartTimerHandler.postDelayed(smartTimerRunnable, waitTime);
        
        Log.d(TAG, "启动智能计时器，等待时间: " + waitTime + "ms");
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
            Log.d(TAG, "内容显著增加且播报进度较高，重新开始播报，增加长度: " + contentIncrease + ", 当前长度: " + currentContent.length());
            
            // 停止当前播报，重新开始播报完整内容
            ttsManager.stopSpeaking();
            ttsManager.startSpeaking(currentContent);
            lastPlayedContentLength = currentContent.length();
        } else if (contentIncrease > 100) {
            // 如果内容增加超过100个字符，强制重新播报
            Log.d(TAG, "内容大量增加，强制重新播报，增加长度: " + contentIncrease);
            ttsManager.stopSpeaking();
            ttsManager.startSpeaking(currentContent);
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
            Log.d(TAG, "内容为空，不进行播报");
            currentStreamingState = StreamingState.STREAMING;
            return;
        }
        
        Log.d(TAG, "开始智能播报，内容长度: " + contentToPlay.length());
        ttsManager.startSpeaking(contentToPlay);
        lastPlayedContentLength = contentToPlay.length();
        lastPlaybackStartTime = System.currentTimeMillis(); // 记录播报开始时间
        
        // 启动流式播报更新定时器
        startStreamingPlaybackUpdateTimer();
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
        Log.d(TAG, "启动流式播报更新定时器，间隔: " + STREAMING_UPDATE_INTERVAL + "ms");
    }
    
    /**
     * 完成智能播报
     */
    private void completeSmartPlayback(String fullResponse) {
        Log.d(TAG, "完成智能播报");
        
        // 如果还在播报，确保播报完整内容
        if (currentStreamingState == StreamingState.PLAYING) {
            // 停止当前播报，重新播报完整内容
            ttsManager.stopSpeaking();
            ttsManager.startSpeaking(fullResponse);
        } else if (currentStreamingState == StreamingState.STREAMING) {
            // 如果还没开始播报，直接播报完整内容
            ttsManager.startSpeaking(fullResponse);
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
        
        Log.d(TAG, "重置智能播报状态");
    }
    
    /**
     * 初始化语音合成管理器
     */
    private void initTTSManager() {
        ttsManager = new TTSManager(requireContext());
        ttsManager.setOnTTSListener(new TTSManager.OnTTSListener() {
            @Override
            public void onTTSStart() {
                // 语音开始播放
                Log.d(TAG, "TTS开始播放");
            }
            
            @Override
            public void onTTSProgress(int progress, int beginPos, int endPos) {
                // 播放进度更新
            }
            
            @Override
            public void onTTSComplete() {
                // 语音播放完成
                Log.d(TAG, "TTS播放完成");
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
        }
    }
    
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
        
        // 释放TTS资源
        if (ttsManager != null) {
            ttsManager.destroy();
        }
        
        // 清理智能播报资源
        if (smartTimerHandler != null) {
            smartTimerHandler.removeCallbacksAndMessages(null);
        }
        if (streamingPlaybackHandler != null) {
            streamingPlaybackHandler.removeCallbacksAndMessages(null);
        }
        resetSmartPlaybackState();
    }
} 