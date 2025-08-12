package com.swj.shiwujie.blind.ui.ai;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
        
        // 这里可以调用真实的AI接口，暂时不显示回复
        // 或者可以显示一个"正在思考..."的提示
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
            
        } catch (Exception e) {
            Log.e(TAG, "添加用户消息失败: " + e.getMessage(), e);
        }
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
    }
} 