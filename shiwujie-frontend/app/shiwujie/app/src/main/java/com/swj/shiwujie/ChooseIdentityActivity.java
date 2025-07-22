package com.swj.shiwujie;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.swj.shiwujie.common.navigation.NavigationHelper;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BaseResponse;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.GET;

public class ChooseIdentityActivity extends AppCompatActivity {
    private static final String TAG = "ChooseIdentityActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Button btnBlind;
    private Button btnVolunteer;
    private ApiService apiService;
    
    // 添加静态常量用于Intent传值
    public static final String EXTRA_FROM_LOGOUT = "from_logout";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        
        // 初始化SharedPrefsUtil和ApiService
        try {
            SharedPrefsUtil.init(this);
            apiService = RetrofitClient.getInstance().createService(ApiService.class);
        } catch (Exception e) {
            Log.e(TAG, "初始化服务失败", e);
      //      Toast.makeText(this, "初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 检查是否是从退出登录跳转来的
        boolean fromLogout = getIntent().getBooleanExtra(EXTRA_FROM_LOGOUT, false);
        Log.d(TAG, "fromLogout: " + fromLogout);
        
        if (fromLogout) {
            // 如果是从退出登录来的，直接显示选择身份页面
            showIdentityChoicePage();
        } else {
            // 正常流程：检查登录状态，如果已登录且token有效，直接跳转
            String token = SharedPrefsUtil.getToken();
            Log.d(TAG, "token: " + (token != null ? "存在" : "不存在"));
            
            if (token != null && !token.isEmpty()) {
                checkLoginStatus();
            } else {
                // 没有token，直接显示选择身份页面
                showIdentityChoicePage();
            }
        }
    }

    private void showIdentityChoicePage() {
        Log.d(TAG, "显示身份选择页面");
        try {
            setContentView(R.layout.activity_choose_identity);
            initViews();
            initListeners();
            checkAndRequestPermissions();
        } catch (Exception e) {
            Log.e(TAG, "显示身份选择页面失败", e);
            Toast.makeText(this, "页面加载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkLoginStatus() {
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            showIdentityChoicePage();
            return;
        }

        // 根据用户类型调用不同的检查接口
        Call<BaseResponse<Void>> checkCall;
        if (SharedPrefsUtil.isBlind()) {
            checkCall = apiService.checkLogin("Bearer " + token);
        } else {
            checkCall = apiService.checkVolunteerLogin("Bearer " + token);
        }

        checkCall.enqueue(new ApiCallback<Void>(this) {
            @Override
            public void onSuccess(Void data) {
                // 如果token有效，根据用户类型跳转到对应的主页
                if (SharedPrefsUtil.isBlind()) {
                    NavigationHelper.toBlindHome(ChooseIdentityActivity.this);
                } else {
                    NavigationHelper.toVolunteerHome(ChooseIdentityActivity.this);
                }
                finish();
            }

            @Override
            public void onError(String message) {
                // token无效，清除本地存储的登录信息
                SharedPrefsUtil.clearAll();
                showIdentityChoicePage();
                Toast.makeText(ChooseIdentityActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        try {
            btnBlind = findViewById(R.id.btn_blind);
            btnVolunteer = findViewById(R.id.btn_volunteer);
            Log.d(TAG, "初始化视图完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化视图失败", e);
            throw e;
        }
    }

    private void initListeners() {
        try {
            btnBlind.setOnClickListener(v -> {
                Log.d(TAG, "点击盲人按钮");
                NavigationHelper.toBlindQuickLogin(this);
            });
            btnVolunteer.setOnClickListener(v -> {
                Log.d(TAG, "点击志愿者按钮");
                NavigationHelper.toVolunteerQuickLogin(this);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // 登录成功，结束当前页面
            finish();
        }
    }
} 