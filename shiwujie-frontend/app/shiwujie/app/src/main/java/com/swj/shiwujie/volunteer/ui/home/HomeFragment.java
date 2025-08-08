package com.swj.shiwujie.volunteer.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.service.FloatingWindowService;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.SocketDataV0;
import com.swj.shiwujie.common.ui.EmergencyHelpFloatingWindow;
import com.swj.shiwujie.common.ui.EmergencyHelpIncomingWindow;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private static final String TAG = "VolunteerHomeFragment";
    
    private Button btnLearnCall;
    private WebSocketManager webSocketManager;
    private WebSocketManager.MessageListener messageListener;
    private EmergencyHelpFloatingWindow emergencyHelpFloatingWindow;
    private EmergencyHelpIncomingWindow emergencyHelpIncomingWindow;
    private String currentBlindIdForHelp = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_volunteer_home, container, false);
        initViews(root);
        initWebSocket();
        return root;
    }
    
    private void initViews(View root) {
        btnLearnCall = root.findViewById(R.id.btn_learn_call);
        
        btnLearnCall.setOnClickListener(v -> {
            startVideoHelpMatching();
        });
    }
    
    private void initWebSocket() {
        webSocketManager = WebSocketManager.getInstance();
        
        // 创建消息监听器
        messageListener = new WebSocketManager.MessageListener() {
            @Override
            public void onMessageReceived(SocketDataV0 data) {
                handleSocketMessage(data);
            }
        };
        
        // 添加全局Socket消息监听
        webSocketManager.addMessageListener(messageListener);
    }
    

    
    private void startVideoHelpMatching() {
        Log.d(TAG, "开始视频求助匹配");
        
        // 设置匹配状态到WebSocketManager
        webSocketManager.setMatchingStatus(true);
        
        // 获取并打印token信息
        String token = SharedPrefsUtil.getToken();
        Log.d(TAG, "当前token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
        
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token为空，无法进行匹配");
            Toast.makeText(requireContext(), "登录状态异常，请重新登录", Toast.LENGTH_SHORT).show();
            webSocketManager.setMatchingStatus(false);
            return;
        }
        
        // 发送志愿者创建视频求助请求
        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        String authToken = "Bearer " + token;
        Log.d(TAG, "发送请求 - Authorization: " + authToken.substring(0, Math.min(30, authToken.length())) + "...");
        
        apiService.volunteerCreateVideohelp(authToken).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                Log.d(TAG, "收到响应 - HTTP状态码: " + response.code());
                Log.d(TAG, "响应头: " + response.headers().toString());
                
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        BaseResponse<Boolean> result = response.body();
                        Log.d(TAG, "响应体: code=" + result.getCode() + ", message=" + result.getMessage() + ", data=" + result.getData());
                        
                        if (result.getCode() == 1 && Boolean.TRUE.equals(result.getData())) {
                            Log.d(TAG, "匹配请求成功，开始等待匹配");
                            // 启动悬浮窗服务
                            startFloatingWindowService();
                            // 匹配成功，保持匹配状态，等待WebSocket消息
                          //  Toast.makeText(requireContext(), "已开始等待匹配", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "匹配失败 - 业务错误: " + result.getMessage());
                            Toast.makeText(requireContext(), "匹配失败: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                            webSocketManager.setMatchingStatus(false);
                        }
                    } else {
                        Log.e(TAG, "响应体为空");
                        Toast.makeText(requireContext(), "服务器响应异常", Toast.LENGTH_SHORT).show();
                        webSocketManager.setMatchingStatus(false);
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
                    webSocketManager.setMatchingStatus(false);
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                Log.e(TAG, "网络请求失败", t);
                Log.e(TAG, "请求URL: " + call.request().url());
                Log.e(TAG, "请求方法: " + call.request().method());
                Log.e(TAG, "请求头: " + call.request().headers());
                Toast.makeText(requireContext(), "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                webSocketManager.setMatchingStatus(false);
            }
        });
    }
    
    private void startFloatingWindowService() {
        Intent intent = new Intent(requireContext(), FloatingWindowService.class);
        requireContext().startService(intent);
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
        
        if (data.getRequestType() == 3) {
            // 志愿者端来电提醒弹窗
            if (emergencyHelpIncomingWindow == null) {
                emergencyHelpIncomingWindow = new EmergencyHelpIncomingWindow(requireActivity());
            }
            emergencyHelpIncomingWindow.setBlindPhone(data.getBlindPhone());
            emergencyHelpIncomingWindow.setOnAcceptListener(v -> {
                respondToEmergencyHelp(data);
                emergencyHelpIncomingWindow.hide();
            });
            emergencyHelpIncomingWindow.setOnRejectListener(v -> {
                emergencyHelpIncomingWindow.hide();
            });
            emergencyHelpIncomingWindow.show();
            currentBlindIdForHelp = data.getBlindPhone();
        } else if (data.getRequestType() == 4) {
            // 盲人取消紧急求助，家属端关闭弹窗
            Log.d(TAG, "收到紧急求助取消通知，关闭弹窗");
            if (emergencyHelpIncomingWindow != null) {
                emergencyHelpIncomingWindow.hide();
            }
        } else if (data.getRequestType() == 1) {
            // 志愿者匹配成功通知
            Log.d(TAG, "收到匹配成功通知，准备进入视频通话页面");
            Log.d(TAG, "频道ID: " + data.getChannelId() + ", 盲人手机号: " + data.getBlindPhone());
            
            // 重置匹配状态
            webSocketManager.setMatchingStatus(false);
            
            // 立即发送type=2消息给盲人端，确保盲人端能收到通知
            sendVideoInitMessage(data);
            
            try {
                // 停止悬浮窗服务
                Intent intent = new Intent(requireContext(), FloatingWindowService.class);
                requireContext().stopService(intent);
                Log.d(TAG, "已停止悬浮窗服务");
                
                // 直接跳转到视频通话页面，让视频通话页面处理初始化
                Intent videoIntent = new Intent(requireContext(), com.swj.shiwujie.volunteer.VideoCallActivity.class);
                videoIntent.putExtra("channelId", data.getChannelId());
                videoIntent.putExtra("blindPhone", data.getBlindPhone());
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
    
    private void sendVideoInitMessage(SocketDataV0 matchData) {
        try {
            Log.d(TAG, "HomeFragment准备发送视频初始化成功消息");
            Log.d(TAG, "匹配信息 - channelId: " + matchData.getChannelId() + 
                  ", blindPhone: " + matchData.getBlindPhone() + 
                  ", volunteerPhone: " + matchData.getVolunteerPhone());
            
            // 创建视频初始化成功消息
            SocketDataV0 initData = new SocketDataV0();
            initData.setRequestType(2); // 视频初始化成功通知
            initData.setBlindPhone(matchData.getBlindPhone());
            initData.setVolunteerPhone(matchData.getVolunteerPhone());
            initData.setChannelId(matchData.getChannelId());
            
            Log.d(TAG, "HomeFragment发送视频初始化成功消息");
            Log.d(TAG, "消息内容: requestType=" + initData.getRequestType() + 
                  ", blindPhone=" + initData.getBlindPhone() + 
                  ", volunteerPhone=" + initData.getVolunteerPhone() + 
                  ", channelId=" + initData.getChannelId());
            
            webSocketManager.sendMessage(initData);
            Log.d(TAG, "HomeFragment视频初始化成功消息已发送");
            
        } catch (Exception e) {
            Log.e(TAG, "HomeFragment发送视频初始化消息失败: " + e.getMessage());
        }
    }

    private void respondToEmergencyHelp(SocketDataV0 data) {
        // 调用ApiService家属响应紧急求助接口
        String blindPhone = data.getBlindPhone();
        if (blindPhone == null) {
            Toast.makeText(requireContext(), "盲人手机号为空，无法响应紧急求助", Toast.LENGTH_SHORT).show();
            return;
        }
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "登录状态异常，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }
        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        String authToken = "Bearer " + token;
        apiService.familyJoinUrgenthelp(authToken, blindPhone).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Boolean> result = response.body();
                    if (result.getCode() == 1 && Boolean.TRUE.equals(result.getData())) {
                        Toast.makeText(requireContext(), "响应成功，进入视频通话", Toast.LENGTH_SHORT).show();
                        // 关闭来电弹窗
                        if (emergencyHelpIncomingWindow != null) emergencyHelpIncomingWindow.hide();
                        // 跳转到视频通话页面，传递isEmergencyHelp=true
                        Intent videoIntent = new Intent(requireContext(), com.swj.shiwujie.volunteer.VideoCallActivity.class);
                        videoIntent.putExtra("channelId", data.getChannelId());
                        videoIntent.putExtra("blindPhone", data.getBlindPhone());
                        videoIntent.putExtra("volunteerPhone", data.getVolunteerPhone());
                        videoIntent.putExtra("isEmergencyHelp", true);
                        startActivity(videoIntent);
                    } else {
                        Toast.makeText(requireContext(), "响应失败: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "网络请求失败", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                Toast.makeText(requireContext(), "网络异常: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    

    


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 移除Socket消息监听
        if (webSocketManager != null && messageListener != null) {
            webSocketManager.removeMessageListener(messageListener);
        }
        // 销毁紧急求助来电弹窗
        if (emergencyHelpIncomingWindow != null) {
            emergencyHelpIncomingWindow.destroy();
            emergencyHelpIncomingWindow = null;
        }
        // 销毁紧急求助悬浮窗（盲人端专用）
        if (emergencyHelpFloatingWindow != null) {
            emergencyHelpFloatingWindow.destroy();
            emergencyHelpFloatingWindow = null;
        }
    }
} 