package com.swj.shiwujie.blind;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BlindVO;

public class QuickLoginActivity extends AppCompatActivity {
    private static final String TAG = "QuickLoginActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView tvPhone;
    private Button btnQuickLogin;
    private Button btnPasswordLogin;
    private CheckBox cbAgreement;
    private String phoneNumber;
    private ApiService apiService;

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
            Toast.makeText(this, "页面初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            showPhoneNumber();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
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
                
                // 设置结果并结束当前页面
                setResult(RESULT_OK);
                finish();
                
                // 跳转到主页
                NavigationHelper.toBlindHome(QuickLoginActivity.this);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(QuickLoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
} 