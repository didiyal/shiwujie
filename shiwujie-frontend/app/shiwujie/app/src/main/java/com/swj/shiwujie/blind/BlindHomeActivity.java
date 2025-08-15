package com.swj.shiwujie.blind;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.swj.shiwujie.R;
import com.swj.shiwujie.databinding.ActivityBlindHomeBinding;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.common.utils.PermissionManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.data.model.BlindVO;

public class BlindHomeActivity extends AppCompatActivity {
    private static final String TAG = "BlindHomeActivity";
    private ActivityBlindHomeBinding binding;
    
    // 身份校验弹窗状态管理
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlindHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
        
        // 检查权限
        checkPermissions();
        
        // 检查登录状态并建立WebSocket连接
        initWebSocketConnection();
        
        // ===== 障碍物检测功能集成 - 严格按照backend_service.py的集成逻辑 =====
        // 改造说明：将Python后端的障碍物检测功能集成到Android原生界面

        
        // 检查是否需要开启连线志愿者功能
        checkAndEnableVolunteerConnection();
        
        // 检查是否需要开启紧急求助功能
        checkAndEnableEmergencyHelp();
    }
    
    /**
     * 检查并开启连线志愿者功能
     */
    private void checkAndEnableVolunteerConnection() {
        try {
            // 检查是否从AI页面传递了自动执行连线志愿者的标记
            boolean fromAiVolunteerConnection = getIntent().getBooleanExtra("from_ai_volunteer_connection", false);
            
            if (fromAiVolunteerConnection) {
                Log.d(TAG, "收到从AI页面跳转过来的连线志愿者请求，准备自动执行");
                
                // 延迟执行，确保页面完全加载
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // 导航到home fragment
                        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                        if (navController != null) {
                            navController.navigate(R.id.navigation_home);
                            Log.d(TAG, "已导航到home fragment，准备自动执行连线志愿者功能");
                            
                            // 显示连线志愿者提示
                            Toast.makeText(BlindHomeActivity.this, "正在自动连线志愿者...", Toast.LENGTH_LONG).show();
                            
                            // 延迟执行连线志愿者功能，确保Fragment完全加载
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    // 自动执行连线志愿者功能
                                    autoExecuteVolunteerConnection();
                                } catch (Exception e) {
                                    Log.e(TAG, "自动执行连线志愿者失败", e);
                                    Toast.makeText(BlindHomeActivity.this, "自动连线志愿者失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }, 2000); // 延迟2秒执行，确保Fragment完全加载
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "自动执行连线志愿者功能失败", e);
                    }
                }, 500); // 延迟500ms执行
                
                // 清除标记，避免重复处理
                getIntent().removeExtra("from_ai_volunteer_connection");
            }
        } catch (Exception e) {
            Log.e(TAG, "检查AI页面连线志愿者标记失败", e);
        }
    }
    
    /**
     * 自动执行连线志愿者功能
     */
    private void autoExecuteVolunteerConnection() {
        try {
            Log.d(TAG, "开始自动执行连线志愿者功能");
            
            // 延迟执行，确保Fragment完全加载
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    // 获取当前显示的Fragment
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                    if (navController != null) {
                        // 通过NavHostFragment获取当前显示的Fragment
                        androidx.fragment.app.Fragment currentFragment = null;
                        try {
                            // 获取NavHostFragment
                            androidx.navigation.fragment.NavHostFragment navHostFragment = 
                                (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.nav_host_fragment_activity_main);
                            
                            if (navHostFragment != null) {
                                // 获取当前显示的Fragment
                                currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
                                Log.d(TAG, "通过NavHostFragment获取到Fragment: " + 
                                    (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "通过NavHostFragment获取Fragment失败", e);
                        }
                        
                        // 添加调试信息
                        Log.d(TAG, "当前Fragment: " + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                        if (currentFragment != null) {
                            Log.d(TAG, "Fragment是否已添加: " + currentFragment.isAdded());
                            Log.d(TAG, "Fragment是否可见: " + currentFragment.isVisible());
                        }
                        
                        if (currentFragment instanceof com.swj.shiwujie.blind.ui.home.HomeFragment) {
                            // 直接调用HomeFragment的连线志愿者方法
                            com.swj.shiwujie.blind.ui.home.HomeFragment homeFragment = 
                                (com.swj.shiwujie.blind.ui.home.HomeFragment) currentFragment;
                            
                            // 通过反射调用startVideoHelpMatching方法
                            try {
                                java.lang.reflect.Method method = homeFragment.getClass()
                                    .getDeclaredMethod("startVideoHelpMatching");
                                method.setAccessible(true);
                                method.invoke(homeFragment);
                                
                                Log.d(TAG, "自动连线志愿者功能已执行");
                                Toast.makeText(this, "正在自动连线志愿者...", Toast.LENGTH_LONG).show();
                                
                            } catch (Exception e) {
                                Log.e(TAG, "通过反射调用连线志愿者方法失败", e);
                                Toast.makeText(this, "自动连线志愿者失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "当前Fragment不是HomeFragment，无法执行连线志愿者功能");
                            Toast.makeText(this, "页面状态异常，无法执行连线志愿者功能", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "自动执行连线志愿者功能异常", e);
                    Toast.makeText(this, "自动连线志愿者失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, 500); // 延迟500ms执行，确保Fragment完全加载
            
        } catch (Exception e) {
            Log.e(TAG, "自动执行连线志愿者功能异常", e);
            Toast.makeText(this, "自动连线志愿者失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 检查并开启紧急求助功能
     */
    private void checkAndEnableEmergencyHelp() {
        try {
            // 检查是否从AI页面传递了自动执行紧急求助的标记
            boolean fromAiEmergencyHelp = getIntent().getBooleanExtra("from_ai_emergency_help", false);
            
            if (fromAiEmergencyHelp) {
                Log.d(TAG, "收到从AI页面跳转过来的紧急求助请求，准备自动执行");
                
                // 延迟执行，确保页面完全加载
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // 导航到home fragment
                        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                        if (navController != null) {
                            navController.navigate(R.id.navigation_home);
                            Log.d(TAG, "已导航到home fragment，准备自动执行紧急求助");
                            
                            // 显示紧急求助提示
                            Toast.makeText(BlindHomeActivity.this, "正在自动发起紧急求助...", Toast.LENGTH_LONG).show();
                            
                            // 延迟执行紧急求助功能，确保Fragment完全加载
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    // 自动执行紧急求助功能
                                    autoExecuteEmergencyHelp();
                                } catch (Exception e) {
                                    Log.e(TAG, "自动执行紧急求助失败", e);
                                    Toast.makeText(BlindHomeActivity.this, "自动紧急求助失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }, 2000); // 延迟2秒执行，确保Fragment完全加载
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "自动执行紧急求助功能失败", e);
                    }
                }, 500); // 延迟500ms执行
                
                // 清除标记，避免重复处理
                getIntent().removeExtra("from_ai_emergency_help");
            }
        } catch (Exception e) {
            Log.e(TAG, "检查AI页面紧急求助标记失败", e);
        }
    }
    
    /**
     * 自动执行紧急求助功能
     */
    private void autoExecuteEmergencyHelp() {
        try {
            Log.d(TAG, "开始自动执行紧急求助功能");
            
            // 延迟执行，确保Fragment完全加载
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    // 获取当前显示的Fragment
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                    if (navController != null) {
                        // 通过NavHostFragment获取当前显示的Fragment
                        androidx.fragment.app.Fragment currentFragment = null;
                        try {
                            // 获取NavHostFragment
                            androidx.navigation.fragment.NavHostFragment navHostFragment = 
                                (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.nav_host_fragment_activity_main);
                            
                            if (navHostFragment != null) {
                                // 获取当前显示的Fragment
                                currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
                                Log.d(TAG, "通过NavHostFragment获取到Fragment: " + 
                                    (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "通过NavHostFragment获取Fragment失败", e);
                        }
                        
                        // 添加调试信息
                        Log.d(TAG, "当前Fragment: " + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                        if (currentFragment != null) {
                            Log.d(TAG, "Fragment是否已添加: " + currentFragment.isAdded());
                            Log.d(TAG, "Fragment是否可见: " + currentFragment.isVisible());
                        }
                        
                        if (currentFragment instanceof com.swj.shiwujie.blind.ui.home.HomeFragment) {
                            // 直接调用HomeFragment的紧急求助方法
                            com.swj.shiwujie.blind.ui.home.HomeFragment homeFragment = 
                                (com.swj.shiwujie.blind.ui.home.HomeFragment) currentFragment;
                            
                            // 通过反射调用startEmergencyHelp方法
                            try {
                                java.lang.reflect.Method method = homeFragment.getClass()
                                    .getDeclaredMethod("startEmergencyHelp");
                                method.setAccessible(true);
                                method.invoke(homeFragment);
                                
                                Log.d(TAG, "自动紧急求助功能已执行");
                                Toast.makeText(this, "正在自动发起紧急求助...", Toast.LENGTH_LONG).show();
                                
                            } catch (Exception e) {
                                Log.e(TAG, "通过反射调用紧急求助方法失败", e);
                                Toast.makeText(this, "自动紧急求助失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "当前Fragment不是HomeFragment，无法执行紧急求助功能");
                            Toast.makeText(this, "页面状态异常，无法执行紧急求助功能", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "自动执行紧急求助功能异常", e);
                    Toast.makeText(this, "自动紧急求助失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, 500); // 延迟500ms执行，确保Fragment完全加载
            
        } catch (Exception e) {
            Log.e(TAG, "自动执行紧急求助功能异常", e);
            Toast.makeText(this, "自动紧急求助失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
                boolean isVolunteer = !SharedPrefsUtil.isBlind(); // 盲人用户
                
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

    @Override
    protected void onResume() {
        super.onResume();
        
        // 检查是否有未销毁的身份校验弹窗，如果有则先清理
        cleanupAbnormalDialogState();
        
        // 全局身份校验监听 - 每次都从服务器获取最新状态
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();
        if (token == null || userId == null) return;

        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        apiService.getBlindById("Bearer " + token, userId).enqueue(new ApiCallback<BlindVO>(this) {
            @Override
            public void onSuccess(BlindVO data) {
                boolean isVerified = data.getIsDisabilityCard() != null && data.getIsDisabilityCard();
                SharedPrefsUtil.setBoolean("isDisabilityCard", isVerified);
                if (!isVerified && currentDialogType == DialogType.NONE) {
                    showIdentityVerificationReminder();
                }
            }
            
            @Override
            public void onError(String message) {
                // 如果获取失败，默认显示校验提醒，但要检查是否已有弹窗
                if (currentDialogType == DialogType.NONE) {
                    showIdentityVerificationReminder();
                }
            }
        });
    }

    /**
     * 销毁所有身份校验弹窗
     */
    private void destroyAllIdentityDialogs() {
        if (currentIdentityReminderDialog != null && currentIdentityReminderDialog.isShowing()) {
            currentIdentityReminderDialog.dismiss();
            currentIdentityReminderDialog = null;
        }
        if (currentIdentityDialog != null && currentIdentityDialog.isShowing()) {
            currentIdentityDialog.dismiss();
            currentIdentityDialog = null;
        }
        currentDialogType = DialogType.NONE;
    }
    
    /**
     * 检查是否有活跃的身份校验弹窗
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 销毁所有身份校验弹窗
        destroyAllIdentityDialogs();
        
        binding = null;
        
        // 停止WebSocket前台服务
        com.swj.shiwujie.common.network.WebSocketService.stopService(this);
    }


    
    private void showIdentityVerificationReminder() {
        Log.d(TAG, "=== 显示身份校验提醒弹窗 ===");
        Log.d(TAG, "当前弹窗类型: " + currentDialogType);
        
        // 如果已经有弹窗在显示，直接返回
        if (currentDialogType != DialogType.NONE) {
            Log.d(TAG, "身份校验弹窗已显示，跳过重复弹窗");
            return;
        }
        
        currentDialogType = DialogType.REMINDER;
        Log.d(TAG, "弹窗类型已更新为: " + currentDialogType);
        
        currentIdentityReminderDialog = new AlertDialog.Builder(BlindHomeActivity.this)
            .setTitle("身份校验提醒")
            .setMessage("您还未完成身份校验，请先补全残疾证证件")
            .setCancelable(false)
            .setPositiveButton("去校验", (dialog, which) -> {
                Log.d(TAG, "用户点击去校验按钮");
                // 切换到输入弹窗
                showIdentityVerificationDialog();
            })
            .setNegativeButton("退出APP", (dialog, which) -> {
                // 设置清理AI对话的标记
                setClearConversationFlag();
                // 直接退出APP，不调用logout API
                SharedPrefsUtil.clearAll();
                finishAffinity();
            })
            .setOnDismissListener(dialog -> {
                // 弹窗被销毁时重置状态
                if (currentDialogType == DialogType.REMINDER) {
                    currentDialogType = DialogType.NONE;
                }
                currentIdentityReminderDialog = null;
            })
            .show();
            
        Log.d(TAG, "身份校验提醒弹窗已显示");
    }

    private void showIdentityVerificationDialog() {
        Log.d(TAG, "=== 显示身份校验输入弹窗 ===");
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_id_card_verification, null);
        builder.setView(dialogView);

        // 动态修改弹窗内容
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        EditText etInput = dialogView.findViewById(R.id.etIdCard);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        // 设置盲人端的内容
        tvTitle.setText("身份校验");
        tvMessage.setText("请输入您的残疾证号码进行身份校验");
        btnConfirm.setText("确认校验");
        
        // 设置残疾证输入框的位数限制（20位：18位身份证+1位类别+1位等级）
        //etInput.setMaxLength(20);

        AlertDialog dialog = builder.create();
        currentIdentityDialog = dialog;

        btnCancel.setOnClickListener(v -> {
            // 点击取消直接退出APP
            SharedPrefsUtil.clearAll();
            finishAffinity();
        });
        
        btnConfirm.setOnClickListener(v -> {
            String disabilityCard = etInput.getText().toString().trim();
            if (disabilityCard.isEmpty()) {
                Toast.makeText(this, "残疾证号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            performDisabilityCardVerification(disabilityCard, dialog);
        });

        dialog.setOnDismissListener(dialogInterface -> {
            // 弹窗被销毁时重置状态
            if (currentDialogType == DialogType.INPUT) {
                currentDialogType = DialogType.NONE;
            }
            currentIdentityDialog = null;
        });

        Log.d(TAG, "显示身份校验输入弹窗");
        dialog.show();
    }


    
    private void performDisabilityCardVerification(String disabilityCard, AlertDialog dialog) {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(this, "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        BlindVO blind = new BlindVO();
        blind.setBlindId(userId);
        blind.setDisabilityCard(disabilityCard);
        blind.setGender(0); // 默认设置为0（男性）

        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        apiService.updateBlindInfo(
                "Bearer " + token,
                blind
        ).enqueue(new ApiCallback<Boolean>(this) {
            @Override
            public void onSuccess(Boolean response) {
                Toast.makeText(BlindHomeActivity.this, "残疾证校验成功", Toast.LENGTH_SHORT).show();
                
                // 身份校验成功后，销毁所有身份校验弹窗并重置状态
                destroyAllIdentityDialogs();
                
                // 更新本地存储的认证状态
                SharedPrefsUtil.setBoolean("isDisabilityCard", true);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(BlindHomeActivity.this, "残疾证校验失败：" + message, Toast.LENGTH_SHORT).show();
            }
        });
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
} 