package com.swj.shiwujie.blind;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.navigation.NavigationHelper;
import com.swj.shiwujie.common.utils.BackButtonHelper;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.utils.PermissionManager;
import com.swj.shiwujie.data.model.BlindVO;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "BlindLoginActivity";

    
    private EditText etPhone;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnQuickLogin;
    private ImageButton btnBack;
    private ApiService apiService;
    
    // 权限相关
    private boolean isWaitingForPermissions = false;
    private BlindVO loginData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initServices();
        initViews();
        initListeners();
    }

    private void initServices() {
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        SharedPrefsUtil.init(this);
    }

    private void initViews() {
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnQuickLogin = findViewById(R.id.btnQuickLogin);
        
        // 添加返回按钮
        ConstraintLayout container = findViewById(R.id.container);
        btnBack = BackButtonHelper.addBackButton(this, container);
    }

    private void initListeners() {
        btnQuickLogin.setOnClickListener(v -> {
            NavigationHelper.toBlindQuickLogin(this);
        });

        btnLogin.setOnClickListener(v -> handleLogin());

        // 修改返回按钮点击事件
        btnBack.setOnClickListener(v -> NavigationHelper.backToChooseIdentity(this));
    }

    private void handleLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 调用登录接口
        apiService.loginAndRegister(phone, password).enqueue(new ApiCallback<BlindVO>(this) {
            @Override
            public void onSuccess(BlindVO data) {
                // 保存登录信息
                SharedPrefsUtil.setToken(data.getToken());
                SharedPrefsUtil.setUserType(true); // true表示盲人用户
                SharedPrefsUtil.setUserId(data.getBlindId());
                SharedPrefsUtil.setPhone(data.getPhone());

                // 建立WebSocket连接
                WebSocketManager.connectWebSocket(LoginActivity.this, data.getPhone(), false);

                // 登录成功后请求权限
                loginData = data;
                requestAllPermissions();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 请求所有必要权限
     */
    private void requestAllPermissions() {
        isWaitingForPermissions = true;
        java.util.List<String> missing = com.swj.shiwujie.common.utils.PermissionManager.getMissingPermissions(this);
        if (missing.isEmpty() && com.swj.shiwujie.common.utils.PermissionManager.hasOverlayPermission(this)) {
            isWaitingForPermissions = false;
            com.swj.shiwujie.common.navigation.NavigationHelper.toBlindHome(LoginActivity.this);
        } else {
            com.swj.shiwujie.common.utils.PermissionManager.checkAndRequestLoginPermissions(this);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE && isWaitingForPermissions) {
            boolean success = PermissionManager.handlePermissionResult(requestCode, permissions, grantResults);
            if (success) {
                // 权限授予成功，进入主页
                isWaitingForPermissions = false;
                NavigationHelper.toBlindHome(LoginActivity.this);
            } else {
                // 核心权限被拒绝，显示提示并退出
                PermissionManager.showPermissionDeniedDialog(this);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionManager.OVERLAY_PERMISSION_REQUEST_CODE && isWaitingForPermissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // 悬浮窗权限已授予
                    isWaitingForPermissions = false;
                    NavigationHelper.toBlindHome(LoginActivity.this);
                } else {
                    // 悬浮窗权限被拒绝，显示提示并退出
                    PermissionManager.showPermissionDeniedDialog(this);
                }
            }
        }
    }
} 