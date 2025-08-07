package com.swj.shiwujie;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.navigation.NavigationHelper;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;

public class SetPasswordActivity extends AppCompatActivity {
    private static final String TAG = "SetPasswordActivity";
    
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnLater;
    private Button btnConfirm;
    private ApiService apiService;
    private boolean isBlind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_set_password);
        
        // 获取用户类型
        isBlind = getIntent().getBooleanExtra("isBlind", true);
        
        initServices();
        initViews();
        initListeners();
    }

    private void initServices() {
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        SharedPrefsUtil.init(this);
    }

    private void initViews() {
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnLater = findViewById(R.id.btnLater);
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    private void initListeners() {
        btnLater.setOnClickListener(v -> {
            // 稍后添加，直接进入主页
            NavigationHelper.toUserHome(this, isBlind);
        });

        btnConfirm.setOnClickListener(v -> {
            handleSetPassword();
        });
    }

    private void handleSetPassword() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "请确认新密码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证密码格式（必须包含字符和数字）
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            Toast.makeText(this, "密码必须包含字符和数字", Toast.LENGTH_SHORT).show();
            return;
        }

        // 调用修改密码接口
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(this, "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 根据用户类型调用不同的接口
        if (isBlind) {
            // 盲人端修改密码
            apiService.updateBlindPassword(
                    "Bearer " + token,
                    userId,
                    "", // 原密码为空，因为是首次设置
                    newPassword
            ).enqueue(new ApiCallback<Boolean>(this) {
                @Override
                public void onSuccess(Boolean response) {
                    Toast.makeText(SetPasswordActivity.this, "密码设置成功", Toast.LENGTH_SHORT).show();
                    NavigationHelper.toUserHome(SetPasswordActivity.this, true);
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(SetPasswordActivity.this, "密码设置失败：" + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // 志愿者端修改密码
            apiService.updateVolunteerPassword(
                    "Bearer " + token,
                    userId,
                    "", // 原密码为空，因为是首次设置
                    newPassword
            ).enqueue(new ApiCallback<Boolean>(this) {
                @Override
                public void onSuccess(Boolean response) {
                    Toast.makeText(SetPasswordActivity.this, "密码设置成功", Toast.LENGTH_SHORT).show();
                    NavigationHelper.toUserHome(SetPasswordActivity.this, false);
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(SetPasswordActivity.this, "密码设置失败：" + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
