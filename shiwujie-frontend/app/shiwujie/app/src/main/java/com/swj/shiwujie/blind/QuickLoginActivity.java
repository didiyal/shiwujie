package com.swj.shiwujie.blind;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.navigation.NavigationHelper;
import com.swj.shiwujie.common.utils.PhoneUtils;
import com.swj.shiwujie.common.utils.PermissionManager;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BlindVO;
import com.swj.shiwujie.common.network.WebSocketManager;

public class QuickLoginActivity extends AppCompatActivity {
    private static final String TAG = "QuickLoginActivity";
    private TextView tvPhone;
    private Button btnQuickLogin;
    private Button btnPasswordLogin;
    private CheckBox cbAgreement;
    private String phoneNumber;
    private ApiService apiService;
    
    // 权限相关
    private boolean isWaitingForPermissions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "onCreate called");
            setContentView(R.layout.activity_blind_quick_login);

            initServices();
            initViews();
            initListeners();
            checkAndRequestPermissions();
        } catch (Exception e) {
            Log.e(TAG, "onCreate failed", e);
        //    Toast.makeText(this, "页面初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        try {
            tvPhone = findViewById(R.id.tv_phone);
            btnQuickLogin = findViewById(R.id.btn_quick_login);
            btnPasswordLogin = findViewById(R.id.btn_password_login);
            cbAgreement = findViewById(R.id.cb_agreement);
            Log.d(TAG, "初始化视图完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化视图失败", e);
            throw e;
        }
    }

    private void initServices() {
        try {
            apiService = RetrofitClient.getInstance().createService(ApiService.class);
            SharedPrefsUtil.init(this);
            
            Log.d(TAG, "初始化服务完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化服务失败", e);
            throw e;
        }
    }

    private void initListeners() {
        try {
            btnQuickLogin.setOnClickListener(v -> {
                Log.d(TAG, "点击一键登录按钮");
                if (!cbAgreement.isChecked()) {
                    Toast.makeText(this, "请先同意服务条款", Toast.LENGTH_SHORT).show();
                    return;
                }
                handleQuickLogin();
            });

            btnPasswordLogin.setOnClickListener(v -> {
                Log.d(TAG, "点击密码登录按钮");
                NavigationHelper.toBlindPasswordLogin(this);
            });
            Log.d(TAG, "初始化监听器完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化监听器失败", e);
            throw e;
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions = new String[]{
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_PHONE_STATE
        };

        boolean needRequest = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, PermissionManager.PERMISSION_REQUEST_CODE);
        } else {
            showPhoneNumber();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE) {
            if (isWaitingForPermissions) {
                // 处理登录后的权限请求结果
                boolean success = PermissionManager.handlePermissionResult(requestCode, permissions, grantResults);
                if (success) {
                    // 权限授予成功，进入主页
                    isWaitingForPermissions = false;
                    NavigationHelper.toBlindHome(QuickLoginActivity.this);
                } else {
                    // 核心权限被拒绝，显示提示并退出
                    PermissionManager.showPermissionDeniedDialog(this);
                }
            } else {
                // 处理手机号权限请求结果
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    showPhoneNumber();
                } else {
                    Toast.makeText(this, "需要获取手机号权限才能使用一键登录功能", Toast.LENGTH_LONG).show();
                    tvPhone.setText(R.string.phone_number_not_available);
                    btnQuickLogin.setEnabled(false);
                }
            }
        }
    }

    private void showPhoneNumber() {
        phoneNumber = PhoneUtils.getPhoneNumber(this);
        
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            // 显示带星号的手机号
            String maskedPhone = phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
            tvPhone.setText(maskedPhone);
            btnQuickLogin.setEnabled(true);
        } else {
            tvPhone.setText(R.string.phone_number_not_available);
            btnQuickLogin.setEnabled(false);
        }
    }

    private void handleQuickLogin() {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, R.string.phone_number_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.blindQuickLogin(phoneNumber).enqueue(new ApiCallback<BlindVO>(this) {
            @Override
            public void onSuccess(BlindVO data) {
                // 保存登录信息
                SharedPrefsUtil.setToken(data.getToken());
                SharedPrefsUtil.setUserType(true); // true表示盲人用户
                SharedPrefsUtil.setUserId(data.getBlindId());
                SharedPrefsUtil.setPhone(data.getPhone());

                // 建立WebSocket连接
                WebSocketManager.connectWebSocket(QuickLoginActivity.this, data.getPhone(), false);

                // 登录成功后请求权限
                isWaitingForPermissions = true;
                java.util.List<String> missing = com.swj.shiwujie.common.utils.PermissionManager.getMissingPermissions(QuickLoginActivity.this);
                if (missing.isEmpty() && com.swj.shiwujie.common.utils.PermissionManager.hasOverlayPermission(QuickLoginActivity.this)) {
                    isWaitingForPermissions = false;
                    com.swj.shiwujie.common.navigation.NavigationHelper.toBlindHome(QuickLoginActivity.this);
                } else {
                    com.swj.shiwujie.common.utils.PermissionManager.checkAndRequestLoginPermissions(QuickLoginActivity.this);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(QuickLoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionManager.OVERLAY_PERMISSION_REQUEST_CODE && isWaitingForPermissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // 悬浮窗权限已授予
                    isWaitingForPermissions = false;
                    NavigationHelper.toBlindHome(QuickLoginActivity.this);
                } else {
                    // 悬浮窗权限被拒绝，显示提示并退出
                    PermissionManager.showPermissionDeniedDialog(this);
                }
            }
        }
    }
} 