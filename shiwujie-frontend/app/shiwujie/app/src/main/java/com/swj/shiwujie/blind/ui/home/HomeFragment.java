package com.swj.shiwujie.blind.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.SocketDataV0;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private static final String TAG = "BlindHomeFragment";

    private CardView cardConnectVolunteer;
    private CardView cardEmergencyHelp;
    private WebSocketManager webSocketManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(root);
        initWebSocket();
        return root;
    }

    private void initViews(View root) {
        cardConnectVolunteer = root.findViewById(R.id.cardConnectVolunteer);
        cardEmergencyHelp = root.findViewById(R.id.cardEmergencyHelp);

        // 设置连线志愿者按钮点击事件
        cardConnectVolunteer.setOnClickListener(v -> {
            startVideoHelpMatching();
        });

        // 设置紧急求助按钮点击事件
        cardEmergencyHelp.setOnClickListener(v -> {
            // TODO: 实现紧急求助功能
            Toast.makeText(requireContext(), "紧急求助功能开发中...", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void initWebSocket() {
        webSocketManager = WebSocketManager.getInstance();
        
        // 添加全局Socket消息监听
        webSocketManager.addMessageListener(new WebSocketManager.MessageListener() {
            @Override
            public void onMessageReceived(SocketDataV0 data) {
                handleSocketMessage(data);
            }
        });
    }
    

    
    private void startVideoHelpMatching() {
        Log.d(TAG, "开始连线志愿者");
        
        // 获取并打印token信息
        String token = SharedPrefsUtil.getToken();
        Log.d(TAG, "当前token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
        
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token为空，无法进行连线");
            Toast.makeText(requireContext(), "登录状态异常，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 发送盲人加入视频求助请求
        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        String authToken = "Bearer " + token;
        Log.d(TAG, "发送请求 - Authorization: " + authToken.substring(0, Math.min(30, authToken.length())) + "...");
        
        apiService.blindJoinVideohelp(authToken).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                Log.d(TAG, "收到响应 - HTTP状态码: " + response.code());
                Log.d(TAG, "响应头: " + response.headers().toString());
                
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        BaseResponse<Boolean> result = response.body();
                        Log.d(TAG, "响应体: code=" + result.getCode() + ", message=" + result.getMessage() + ", data=" + result.getData());
                        
                        if (result.getCode() == 1 && Boolean.TRUE.equals(result.getData())) {
                            Log.d(TAG, "连线请求成功，等待志愿者接听");
                            Toast.makeText(requireContext(), "正在等待志愿者接听...", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "连线失败 - 业务错误: " + result.getMessage());
                            Toast.makeText(requireContext(), "连线失败: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "响应体为空");
                        Toast.makeText(requireContext(), "服务器响应异常", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "HTTP请求失败 - 状态码: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "无错误信息";
                        Log.e(TAG, "错误响应体: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "读取错误响应体失败", e);
                    }
                    Toast.makeText(requireContext(), "网络请求失败 - HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                Log.e(TAG, "网络请求失败", t);
                Log.e(TAG, "请求URL: " + call.request().url());
                Log.e(TAG, "请求方法: " + call.request().method());
                Log.e(TAG, "请求头: " + call.request().headers());
                Toast.makeText(requireContext(), "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void handleSocketMessage(SocketDataV0 data) {
        // 检查Fragment是否还attached到context
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment未attached到context，跳过消息处理");
            return;
        }
        
        Log.d(TAG, "收到Socket消息: " + data.toString());
        Log.d(TAG, "消息类型: " + data.getRequestType() + 
              ", 盲人手机号: " + data.getBlindPhone() + 
              ", 志愿者手机号: " + data.getVolunteerPhone() + 
              ", 频道ID: " + data.getChannelId());
        
        if (data.getRequestType() == 2) {
            // 视频初始化成功通知，进入视频通话页面
            Log.d(TAG, "收到视频初始化成功通知，准备进入视频通话页面");
            Log.d(TAG, "频道ID: " + data.getChannelId() + ", 志愿者手机号: " + data.getVolunteerPhone());
            
            try {
                // 跳转到视频通话页面
                Intent videoIntent = new Intent(requireContext(), com.swj.shiwujie.blind.VideoCallActivity.class);
                videoIntent.putExtra("channelId", data.getChannelId());
                videoIntent.putExtra("volunteerPhone", data.getVolunteerPhone());
                startActivity(videoIntent);
                Log.d(TAG, "已启动视频通话Activity");
            } catch (Exception e) {
                Log.e(TAG, "启动视频通话Activity失败", e);
            }
        } else if (data.getRequestType() == 0) {
            // 收到登录确认消息
            Log.d(TAG, "收到登录确认消息");
        } else {
            Log.d(TAG, "收到其他类型消息，类型: " + data.getRequestType());
        }
    }
    

    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 移除Socket消息监听
        if (webSocketManager != null) {
            webSocketManager.removeMessageListener(null);
        }
    }
} 