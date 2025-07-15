package com.swj.shiwujie.blind;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.navigation.NavigationHelper;
import com.swj.shiwujie.common.utils.BackButtonHelper;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BlindVO;

public class LoginActivity extends AppCompatActivity {
    private EditText etPhone;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnQuickLogin;
    private ImageButton btnBack;
    private ApiService apiService;

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

                NavigationHelper.toBlindHome(LoginActivity.this);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
} 