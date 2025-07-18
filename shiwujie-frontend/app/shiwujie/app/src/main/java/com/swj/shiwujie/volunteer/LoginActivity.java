package com.swj.shiwujie.volunteer;

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
import com.swj.shiwujie.data.model.VolunteerVO;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "VolunteerLoginActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 101;
    
    private EditText etPhone;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnQuickLogin;
    private ImageButton btnBack;
    private ApiService apiService;
    
    // 权限相关
    private boolean isWaitingForPermissions = false;
    private VolunteerVO loginData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_login);
        
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
            NavigationHelper.toVolunteerQuickLogin(this);
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
        apiService.volunteerLoginAndRegister(phone, password).enqueue(new ApiCallback<VolunteerVO>(this) {
            @Override
            public void onSuccess(VolunteerVO data) {
                // 保存登录信息
                SharedPrefsUtil.setToken(data.getToken());
                SharedPrefsUtil.setUserType(false); // false表示志愿者用户
                SharedPrefsUtil.setUserId(data.getVolunteerId());
                SharedPrefsUtil.setPhone(data.getPhone());

                // 建立WebSocket连接
                WebSocketManager.connectWebSocket(LoginActivity.this, data.getPhone(), true);

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
        // 检查音视频权限和蓝牙权限
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.BLUETOOTH_CONNECT
        };
        
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                allGranted = false;
            }
        }
        
        if (allGranted) {
            // 所有权限都已授予，直接进入主页
            NavigationHelper.toVolunteerHome(LoginActivity.this);
        } else {
            // 请求权限
            isWaitingForPermissions = true;
            
            // 先请求音视频权限
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            
            // 再请求悬浮窗权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
                }
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && isWaitingForPermissions) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "需要相关权限才能使用视频通话功能", Toast.LENGTH_LONG).show();
                finish(); // 退出APP
            } else {
                // 音视频权限已授予，检查悬浮窗权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        // 所有权限都已授予
                        isWaitingForPermissions = false;
                        NavigationHelper.toVolunteerHome(LoginActivity.this);
                    }
                    // 如果悬浮窗权限未授予，等待onActivityResult回调
                } else {
                    // 低版本Android不需要悬浮窗权限
                    isWaitingForPermissions = false;
                    NavigationHelper.toVolunteerHome(LoginActivity.this);
                }
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE && isWaitingForPermissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // 悬浮窗权限已授予
                    isWaitingForPermissions = false;
                    NavigationHelper.toVolunteerHome(LoginActivity.this);
                } else {
                    Toast.makeText(this, "需要悬浮窗权限才能使用完整功能", Toast.LENGTH_LONG).show();
                    finish(); // 退出APP
                }
            }
        }
    }
} 