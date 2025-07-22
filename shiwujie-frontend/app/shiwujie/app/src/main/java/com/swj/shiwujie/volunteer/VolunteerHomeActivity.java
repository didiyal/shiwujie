package com.swj.shiwujie.volunteer;

import android.os.Bundle;
import android.util.Log;

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

public class VolunteerHomeActivity extends AppCompatActivity {
    private static final String TAG = "VolunteerHomeActivity";
    private ActivityVolunteerHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVolunteerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViews();
        
        // 检查权限
        checkPermissions();
        
        // 检查登录状态并建立WebSocket连接
        initWebSocketConnection();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();
        if (token == null || userId == null) return;

        com.swj.shiwujie.common.network.ApiService apiService = com.swj.shiwujie.common.network.RetrofitClient.getInstance().createService(com.swj.shiwujie.common.network.ApiService.class);
        apiService.getVolunteerVOById("Bearer " + token, userId).enqueue(new com.swj.shiwujie.common.network.ApiCallback<com.swj.shiwujie.data.model.VolunteerVO>(this) {
            @Override
            public void onSuccess(com.swj.shiwujie.data.model.VolunteerVO data) {
                boolean isIdCard = data.getIsIdCard() != null && data.getIsIdCard();
                com.swj.shiwujie.common.utils.SharedPrefsUtil.setBoolean("isIdCard", isIdCard);
                if (!isIdCard) {
                    new android.app.AlertDialog.Builder(VolunteerHomeActivity.this)
                        .setTitle("实名认证提醒")
                        .setMessage("您还未完成实名认证，请先进行实名校验")
                        .setCancelable(false)
                        .setPositiveButton("去实名", (dialog, which) -> {
                            startActivity(new android.content.Intent(VolunteerHomeActivity.this, com.swj.shiwujie.volunteer.EditProfileActivity.class));
                        })
                        .setNegativeButton("退出APP", (dialog, which) -> {
                            com.swj.shiwujie.common.network.ApiService apiService = com.swj.shiwujie.common.network.RetrofitClient.getInstance().createService(com.swj.shiwujie.common.network.ApiService.class);
                            String token = com.swj.shiwujie.common.utils.SharedPrefsUtil.getToken();
                            apiService.volunteerLogout("Bearer " + token).enqueue(new com.swj.shiwujie.common.network.ApiCallback<Boolean>(VolunteerHomeActivity.this) {
                                @Override
                                public void onSuccess(Boolean data) {
                                    com.swj.shiwujie.common.utils.SharedPrefsUtil.clearAll();
                                    finishAffinity();
                                }
                                @Override
                                public void onError(String message) {
                                    com.swj.shiwujie.common.utils.SharedPrefsUtil.clearAll();
                                    finishAffinity();
                                }
                            });
                        })
                        .show();
                }
            }
        });
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
                    // 检查WebSocket连接状态，避免重复连接
                    WebSocketManager webSocketManager = WebSocketManager.getInstance();
                    if (!webSocketManager.isConnected()) {
                        Log.d(TAG, "用户已登录，建立WebSocket连接 - 手机号: " + phone);
                        WebSocketManager.connectWebSocket(this, phone, isVolunteer);
                    } else {
                        Log.d(TAG, "WebSocket已连接，跳过重复连接");
                    }
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
        // 配置底部导航栏
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_community,
                R.id.navigation_message, R.id.navigation_family,
                R.id.navigation_profile)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 