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
import com.swj.shiwujie.common.utils.PermissionManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
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
        
        // 检查权限
        checkPermissions();
        
        // 检查登录状态并建立WebSocket连接
        initWebSocketConnection();
    }
    
    private void checkPermissions() {
        // 检查视频通话所需的所有权限（包括蓝牙权限）
        if (!PermissionManager.hasVideoCallPermissions(this)) {
            PermissionManager.showPermissionRequiredDialog(this, 
                "需要摄像头、麦克风和蓝牙权限才能使用视频通话功能。请在设置中开启相关权限。");
            return;
        }
        
        // 检查悬浮窗权限
        if (!PermissionManager.hasOverlayPermission(this)) {
            PermissionManager.showPermissionRequiredDialog(this, 
                "需要悬浮窗权限才能使用完整功能。请在设置中开启悬浮窗权限。");
            return;
        }
    }
    
    private void initWebSocketConnection() {
        try {
            if (SharedPrefsUtil.isLoggedIn()) {
                String phone = SharedPrefsUtil.getPhone();
                boolean isVolunteer = !SharedPrefsUtil.isBlind(); // 盲人用户
                
                if (phone != null && !phone.isEmpty()) {
                    Log.d(TAG, "用户已登录，启动WebSocket前台服务 - 手机号: " + phone);
                    // 启动前台服务来维护WebSocket连接
                    com.swj.shiwujie.common.network.WebSocketService.startService(this);
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
        // 全局身份校验监听 - 每次都从服务器获取最新状态
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();
        if (token == null || userId == null) return;

        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        apiService.getBlindById("Bearer " + token, userId).enqueue(new ApiCallback<BlindVO>(this) {
            @Override
            public void onSuccess(BlindVO data) {
                boolean isVerified = data.getIsDisabilityCard() != null && data.getIsDisabilityCard();
                SharedPrefsUtil.setBoolean("isDisabilityCard", isVerified);
                if (!isVerified) {
                    showIdentityVerificationReminder();
                }
            }
            
            @Override
            public void onError(String message) {
                // 如果获取失败，默认显示校验提醒
                showIdentityVerificationReminder();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        
        // 停止WebSocket前台服务
        com.swj.shiwujie.common.network.WebSocketService.stopService(this);
    }


    
    private void showIdentityVerificationReminder() {
        new AlertDialog.Builder(BlindHomeActivity.this)
            .setTitle("身份校验提醒")
            .setMessage("您还未完成身份校验，请先补全残疾证证件")
            .setCancelable(false)
            .setPositiveButton("去校验", (dialog, which) -> {
                showIdentityVerificationDialog();
            })
            .setNegativeButton("退出APP", (dialog, which) -> {
                // 设置清理AI对话的标记
                setClearConversationFlag();
                // 直接退出APP，不调用logout API
                SharedPrefsUtil.clearAll();
                finishAffinity();
            })
            .show();
    }

    private void showIdentityVerificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_id_card_verification, null);
        builder.setView(dialogView);

        // 动态修改弹窗内容
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        EditText etInput = dialogView.findViewById(R.id.etIdCard);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        // 设置盲人端的内容
        tvTitle.setText("身份校验");
        tvMessage.setText("请输入您的残疾证号码进行身份校验");
        btnConfirm.setText("确认校验");
        
        // 设置残疾证输入框的位数限制（20位：18位身份证+1位类别+1位等级）
        //etInput.setMaxLength(20);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> {
            // 点击取消直接退出APP
            SharedPrefsUtil.clearAll();
            finishAffinity();
        });
        
        btnConfirm.setOnClickListener(v -> {
            String disabilityCard = etInput.getText().toString().trim();
            if (disabilityCard.isEmpty()) {
                Toast.makeText(this, "残疾证号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            performDisabilityCardVerification(disabilityCard, dialog);
        });

        dialog.show();
    }

    private void performDisabilityCardVerification(String disabilityCard, AlertDialog dialog) {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(this, "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        BlindVO blind = new BlindVO();
        blind.setBlindId(userId);
        blind.setDisabilityCard(disabilityCard);
        blind.setGender(0); // 默认设置为0（男性）

        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        apiService.updateBlindInfo(
                "Bearer " + token,
                blind
        ).enqueue(new ApiCallback<Boolean>(this) {
            @Override
            public void onSuccess(Boolean response) {
                Toast.makeText(BlindHomeActivity.this, "残疾证校验成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                // 更新本地存储的认证状态
                SharedPrefsUtil.setBoolean("isDisabilityCard", true);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(BlindHomeActivity.this, "残疾证校验失败：" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 设置清理AI对话的标记
     */
    private void setClearConversationFlag() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("ai_conversation_history", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("should_clear_conversation", true);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "设置清理对话标记失败", e);
        }
    }
} 