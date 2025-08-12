package com.swj.shiwujie.blind.ui.ai;

import android.Manifest;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.swj.shiwujie.R;
import com.swj.shiwujie.common.utils.SpeechRecognitionManager;

/**
 * AI助理Fragment
 * 集成讯飞语音听写功能，按照官方文档要求实现
 */
public class AiFragment extends Fragment {
    private static final String TAG = "AiFragment";
    private static final int PERMISSION_REQUEST_CODE = 200;
    
    private SpeechRecognitionManager speechManager;
    private MaterialButton btnVoice;
    private boolean isListening = false;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            Log.d(TAG, "AiFragment onCreateView called");
            View view = inflater.inflate(R.layout.fragment_ai_assistant, container, false);
            Log.d(TAG, "AiFragment layout inflated successfully");
            
            initViews(view);
            initSpeechManager();
            
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error inflating AI fragment layout", e);
            // 返回一个简单的视图作为后备
            return new View(requireContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "AiFragment onViewCreated called");
        // 这里可以添加任何需要的初始化代码
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        btnVoice = view.findViewById(R.id.btn_voice);
        btnVoice.setOnClickListener(v -> handleVoiceButtonClick());
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
                handleSpeechResult(result);
            }
            
            @Override
            public void onError(String error) {
                handleSpeechError(error);
            }
            
            @Override
            public void onStart() {
                updateVoiceButtonState(true);
                showRecordingStatus("正在录音...");
            }
            
            @Override
            public void onEnd() {
                updateVoiceButtonState(false);
                hideRecordingStatus();
            }
            
            @Override
            public void onBeginOfSpeech() {
                // 开始说话
                showRecordingStatus("开始说话...");
            }
            
            @Override
            public void onEndOfSpeech() {
                // 结束说话
                showRecordingStatus("正在识别...");
            }
            
            @Override
            public void onVolumeChanged(int volume) {
                // 音量变化，可以用于显示音量指示器
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
        Log.d(TAG, "音量变化: " + volume);
    }
    
    /**
     * 处理语音按钮点击事件
     * 按照官方文档要求实现语音识别控制
     */
    private void handleVoiceButtonClick() {
        if (checkAudioPermission()) {
            if (!isListening) {
                startVoiceRecognition();
            } else {
                stopVoiceRecognition();
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
            
            // 显示权限说明对话框
            showPermissionExplanationDialog();
            
            // 请求权限
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
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
        Toast.makeText(requireContext(), "开始语音识别", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 停止语音识别
     * 按照官方文档要求调用stopListening
     */
    private void stopVoiceRecognition() {
        speechManager.stopListening();
        Toast.makeText(requireContext(), "停止语音识别", Toast.LENGTH_SHORT).show();
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
        
        // 模拟AI回复（这里可以调用真实的AI接口）
        simulateAIResponse("我听到了：" + result);
        
        // 显示成功提示
        Toast.makeText(requireContext(), "语音识别成功：" + result, Toast.LENGTH_LONG).show();
    }
    
    /**
     * 处理语音识别错误
     * 按照官方文档要求处理错误情况
     */
    private void handleSpeechError(String error) {
        // 隐藏录音状态
        hideRecordingStatus();
        
        // 根据错误类型显示不同的提示
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
        
        // 显示错误提示
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
        
        // 记录错误日志
        Log.e(TAG, "语音识别错误: " + error);
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
            
            Log.d(TAG, "用户消息已添加: " + message);
        } catch (Exception e) {
            Log.e(TAG, "添加用户消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 模拟AI回复
     * 模拟AI回复的逻辑
     */
    private void simulateAIResponse(String response) {
        if (getView() == null) return;
        
        try {
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
            
            Log.d(TAG, "AI回复已添加: " + response);
        } catch (Exception e) {
            Log.e(TAG, "添加AI回复失败: " + e.getMessage(), e);
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
        textView.setMaxWidth(280);
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
        // 按照官方文档要求，销毁时释放语音识别资源
        if (speechManager != null) {
            speechManager.destroy();
        }
    }
} 