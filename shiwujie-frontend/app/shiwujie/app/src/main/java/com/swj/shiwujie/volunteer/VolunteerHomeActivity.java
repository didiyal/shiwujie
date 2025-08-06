package com.swj.shiwujie.volunteer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
                            showIdCardInputDialog();
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

    private void showIdCardInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_id_card_verification, null);
        builder.setView(dialogView);

        EditText etIdCard = dialogView.findViewById(R.id.etIdCard);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // 不允许点击外部取消，与原有逻辑保持一致

        btnCancel.setOnClickListener(v -> {
            // 取消认证，退出APP
            com.swj.shiwujie.common.network.ApiService apiService = com.swj.shiwujie.common.network.RetrofitClient.getInstance().createService(com.swj.shiwujie.common.network.ApiService.class);
            String token = com.swj.shiwujie.common.utils.SharedPrefsUtil.getToken();
            apiService.volunteerLogout("Bearer " + token).enqueue(new com.swj.shiwujie.common.network.ApiCallback<Boolean>(this) {
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
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            String idCard = etIdCard.getText().toString().trim();

            // 验证身份证号
            if (TextUtils.isEmpty(idCard)) {
                Toast.makeText(this, "身份证号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!idCard.matches("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$")) {
                Toast.makeText(this, "身份证号格式不正确", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用实名认证API
            performIdCardVerification(idCard, dialog);
        });

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
                dialog.dismiss();
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
        binding = null;
    }
} 