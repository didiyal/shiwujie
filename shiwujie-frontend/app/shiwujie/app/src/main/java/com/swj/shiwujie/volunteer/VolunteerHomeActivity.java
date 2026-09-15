package com.swj.shiwujie.volunteer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.swj.shiwujie.R;
import com.swj.shiwujie.databinding.ActivityVolunteerHomeBinding;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.common.utils.PermissionManager;
import com.swj.shiwujie.common.utils.EmergencyRingerManager;
import com.swj.shiwujie.common.utils.TTSManager;
import com.swj.shiwujie.common.ui.EmergencyHelpIncomingWindow;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.service.FloatingWindowService;
import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.SocketDataV0;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VolunteerHomeActivity extends AppCompatActivity {
    private static final String TAG = "VolunteerHomeActivity";
    private ActivityVolunteerHomeBinding binding;
    
    // 实名认证弹窗状态管理
    private boolean isIdentityDialogShowing = false;
    private AlertDialog currentIdentityDialog = null;
    private AlertDialog currentIdentityReminderDialog = null;
    
    // 弹窗类型枚举
    private enum DialogType {
        NONE,
        REMINDER,
        INPUT
    }
    private DialogType currentDialogType = DialogType.NONE;

    // 2026-09-16：紧急求助/匹配信令监听提升到 Activity 级——此前绑在 HomeFragment 生命周期，
    // tab 切到家庭/社区/我的后监听器被移除，紧急求助弹窗永远收不到
    private WebSocketManager webSocketManager;
    private WebSocketManager.MessageListener globalSignalListener;
    private EmergencyHelpIncomingWindow emergencyHelpIncomingWindow;
    private TTSManager ttsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVolunteerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 强制更新检查：服务端 versionCode 高于本地即弹不可取消更新弹窗
        com.swj.shiwujie.common.utils.UpdateManager.checkUpdate(this);

        // 志愿者端主页兜底：进入即停止AI悬浮球服务，避免盲人端残留
        try {
            stopService(new Intent(this, com.swj.shiwujie.common.service.AIFloatingBallService.class));
        } catch (Exception e) {
            Log.w(TAG, "停止AI悬浮球服务失败", e);
        }

        setupViews();

        // 检查权限
        checkPermissions();
        checkRingerPermissions();

        // 检查登录状态并建立WebSocket连接
        initWebSocketConnection();

        // 全局信令监听（Activity 级，全 tab/后台可用）
        initGlobalSignalListener();
        initTts();
    }

    /** 首次申请响铃/震动权限（紧急求助来电提醒用） */
    private void checkRingerPermissions() {
        if (!PermissionManager.hasRingerPermissions(this)) {
            PermissionManager.requestRingerPermissions(this);
        }
    }

    private void initTts() {
        try {
            ttsManager = new TTSManager(this);
        } catch (Exception e) {
            Log.e(TAG, "TTS初始化失败", e);
        }
    }

    /** 播报辅助：TTS 不可用时静默降级 */
    private void speak(String text) {
        if (ttsManager != null && text != null && !text.isEmpty()) {
            ttsManager.startSpeaking(text);
        }
    }

    /** Activity 级全局信令监听：紧急求助来电(3)/取消(4)/志愿者匹配成功(1) */
    private void initGlobalSignalListener() {
        webSocketManager = WebSocketManager.getInstance();
        globalSignalListener = new WebSocketManager.MessageListener() {
            @Override
            public void onMessageReceived(@NonNull SocketDataV0 data) {
                runOnUiThread(() -> handleGlobalSocketMessage(data));
            }
        };
        webSocketManager.addMessageListener(globalSignalListener);
    }

    private void handleGlobalSocketMessage(SocketDataV0 data) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Log.d(TAG, "全局信令: type=" + data.getRequestType()
                + ", 盲人手机号=" + data.getBlindPhone());

        if (data.getRequestType() == SocketDataV0.REQUEST_TYPE_EMERGENCY_INCOMING) {
            // 紧急求助来电：响铃 + 全屏弹窗（任意 tab/后台均可，依赖悬浮窗权限）
            if (PermissionManager.hasRingerPermissions(this)) {
                EmergencyRingerManager.getInstance().startEmergencyRinger(this);
            } else {
                Log.w(TAG, "缺少响铃权限，请求权限");
                PermissionManager.requestRingerPermissions(this);
            }
            if (emergencyHelpIncomingWindow == null) {
                emergencyHelpIncomingWindow = new EmergencyHelpIncomingWindow(this);
            }
            emergencyHelpIncomingWindow.setBlindPhone(data.getBlindPhone());
            emergencyHelpIncomingWindow.setOnAcceptListener(v -> {
                EmergencyRingerManager.getInstance().stopEmergencyRinger();
                respondToEmergencyHelp(data);
                emergencyHelpIncomingWindow.hide();
            });
            emergencyHelpIncomingWindow.setOnRejectListener(v -> {
                EmergencyRingerManager.getInstance().stopEmergencyRinger();
                emergencyHelpIncomingWindow.hide();
            });
            emergencyHelpIncomingWindow.show();
        } else if (data.getRequestType() == SocketDataV0.REQUEST_TYPE_EMERGENCY_CANCELLED) {
            // 求助取消/已被其他家属接通：停铃 + 收回弹窗 + 播报
            Log.d(TAG, "收到紧急求助收回通知，文案: " + data.getMessage());
            EmergencyRingerManager.getInstance().stopEmergencyRinger();
            if (emergencyHelpIncomingWindow != null) {
                emergencyHelpIncomingWindow.hide();
            }
            speak(data.getMessage() != null ? data.getMessage() : "紧急求助已取消");
        } else if (data.getRequestType() == SocketDataV0.REQUEST_TYPE_MATCH_SUCCESS) {
            // 志愿者匹配成功：停等待悬浮窗 → 回发视频初始化 → 跳视频页
            webSocketManager.setMatchingStatus(false);
            sendVideoInitMessage(data);
            try {
                stopService(new Intent(this, FloatingWindowService.class));
                Intent videoIntent = new Intent(this, com.swj.shiwujie.volunteer.VideoCallActivity.class);
                videoIntent.putExtra("channelId", data.getChannelId());
                videoIntent.putExtra("blindPhone", data.getBlindPhone());
                videoIntent.putExtra("volunteerPhone", data.getVolunteerPhone());
                startActivity(videoIntent);
            } catch (Exception e) {
                Log.e(TAG, "启动视频通话Activity失败", e);
            }
        }
    }

    /** 回发视频初始化成功（type=2），盲人端据此进入视频页 */
    private void sendVideoInitMessage(SocketDataV0 matchData) {
        try {
            SocketDataV0 initData = new SocketDataV0();
            initData.setRequestType(2);
            initData.setBlindPhone(matchData.getBlindPhone());
            initData.setVolunteerPhone(matchData.getVolunteerPhone());
            initData.setChannelId(matchData.getChannelId());
            webSocketManager.sendMessage(initData);
            Log.d(TAG, "视频初始化成功消息已发送");
        } catch (Exception e) {
            Log.e(TAG, "发送视频初始化消息失败: " + e.getMessage(), e);
        }
    }

    /** 家属接听紧急求助：登记响应后进入视频页 */
    private void respondToEmergencyHelp(SocketDataV0 data) {
        String blindPhone = data.getBlindPhone();
        if (blindPhone == null) {
            Toast.makeText(this, "盲人手机号为空，无法响应紧急求助", Toast.LENGTH_SHORT).show();
            return;
        }
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "登录状态异常，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }
        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        apiService.familyJoinUrgenthelp("Bearer " + token, blindPhone).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Boolean> result = response.body();
                    if (result.getCode() == 1 && Boolean.TRUE.equals(result.getData())) {
                        Toast.makeText(VolunteerHomeActivity.this, "响应成功，进入视频通话", Toast.LENGTH_SHORT).show();
                        if (emergencyHelpIncomingWindow != null) emergencyHelpIncomingWindow.hide();
                        Intent videoIntent = new Intent(VolunteerHomeActivity.this, com.swj.shiwujie.volunteer.VideoCallActivity.class);
                        videoIntent.putExtra("channelId", data.getChannelId());
                        videoIntent.putExtra("blindPhone", data.getBlindPhone());
                        videoIntent.putExtra("volunteerPhone", data.getVolunteerPhone());
                        videoIntent.putExtra("isEmergencyHelp", true);
                        startActivity(videoIntent);
                    } else {
                        Toast.makeText(VolunteerHomeActivity.this, "响应失败: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                        if (emergencyHelpIncomingWindow != null) emergencyHelpIncomingWindow.hide();
                        if (result.getMessage() != null && result.getMessage().contains("已有家属接通")) {
                            speak("已有其他家属接通本次求助");
                        } else {
                            speak("接听失败，请稍后再试");
                        }
                    }
                } else {
                    Toast.makeText(VolunteerHomeActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                Toast.makeText(VolunteerHomeActivity.this, "网络异常: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionManager.handlePermissionResult(requestCode, permissions, grantResults)) {
            Toast.makeText(this, "权限申请成功，紧急求助时会有响铃提醒", Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "响铃权限申请失败");
        }
    }

    
    @Override
    protected void onResume() {
        super.onResume();

        // 检查是否有未销毁的实名认证弹窗，如果有则先清理
        cleanupAbnormalDialogState();

        // 2026-09-14：实名认证已按产品要求关闭——志愿者端进入软件不再弹实名提醒。
        // 如需恢复，在此处查 getVolunteerVOById 后按 isIdCard 调 showIdentityVerificationReminder()
        // （弹窗逻辑未删）。
    }
    
    private void checkPermissions() {
        // 检查视频通话所需的所有权限（包括蓝牙权限）
        if (!PermissionManager.hasVideoCallPermissions(this)) {
            PermissionManager.showPermissionRequiredDialog(this, 
                "需要摄像头、麦克风和蓝牙权限才能使用视频通话功能。请在设置中开启相关权限。");
            return;
        }
        
        // 检查悬浮窗权限
        if (!PermissionManager.hasOverlayPermission(this)) {
            PermissionManager.showPermissionRequiredDialog(this, 
                "需要悬浮窗权限才能使用完整功能。请在设置中开启悬浮窗权限。");
            return;
        }
    }
    
    private void initWebSocketConnection() {
        try {
            if (SharedPrefsUtil.isLoggedIn()) {
                String phone = SharedPrefsUtil.getPhone();
                boolean isVolunteer = !SharedPrefsUtil.isBlind(); // 志愿者用户
                
                if (phone != null && !phone.isEmpty()) {
                    Log.d(TAG, "用户已登录，启动WebSocket前台服务 - 手机号: " + phone);
                    // 启动前台服务来维护WebSocket连接
                    com.swj.shiwujie.common.network.WebSocketService.startService(this);
                } else {
                    Log.w(TAG, "用户已登录但手机号为空");
                }
            } else {
                Log.d(TAG, "用户未登录，跳过WebSocket连接");
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化WebSocket连接失败", e);
        }
    }

    private void setupViews() {
        // 配置底部导航栏（2026-09-15：tabBar 主页/家庭/社区/我的，消息页下线）
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_family,
                R.id.navigation_community, R.id.navigation_profile)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    private void showIdCardInputDialog() {
        Log.d(TAG, "=== 显示身份证输入弹窗 ===");
        Log.d(TAG, "当前弹窗类型: " + currentDialogType);
        
        // 允许从提醒弹窗切换到输入弹窗
        if (currentDialogType == DialogType.INPUT) {
            Log.d(TAG, "输入弹窗已显示，跳过重复弹窗");
            return;
        }
        
        // 先销毁提醒弹窗，因为要切换到输入弹窗
        if (currentIdentityReminderDialog != null && currentIdentityReminderDialog.isShowing()) {
            Log.d(TAG, "销毁提醒弹窗，准备显示输入弹窗");
            currentIdentityReminderDialog.dismiss();
            currentIdentityReminderDialog = null;
        }
        
        // 更新弹窗类型
        currentDialogType = DialogType.INPUT;
        Log.d(TAG, "弹窗类型已更新为: " + currentDialogType);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_id_card_verification, null);
        builder.setView(dialogView);

        // 动态修改弹窗内容
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        EditText etIdCard = dialogView.findViewById(R.id.etIdCard);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        // 设置志愿者端的内容
        tvTitle.setText("实名认证");
        tvMessage.setText("请输入您的身份证号码进行实名认证");
        btnConfirm.setText("确认认证");
        
        // 设置身份证输入框的位数限制（18位）
        //etIdCard.setMaxLength(18);

        AlertDialog dialog = builder.create();
        currentIdentityDialog = dialog;
        dialog.setCancelable(false); // 不允许点击外部取消，与原有逻辑保持一致

        btnCancel.setOnClickListener(v -> {
            // 取消认证，退出APP
            com.swj.shiwujie.common.network.ApiService apiService = com.swj.shiwujie.common.network.RetrofitClient.getInstance().createService(com.swj.shiwujie.common.network.ApiService.class);
            String token = com.swj.shiwujie.common.utils.SharedPrefsUtil.getToken();
            apiService.volunteerLogout("Bearer " + token).enqueue(new com.swj.shiwujie.common.network.ApiCallback<Boolean>(VolunteerHomeActivity.this) {
                @Override
                public void onSuccess(Boolean data) {
                    setClearConversationFlag();
                    com.swj.shiwujie.common.utils.SharedPrefsUtil.clearAll();
                    finishAffinity();
                }
                @Override
                public void onError(String message) {
                    setClearConversationFlag();
                    com.swj.shiwujie.common.utils.SharedPrefsUtil.clearAll();
                    finishAffinity();
                }
            });
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            String idCard = etIdCard.getText().toString().trim();

            // 验证身份证号
            if (TextUtils.isEmpty(idCard)) {
                Toast.makeText(this, "身份证号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用实名认证API
            performIdCardVerification(idCard, dialog);
        });

        dialog.setOnDismissListener(dialogInterface -> {
            // 弹窗被销毁时重置状态
            if (currentDialogType == DialogType.INPUT) {
                currentDialogType = DialogType.NONE;
            }
            currentIdentityDialog = null;
        });

        Log.d(TAG, "显示身份证输入弹窗");
        dialog.show();
    }

    private void performIdCardVerification(String idCard, AlertDialog dialog) {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(this, "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建请求体，更新身份证号并设置默认gender
        com.swj.shiwujie.data.model.VolunteerVO volunteer = new com.swj.shiwujie.data.model.VolunteerVO();
        volunteer.setVolunteerId(userId);
        volunteer.setIdCard(idCard);
        volunteer.setGender(0); // 默认设置为0（男性）

        // 调用更新用户信息的API
        com.swj.shiwujie.common.network.ApiService apiService = com.swj.shiwujie.common.network.RetrofitClient.getInstance().createService(com.swj.shiwujie.common.network.ApiService.class);
        apiService.updateVolunteerInfo(
                "Bearer " + token,
                volunteer
        ).enqueue(new com.swj.shiwujie.common.network.ApiCallback<Boolean>(this) {
            @Override
            public void onSuccess(Boolean response) {
                Toast.makeText(VolunteerHomeActivity.this, "实名认证成功", Toast.LENGTH_SHORT).show();
                
                // 实名认证成功后，销毁所有实名认证弹窗并重置状态
                destroyAllIdentityDialogs();
                
                // 更新本地存储的认证状态
                SharedPrefsUtil.setBoolean("isIdCard", true);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(VolunteerHomeActivity.this, "实名认证失败：" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 销毁所有实名认证弹窗
        destroyAllIdentityDialogs();

        // 全局信令监听与紧急来电资源清理（2026-09-16）
        if (webSocketManager != null && globalSignalListener != null) {
            webSocketManager.removeMessageListener(globalSignalListener);
        }
        EmergencyRingerManager.getInstance().stopEmergencyRinger();
        if (emergencyHelpIncomingWindow != null) {
            emergencyHelpIncomingWindow.destroy();
            emergencyHelpIncomingWindow = null;
        }
        if (ttsManager != null) {
            ttsManager.destroy();
            ttsManager = null;
        }

        binding = null;

        // 停止WebSocket前台服务
        com.swj.shiwujie.common.network.WebSocketService.stopService(this);
    }
    
    /**
     * 设置清理AI对话的标记
     */
    private void setClearConversationFlag() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("ai_conversation_history", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("should_clear_conversation", true);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "设置清理对话标记失败", e);
        }
    }

    private void showIdentityVerificationReminder() {
        Log.d(TAG, "=== 显示实名认证提醒弹窗 ===");
        Log.d(TAG, "当前弹窗类型: " + currentDialogType);
        
        // 如果已经有弹窗在显示，直接返回
        if (currentDialogType != DialogType.NONE) {
            Log.d(TAG, "实名认证弹窗已显示，跳过重复弹窗");
            return;
        }
        
        currentDialogType = DialogType.REMINDER;
        Log.d(TAG, "弹窗类型已更新为: " + currentDialogType);
        
        currentIdentityReminderDialog = new AlertDialog.Builder(VolunteerHomeActivity.this)
            .setTitle("实名认证提醒")
            .setMessage("您还未完成实名认证，请先进行实名校验")
            .setCancelable(false)
            .setPositiveButton("去实名", (dialog, which) -> {
                Log.d(TAG, "用户点击去实名按钮");
                // 切换到输入弹窗
                showIdCardInputDialog();
            })
            .setNegativeButton("退出APP", (dialog, which) -> {
                com.swj.shiwujie.common.network.ApiService apiService = com.swj.shiwujie.common.network.RetrofitClient.getInstance().createService(com.swj.shiwujie.common.network.ApiService.class);
                String token = com.swj.shiwujie.common.utils.SharedPrefsUtil.getToken();
                apiService.volunteerLogout("Bearer " + token).enqueue(new com.swj.shiwujie.common.network.ApiCallback<Boolean>(VolunteerHomeActivity.this) {
                    @Override
                    public void onSuccess(Boolean data) {
                        setClearConversationFlag();
                        com.swj.shiwujie.common.utils.SharedPrefsUtil.clearAll();
                        finishAffinity();
                    }
                    @Override
                    public void onError(String message) {
                        setClearConversationFlag();
                        com.swj.shiwujie.common.utils.SharedPrefsUtil.clearAll();
                        finishAffinity();
                    }
                });
            })
            .setOnDismissListener(dialog -> {
                // 弹窗被销毁时重置状态
                if (currentDialogType == DialogType.REMINDER) {
                    currentDialogType = DialogType.NONE;
                }
                currentIdentityReminderDialog = null;
            })
            .show();
            
        Log.d(TAG, "实名认证提醒弹窗已显示");
    }

    /**
     * 销毁所有实名认证弹窗
     */
    private void destroyAllIdentityDialogs() {
        if (currentIdentityDialog != null && currentIdentityDialog.isShowing()) {
            currentIdentityDialog.dismiss();
            currentIdentityDialog = null;
        }
        if (currentIdentityReminderDialog != null && currentIdentityReminderDialog.isShowing()) {
            currentIdentityReminderDialog.dismiss();
            currentIdentityReminderDialog = null;
        }
        currentDialogType = DialogType.NONE; // 重置弹窗类型
    }
    
    /**
     * 检查是否有活跃的实名认证弹窗
     */
    private boolean hasActiveIdentityDialog() {
        return currentDialogType != DialogType.NONE;
    }
    
    /**
     * 清理异常弹窗状态
     */
    private void cleanupAbnormalDialogState() {
        // 检查弹窗状态是否一致
        boolean hasActiveDialog = false;
        if (currentIdentityReminderDialog != null && currentIdentityReminderDialog.isShowing()) {
            hasActiveDialog = true;
        }
        if (currentIdentityDialog != null && currentIdentityDialog.isShowing()) {
            hasActiveDialog = true;
        }
        
        // 如果状态不一致，重置所有状态
        if (currentDialogType != DialogType.NONE && !hasActiveDialog) {
            Log.d(TAG, "检测到弹窗状态异常，重置状态");
            currentDialogType = DialogType.NONE;
            currentIdentityReminderDialog = null;
            currentIdentityDialog = null;
        }
    }
} 