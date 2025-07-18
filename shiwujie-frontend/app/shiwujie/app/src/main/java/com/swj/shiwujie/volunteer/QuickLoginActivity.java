package com.swj.shiwujie.volunteer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Toast;

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
import com.swj.shiwujie.data.model.VolunteerVO;
import com.swj.shiwujie.common.network.WebSocketManager;

import java.util.List;
import java.util.stream.Collectors;

public class QuickLoginActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView tvPhone;
    private Button btnQuickLogin;
    private Button btnPasswordLogin;
    private ImageButton btnBack;
    private CheckBox cbAgreement;
    private String phoneNumber;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_quick_login);

        initViews();
        initServices();
        initListeners();
        checkAndRequestPermissions();
    }

    private void initViews() {
        tvPhone = findViewById(R.id.tv_phone);
        btnQuickLogin = findViewById(R.id.btn_quick_login);
        btnPasswordLogin = findViewById(R.id.btn_password_login);
        btnBack = findViewById(R.id.btn_back);
        cbAgreement = findViewById(R.id.cb_agreement);
    }

    private void initServices() {
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        SharedPrefsUtil.init(this);
    }

    private void initListeners() {
        btnBack.setOnClickListener(v -> NavigationHelper.backToChooseIdentity(this));
        
        btnQuickLogin.setOnClickListener(v -> {
            if (!cbAgreement.isChecked()) {
                Toast.makeText(this, "请先同意服务条款", Toast.LENGTH_SHORT).show();
                return;
            }
            handleQuickLogin();
        });

        btnPasswordLogin.setOnClickListener(v -> {
            NavigationHelper.toVolunteerPasswordLogin(this);
        });
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_NUMBERS},
                    PERMISSION_REQUEST_CODE);
        } else {
            showPhoneNumber();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

        // 调试用：显示获取到的手机号
        Toast.makeText(this, "手机号: " + phoneNumber, Toast.LENGTH_LONG).show();

        apiService.volunteerQuickLogin(phoneNumber).enqueue(new ApiCallback<VolunteerVO>(this) {
            @Override
            public void onSuccess(VolunteerVO data) {
                // 保存登录信息
                SharedPrefsUtil.setToken(data.getToken());
                SharedPrefsUtil.setUserType(false); // false表示志愿者用户
                SharedPrefsUtil.setUserId(data.getVolunteerId());
                SharedPrefsUtil.setPhone(data.getPhone());
                
                // 建立WebSocket连接
                WebSocketManager.connectWebSocket(QuickLoginActivity.this, data.getPhone(), true);
                
                // 跳转到主页
                NavigationHelper.toVolunteerHome(QuickLoginActivity.this);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(QuickLoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
} 