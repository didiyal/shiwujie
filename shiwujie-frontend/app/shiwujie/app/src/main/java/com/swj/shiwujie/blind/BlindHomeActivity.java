package com.swj.shiwujie.blind;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.data.model.BlindVO;
import com.swj.shiwujie.common.utils.AppListManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    
    // 应用列表管理相关
    private AppListManager appListManager;
    private ExecutorService appListExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlindHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
        
        // 检查是否需要直接跳转到AI页面，如果是则延迟导航到AI页面
        Intent intent = getIntent();
        
        // 验证用户身份：只有盲人端用户才能访问此页面
        if (intent != null && "volunteer".equals(intent.getStringExtra("user_type"))) {
            Log.w(TAG, "志愿者端用户尝试访问盲人端页面，拒绝访问");
            finish();
            return;
        }
        
        if (intent != null && intent.getBooleanExtra("direct_to_ai", false)) {
            Log.d(TAG, "检测到直接跳转AI页面标记，延迟导航到AI页面");
            // 延迟导航，确保导航状态完全初始化
            new android.os.Handler().postDelayed(() -> {
                try {
                    Log.d(TAG, "=== AI悬浮球跳转调试 ===");
                    Log.d(TAG, "开始执行导航到AI页面...");
                    
                    // 获取当前页面信息
                    int currentDestinationId = navController.getCurrentDestination() != null ? 
                        navController.getCurrentDestination().getId() : -1;
                    Log.d(TAG, "导航前当前页面ID: " + currentDestinationId);
                    
                    // 执行导航
                    navController.navigate(R.id.navigation_ai);
                    Log.d(TAG, "导航命令已执行，目标页面: navigation_ai");
                    
                    // 同步更新底部导航栏状态
                    binding.navView.setSelectedItemId(R.id.navigation_ai);
                    Log.d(TAG, "底部导航栏状态已更新为AI页面");
                    
                    Log.d(TAG, "=== AI悬浮球跳转完成 ===");
                } catch (Exception e) {
                    Log.e(TAG, "导航到AI页面失败", e);
                }
            }, 200); // 延迟200ms，确保导航状态稳定
            // 清除标记
            intent.removeExtra("direct_to_ai");
        }
        
        // 检查是否需要跳转到AI页面
        checkNavigateToAI();
        
        // 添加导航监听器，实现TalkBack焦点转移和调试日志
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // 打印当前页面信息
            String currentPageName = destination.getLabel() != null ? destination.getLabel().toString() : "未知页面";
            int currentPageId = destination.getId();
            Log.d(TAG, "=== 页面切换监听器 ===");
            Log.d(TAG, "当前页面: " + currentPageName + " (ID: " + currentPageId + ")");
            Log.d(TAG, "页面参数: " + (arguments != null ? arguments.toString() : "无"));
            
            // 延迟一点时间确保页面加载完成
            new android.os.Handler().postDelayed(() -> {
                // 获取当前页面的根布局
                View currentFragmentView = findViewById(R.id.nav_host_fragment_activity_main);
                if (currentFragmentView != null) {
                    // 找到当前Fragment的根视图
                    ViewGroup fragmentContainer = (ViewGroup) currentFragmentView;
                    if (fragmentContainer.getChildCount() > 0) {
                        View fragmentRoot = fragmentContainer.getChildAt(0);
                        // 强制聚焦到页面根布局
                        fragmentRoot.requestFocus();
                        // 可选：引导焦点向下移动
                        fragmentRoot.setNextFocusDownId(fragmentRoot.getId());
                    }
                }
            }, 300); // 延迟300毫秒
        });
        
        // 添加底部导航栏点击监听器，记录用户点击行为
        binding.navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            String itemTitle = item.getTitle() != null ? item.getTitle().toString() : "未知";
            
            Log.d(TAG, "=== 底部导航栏点击事件 ===");
            Log.d(TAG, "用户点击了: " + itemTitle + " (ID: " + itemId + ")");
            
            // 获取当前选中的页面
            int currentSelectedId = binding.navView.getSelectedItemId();
            String currentSelectedTitle = "";
            for (int i = 0; i < binding.navView.getMenu().size(); i++) {
                if (binding.navView.getMenu().getItem(i).getItemId() == currentSelectedId) {
                    currentSelectedTitle = binding.navView.getMenu().getItem(i).getTitle().toString();
                    break;
                }
            }
            Log.d(TAG, "点击前当前页面: " + currentSelectedTitle + " (ID: " + currentSelectedId + ")");
            
            // 直接使用navController.navigate确保页面跳转
            try {
                Log.d(TAG, "开始执行页面跳转: " + itemTitle + " (ID: " + itemId + ")");
                navController.navigate(itemId);
                Log.d(TAG, "页面跳转命令已执行");
            } catch (Exception e) {
                Log.e(TAG, "页面跳转失败: " + e.getMessage(), e);
            }
            
            return true; // 返回true表示已处理点击事件
        });
        
        // 检查权限，如果权限不足则延迟初始化
        if (checkPermissions()) {
            // 权限充足，延迟初始化确保页面完全加载完成
            Log.d(TAG, "权限充足，延迟初始化确保页面完全加载");
            new android.os.Handler().postDelayed(() -> {
                try {
                    initializeAfterPermissions();
                } catch (Exception e) {
                    Log.e(TAG, "延迟初始化失败", e);
                }
            }, 1000); // 延迟1秒，确保页面完全加载
        } else {
            // 权限不足，等待权限授权后再初始化
            Log.d(TAG, "权限不足，等待权限授权后再初始化");
        }
        
        // ===== 障碍物检测功能集成 - 严格按照backend_service.py的集成逻辑 =====
        // 改造说明：将Python后端的障碍物检测功能集成到Android原生界面

    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent被调用，处理AI悬浮球跳转");
        
        // 设置新的Intent
        setIntent(intent);
        
        // 检查是否需要跳转到AI页面
        if (intent != null && intent.getBooleanExtra("navigate_to_ai", false)) {
            Log.d(TAG, "检测到onNewIntent中的跳转标记，准备跳转到AI页面");
            
            // 延迟导航，确保导航状态完全初始化
            new android.os.Handler().postDelayed(() -> {
                try {
                    Log.d(TAG, "=== onNewIntent AI悬浮球跳转调试 ===");
                    
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                    
                    // 获取当前页面信息
                    int currentDestinationId = navController.getCurrentDestination() != null ? 
                        navController.getCurrentDestination().getId() : -1;
                    Log.d(TAG, "onNewIntent导航前当前页面ID: " + currentDestinationId);
                    
                    // 执行导航
                    navController.navigate(R.id.navigation_ai);
                    Log.d(TAG, "onNewIntent导航命令已执行，目标页面: navigation_ai");
                    
                    // 同步更新底部导航栏状态
                    binding.navView.setSelectedItemId(R.id.navigation_ai);
                    Log.d(TAG, "onNewIntent底部导航栏状态已更新为AI页面");
                    
                    // 清除跳转标记
                    intent.removeExtra("navigate_to_ai");
                    intent.removeExtra("direct_to_ai");
                    
                    Log.d(TAG, "=== onNewIntent AI悬浮球跳转完成 ===");
                } catch (Exception e) {
                    Log.e(TAG, "onNewIntent导航到AI页面失败", e);
                }
            }, 200); // 延迟200ms，确保导航状态稳定
        }
    }
    
    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        Log.d(TAG, "=== 权限请求结果回调 ===");
        Log.d(TAG, "请求码: " + requestCode);
        Log.d(TAG, "权限数量: " + permissions.length);
        
        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE) {
            boolean success = PermissionManager.handlePermissionResult(requestCode, permissions, grantResults);
            
            if (success) {
                Log.d(TAG, "权限授权成功，开始初始化...");
                // 权限授权成功，延迟更长时间确保权限状态和系统资源完全稳定
                new android.os.Handler().postDelayed(() -> {
                    try {
                        Log.d(TAG, "延迟初始化开始，权限状态应该已稳定");
                        initializeAfterPermissions();
                    } catch (Exception e) {
                        Log.e(TAG, "权限授权后初始化失败", e);
                    }
                }, 1500); // 延迟1.5秒，确保权限状态和系统资源完全稳定
            } else {
                Log.w(TAG, "权限授权失败或被拒绝");
                // 权限被拒绝，显示提示或退出
                // Toast.makeText(this, "权限不足，应用无法正常运行", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private boolean checkPermissions() {
        // 检查视频通话所需的所有权限（包括蓝牙权限）
        if (!PermissionManager.hasVideoCallPermissions(this)) {
            PermissionManager.showPermissionRequiredDialog(this, 
                "需要摄像头、麦克风和蓝牙权限才能使用视频通话功能。请在设置中开启相关权限。");
            return false;
        }
        
        // 检查悬浮窗权限
        if (!PermissionManager.hasOverlayPermission(this)) {
            PermissionManager.showPermissionRequiredDialog(this, 
                "需要悬浮窗权限才能使用完整功能。请在设置中开启悬浮窗权限。");
            return false;
        }
        
        return true;
    }
    
    /**
     * 权限充足后的初始化方法
     */
    private void initializeAfterPermissions() {
        try {
            Log.d(TAG, "权限充足，开始初始化...");
            
            // 检查登录状态并建立WebSocket连接
            initWebSocketConnection();
            
            // 初始化应用列表管理器
            initAppListManager();
            
            // 检查是否需要跳转到AI页面
            checkNavigateToAI();
            
            // 延迟启动AI悬浮球服务（盲人端专属），确保其他初始化完成且系统稳定
            new android.os.Handler().postDelayed(() -> {
                try {
                    // 仅在已登录且为盲人端时启动；否则兜底停止
                    if (SharedPrefsUtil.isLoggedIn() && SharedPrefsUtil.isBlind()) {
                        Log.d(TAG, "延迟启动AI悬浮球服务（盲人端专属），系统应该已稳定");
                        startAIFloatingBallService();
                    } else {
                        Log.d(TAG, "当前非盲人端或未登录，停止AI悬浮球服务（兜底）");
                        stopService(new Intent(this, com.swj.shiwujie.common.service.AIFloatingBallService.class));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "延迟启动/停止AI悬浮球服务失败", e);
                }
            }, 1000); // 再延迟1秒启动前台服务
            
            Log.d(TAG, "权限充足后的初始化完成（前台服务将在1秒后启动）");
        } catch (Exception e) {
            Log.e(TAG, "权限充足后的初始化失败", e);
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
    
    /**
     * 检查是否需要跳转到AI页面
     */
    private void checkNavigateToAI() {
        try {
            Intent intent = getIntent();
            if (intent != null && intent.getBooleanExtra("navigate_to_ai", false)) {
                Log.d(TAG, "检测到跳转标记，准备跳转到AI页面");
                
                // 检查是否是直接跳转，如果是则立即跳转，避免中间过渡页面
                boolean isDirectToAI = intent.getBooleanExtra("direct_to_ai", false);
                long delay = isDirectToAI ? 100 : 1000; // 直接跳转延迟100ms，普通跳转延迟1秒
                
                new android.os.Handler().postDelayed(() -> {
                    try {
                        // 使用Navigation跳转到AI页面
                        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                        navController.navigate(R.id.navigation_ai);
                        
                        // 清除跳转标记
                        intent.removeExtra("navigate_to_ai");
                        intent.removeExtra("direct_to_ai");
                        
                        Log.d(TAG, "成功跳转到AI页面");
                    } catch (Exception e) {
                        Log.e(TAG, "跳转到AI页面失败", e);
                    }
                }, delay);
            }
        } catch (Exception e) {
            Log.e(TAG, "检查跳转标记失败", e);
        }
    }
    
    /**
     * 启动AI悬浮球服务
     */
    private void startAIFloatingBallService() {
        try {
            Log.d(TAG, "启动AI悬浮球服务（全局显示）");
            
            Intent serviceIntent = new Intent(this, com.swj.shiwujie.common.service.AIFloatingBallService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            Log.d(TAG, "AI悬浮球服务启动成功");
            
        } catch (Exception e) {
            Log.e(TAG, "启动AI悬浮球服务失败", e);
        }
    }
    
    /**
     * 初始化应用列表管理器
     * 在独立的子线程中执行，避免影响主线程和现有功能
     */
    private void initAppListManager() {
        try {
            Log.d(TAG, "开始初始化应用列表管理器...");
            
            // 创建专用的线程池，与现有功能完全隔离
            appListExecutor = Executors.newSingleThreadExecutor();
            appListManager = new AppListManager(this);
            
            // 在子线程中获取应用列表，避免阻塞主线程
            appListExecutor.execute(() -> {
                try {
                    Log.d(TAG, "在子线程中开始获取应用列表...");
                    appListManager.loadInstalledApps();
                    Log.d(TAG, "应用列表获取完成，缓存数量: " + appListManager.getCachedAppCount());
                } catch (Exception e) {
                    Log.e(TAG, "在子线程中获取应用列表失败", e);
                    Log.e(TAG, "错误详情: " + e.getMessage());
                }
            });
            
            Log.d(TAG, "应用列表管理器初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "初始化应用列表管理器失败", e);
            Log.e(TAG, "错误详情: " + e.getMessage());
            // 即使失败也不影响其他功能，继续正常运行
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
        
        // 测试应用列表管理器功能（延迟执行，确保初始化完成）
        new android.os.Handler().postDelayed(() -> {
            if (appListManager != null && appListManager.isInitialized()) {
                testAppListManager();
            } else {
                Log.d(TAG, "应用列表管理器尚未初始化完成，跳过测试");
            }
        }, 3000); // 延迟3秒执行，确保应用列表加载完成
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
        
        // 清理应用列表管理器资源
        cleanupAppListManager();
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
                // Toast.makeText(BlindHomeActivity.this, "残疾证校验成功", Toast.LENGTH_SHORT).show();
                
                // 身份校验成功后，销毁所有身份校验弹窗并重置状态
                destroyAllIdentityDialogs();
                
                // 更新本地存储的认证状态
                SharedPrefsUtil.setBoolean("isDisabilityCard", true);
            }

            @Override
            public void onError(String message) {
                // Toast.makeText(BlindHomeActivity.this, "残疾证校验失败：" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 清理应用列表管理器资源
     * 确保在Activity销毁时正确关闭线程池
     */
    private void cleanupAppListManager() {
        try {
            Log.d(TAG, "开始清理应用列表管理器资源...");
            
            // 清理应用列表缓存
            if (appListManager != null) {
                appListManager.clearCache();
                appListManager = null;
                Log.d(TAG, "应用列表缓存已清理");
            }
            
            // 安全关闭线程池
            if (appListExecutor != null && !appListExecutor.isShutdown()) {
                Log.d(TAG, "正在关闭应用列表线程池...");
                appListExecutor.shutdown();
                
                try {
                    // 等待最多1秒让任务完成
                    if (!appListExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                        Log.w(TAG, "线程池任务未在1秒内完成，强制关闭");
                        appListExecutor.shutdownNow();
                        
                        // 再次等待强制关闭完成
                        if (!appListExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                            Log.e(TAG, "线程池强制关闭失败");
                        } else {
                            Log.d(TAG, "线程池强制关闭成功");
                        }
                    } else {
                        Log.d(TAG, "线程池已安全关闭");
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, "等待线程池关闭时被中断", e);
                    Thread.currentThread().interrupt();
                    appListExecutor.shutdownNow();
                }
                
                appListExecutor = null;
            }
            
            Log.d(TAG, "应用列表管理器资源清理完成");
            
        } catch (Exception e) {
            Log.e(TAG, "清理应用列表管理器资源失败", e);
            Log.e(TAG, "错误详情: " + e.getMessage());
            // 即使清理失败也不影响其他功能
        }
    }
    
    /**
     * 测试应用列表管理器功能
     * 用于验证应用列表获取是否正常工作
     */
    private void testAppListManager() {
        if (appListManager == null) {
            Log.w(TAG, "应用列表管理器未初始化，无法测试");
            return;
        }
        
        Log.d(TAG, "=== 开始测试应用列表管理器 ===");
        Log.d(TAG, "应用列表管理器状态: " + (appListManager.isInitialized() ? "已初始化" : "未初始化"));
        Log.d(TAG, "缓存中的应用总数: " + appListManager.getCachedAppCount());
        
        // 测试一些常见应用
        String[] testApps = {"微信", "QQ", "支付宝", "高德地图", "百度地图"};
        
        for (String appName : testApps) {
            boolean isInstalled = appListManager.isAppInstalled(appName);
            String packageName = appListManager.getPackageName(appName);
            
            Log.d(TAG, "测试应用: " + appName + 
                      " | 已安装: " + (isInstalled ? "是" : "否") + 
                      " | 包名: " + (packageName != null ? packageName : "未找到"));
        }
        
        Log.d(TAG, "=== 应用列表管理器测试完成 ===");
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