package com.swj.shiwujie;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.swj.shiwujie.common.navigation.NavigationHelper;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;

public class ChooseIdentityActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Button btnBlind;
    private Button btnVolunteer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化SharedPrefsUtil
        SharedPrefsUtil.init(this);
        
        // 检查登录状态
        if (checkLoginStatus()) {
            return;
        }
        
        setContentView(R.layout.activity_choose_identity);
        initViews();
        initListeners();
        checkAndRequestPermissions();
    }

    private boolean checkLoginStatus() {
        String token = SharedPrefsUtil.getToken();
        if (token != null && !token.isEmpty()) {
            // 已登录，根据用户类型跳转到对应主页
            boolean isBlind = SharedPrefsUtil.isBlind();
            NavigationHelper.toUserHome(this, isBlind);
            return true;
        }
        return false;
    }

    private void initViews() {
        btnBlind = findViewById(R.id.btn_blind);
        btnVolunteer = findViewById(R.id.btn_volunteer);
    }

    private void initListeners() {
        btnBlind.setOnClickListener(v -> NavigationHelper.toBlindQuickLogin(this));
        btnVolunteer.setOnClickListener(v -> {
            // TODO: 实现志愿者登录页面跳转
        });
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
            if (!allGranted) {
                Toast.makeText(this, "为了更好的使用体验，建议授予手机号读取权限", Toast.LENGTH_LONG).show();
            }
        }
    }
} 