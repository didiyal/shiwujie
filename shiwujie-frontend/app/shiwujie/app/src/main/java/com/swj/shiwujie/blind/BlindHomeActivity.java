package com.swj.shiwujie.blind;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.swj.shiwujie.R;
import com.swj.shiwujie.databinding.ActivityBlindHomeBinding;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import android.app.AlertDialog;
import android.content.Intent;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.data.model.BlindVO;

public class BlindHomeActivity extends AppCompatActivity {
    private static final String TAG = "BlindHomeActivity";
    private ActivityBlindHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlindHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
        
        // 检查登录状态并建立WebSocket连接
        initWebSocketConnection();
    }
    
    private void initWebSocketConnection() {
        try {
            if (SharedPrefsUtil.isLoggedIn()) {
                String phone = SharedPrefsUtil.getPhone();
                boolean isVolunteer = !SharedPrefsUtil.isBlind(); // 盲人用户
                
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

    @Override
    protected void onResume() {
        super.onResume();
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();
        if (token == null || userId == null) return;

        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        apiService.getBlindById("Bearer " + token, userId).enqueue(new ApiCallback<BlindVO>(this) {
            @Override
            public void onSuccess(BlindVO data) {
                boolean isDisabilityCard = data.getIsDisabilityCard() != null && data.getIsDisabilityCard();
                SharedPrefsUtil.setBoolean("isDisabilityCard", isDisabilityCard);
                if (!isDisabilityCard) {
                    new AlertDialog.Builder(BlindHomeActivity.this)
                        .setTitle("残疾证校验提醒")
                        .setMessage("您还未完成残疾证校验，请先补充残疾证信息")
                        .setCancelable(false)
                        .setPositiveButton("去校验", (dialog, which) -> {
                            startActivity(new Intent(BlindHomeActivity.this, EditProfileActivity.class));
                        })
                        .setNegativeButton("退出APP", (dialog, which) -> {
                            finishAffinity();
                        })
                        .show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 