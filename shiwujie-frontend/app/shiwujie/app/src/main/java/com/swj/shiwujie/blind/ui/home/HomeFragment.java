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
import com.swj.shiwujie.common.network.EmergencyHelpManager;
import com.swj.shiwujie.common.ui.EmergencyHelpFloatingWindow;
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
    private WebSocketManager.MessageListener messageListener;
    private boolean isMatching = false; // 防止重复请求
    private static boolean isVideoCallStarted = false; // 防止重复启动视频通话
    private boolean isEmergencyHelpMatching = false; // 标记是否为紧急求助匹配
    public static void resetVideoCallStartedFlag() {
        isVideoCallStarted = false;
    }
    
    // 紧急求助相关
    private EmergencyHelpManager emergencyHelpManager;
    private EmergencyHelpFloatingWindow emergencyHelpFloatingWindow;
    // 已去除 isEmergencyHelpRequested，交由后端判断重复请求

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
            startEmergencyHelp();
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
        
        // 初始化紧急求助管理器
        initEmergencyHelp();
    }
    

    
    private void startVideoHelpMatching() {
        Log.d(TAG, "开始连线志愿者");
        
        // 防止重复请求
        // if (isMatching) {
        //   Toast.makeText(requireContext(), "正在匹配中，请稍候...", Toast.LENGTH_SHORT).show();
        //   return;
        // }
        
        // 重置视频通话启动标志，允许新的视频通话
        isVideoCallStarted = false;
        Log.d(TAG, "重置视频通话启动标志，允许新的视频通话");
        
        // 获取并打印token信息
        String token = SharedPrefsUtil.getToken();
        Log.d(TAG, "当前token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
        
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token为空，无法进行连线");
            Toast.makeText(requireContext(), "登录状态异常，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 设置匹配状态到WebSocketManager
        webSocketManager.setMatchingStatus(true);
        
        // 先检查登录状态
        checkLoginStatusBeforeMatching(token);
        
        // 设置匹配状态
        // isMatching = true;
    }
    
    private void checkLoginStatusBeforeMatching(String token) {
        Log.d(TAG, "检查登录状态");
        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        String authToken = "Bearer " + token;
        
        apiService.checkLogin(authToken).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                Log.d(TAG, "登录状态检查响应 - HTTP状态码: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Void> result = response.body();
                    Log.d(TAG, "登录状态检查结果: code=" + result.getCode() + ", message=" + result.getMessage());
                    
                    if (result.getCode() == 1) {
                        // 登录状态有效，继续匹配
                        Log.d(TAG, "登录状态有效，开始匹配");
                        startVideoHelpMatchingRequest(token);
                    } else {
                        Log.e(TAG, "登录状态无效: " + result.getMessage());
                        Toast.makeText(requireContext(), "登录状态异常: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                        isMatching = false;
                    }
                } else {
                    Log.e(TAG, "登录状态检查失败");
                    Toast.makeText(requireContext(), "登录状态检查失败", Toast.LENGTH_SHORT).show();
                    isMatching = false;
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                Log.e(TAG, "登录状态检查网络失败", t);
                Toast.makeText(requireContext(), "登录状态检查失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                isMatching = false;
            }
        });
    }
    
    private void startVideoHelpMatchingRequest(String token) {
        Log.d(TAG, "开始发送匹配请求");
        
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
                            String msg = "连线失败: " + result.getMessage();
                            if (result.getCode() == 40000 && "请求参数错误".equals(result.getMessage())) {
                                msg = "当前还没有志愿者等待，请稍后再试";
                            }
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                            // 重置匹配状态
                            isMatching = false;
                            webSocketManager.setMatchingStatus(false);
                        }
                    } else {
                        Log.e(TAG, "响应体为空");
                        Toast.makeText(requireContext(), "服务器响应异常", Toast.LENGTH_SHORT).show();
                        // 重置匹配状态
                        isMatching = false;
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
                    // 重置匹配状态
                    isMatching = false;
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
                // 重置匹配状态
                isMatching = false;
                webSocketManager.setMatchingStatus(false);
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
            
            // 防止重复启动视频通话
            if (isVideoCallStarted) {
                Log.w(TAG, "视频通话已启动，忽略重复的type=2消息");
                return;
            }
            
            // 重置匹配状态
            isMatching = false;
            webSocketManager.setMatchingStatus(false);
            isVideoCallStarted = true;
            
            // 如果是紧急求助，隐藏悬浮窗
            if (emergencyHelpFloatingWindow != null) {
                emergencyHelpFloatingWindow.hide();
            }
            
            try {
                // 跳转到视频通话页面
                Intent videoIntent = new Intent(requireContext(), com.swj.shiwujie.blind.VideoCallActivity.class);
                videoIntent.putExtra("channelId", data.getChannelId());
                videoIntent.putExtra("volunteerPhone", data.getVolunteerPhone());
                if (isEmergencyHelpMatching) {
                    videoIntent.putExtra("isEmergencyHelp", true);
                    isEmergencyHelpMatching = false;
                }
                // 添加标志，确保只有一个实例
                videoIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                videoIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(videoIntent);
                Log.d(TAG, "已启动视频通话Activity");
            } catch (Exception e) {
                Log.e(TAG, "启动视频通话Activity失败", e);
                isVideoCallStarted = false; // 启动失败时重置标志
            }
        } else if (data.getRequestType() == 5) {
            // 通话结束，重置视频通话启动标志
            Log.d(TAG, "收到通话结束(type=5)消息，重置isVideoCallStarted");
            isVideoCallStarted = false;
            // 重置紧急求助状态
            isEmergencyHelpMatching = false;
        } else if (data.getRequestType() == 0) {
            // 收到登录确认消息
            Log.d(TAG, "收到登录确认消息");
        } else {
            Log.d(TAG, "收到其他类型消息，类型: " + data.getRequestType());
        }
    }
    

    
    /**
     * 初始化紧急求助
     */
    private void initEmergencyHelp() {
        emergencyHelpManager = EmergencyHelpManager.getInstance();
        emergencyHelpManager.setContext(requireContext());
        
        // 使用Activity的上下文创建悬浮窗
        if (getActivity() != null) {
            emergencyHelpFloatingWindow = new EmergencyHelpFloatingWindow(getActivity());
        }
        
        // 设置紧急求助回调
        emergencyHelpManager.setCallback(new EmergencyHelpManager.EmergencyHelpCallback() {
            @Override
            public void onHelpRequestSuccess() {
                Log.d(TAG, "紧急求助请求成功");
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "紧急求助请求已发送", Toast.LENGTH_SHORT).show();
                        
                        // 设置紧急求助状态为匹配中
                        isEmergencyHelpMatching = true;
                        
                        // 强制重新创建悬浮窗，确保状态干净
                        if (emergencyHelpFloatingWindow != null) {
                            emergencyHelpFloatingWindow.destroy();
                        }
                        if (getActivity() != null) {
                            emergencyHelpFloatingWindow = new EmergencyHelpFloatingWindow(getActivity());
                        }
                        
                        // 强制显示悬浮窗
                        Log.d(TAG, "强制显示悬浮窗");
                        if (emergencyHelpFloatingWindow != null) {
                            emergencyHelpFloatingWindow.show();
                        }
                    });
                } else {
                    Log.w(TAG, "Fragment未attached，无法显示悬浮窗");
                }
            }
            
            @Override
            public void onHelpRequestFailed(String error) {
                Log.e(TAG, "紧急求助请求失败: " + error);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "紧急求助请求失败: " + error, Toast.LENGTH_SHORT).show();
                        // 重置状态，确保下次可以正常发起求助
                        isEmergencyHelpMatching = false;
                        // 确保EmergencyHelpManager状态也重置
                        emergencyHelpManager.resetEmergencyHelp();
                    });
                }
            }
            
            @Override
            public void onHelpCancelled() {
                Log.d(TAG, "紧急求助已取消");
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "紧急求助已取消", Toast.LENGTH_SHORT).show();
                        // 强制隐藏悬浮窗
                        if (emergencyHelpFloatingWindow != null) {
                            emergencyHelpFloatingWindow.hide();
                        }
                        // 重置状态，确保下次可以正常发起求助
                        isEmergencyHelpMatching = false;
                        // 确保EmergencyHelpManager状态也重置
                        emergencyHelpManager.resetEmergencyHelp();
                    });
                }
            }
            
            @Override
            public void onHelpResponseSuccess() {
                // 盲人端不会收到这个回调
            }
            
            @Override
            public void onHelpResponseFailed(String error) {
                // 盲人端不会收到这个回调
            }
            
            @Override
            public void onHelpHangupSuccess() {
                Log.d(TAG, "紧急求助通话已结束");
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "通话已结束", Toast.LENGTH_SHORT).show();
                        // 重置状态，允许重新发起求助
                        isEmergencyHelpMatching = false;
                        // 确保EmergencyHelpManager状态也重置
                        emergencyHelpManager.resetEmergencyHelp();
                        // 隐藏悬浮窗
                        if (emergencyHelpFloatingWindow != null) {
                            emergencyHelpFloatingWindow.hide();
                        }
                    });
                }
            }
            
            @Override
            public void onHelpHangupFailed(String error) {
                Log.e(TAG, "紧急求助挂断失败: " + error);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "挂断失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    /**
     * 开始紧急求助
     */
    private void startEmergencyHelp() {
        Log.d(TAG, "开始紧急求助");
        
        // 检查是否已经在紧急求助中，防止重复点击
        if (emergencyHelpManager.isInEmergencyHelp()) {
            Log.d(TAG, "已在紧急求助中，忽略重复请求");
            Toast.makeText(requireContext(), "已在紧急求助中，请等待响应", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查登录状态
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token为空，无法发起紧急求助");
            Toast.makeText(requireContext(), "登录状态异常，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取用户手机号
        String phone = SharedPrefsUtil.getPhone();
        if (phone == null || phone.isEmpty()) {
            Log.e(TAG, "手机号为空，无法发起紧急求助");
            Toast.makeText(requireContext(), "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 重置紧急求助状态，确保可以重新发起
        emergencyHelpManager.resetEmergencyHelp();
        isEmergencyHelpMatching = false;
        
        // 强制销毁并重新创建悬浮窗对象，确保状态干净
        if (emergencyHelpFloatingWindow != null) {
            emergencyHelpFloatingWindow.destroy();
            emergencyHelpFloatingWindow = null;
        }
        if (getActivity() != null) {
            emergencyHelpFloatingWindow = new EmergencyHelpFloatingWindow(getActivity());
        }
        
        Log.d(TAG, "发起紧急求助，手机号: " + phone);
        // 发起紧急求助请求
        emergencyHelpManager.requestEmergencyHelp(phone);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // 检查是否有从AI页面传递过来的参数
        if (getArguments() != null) {
            Bundle args = getArguments();
            
            // 检查是否来自AI页面的连线志愿者请求
            if (args.getBoolean("from_ai_volunteer_connection", false)) {
                Log.d(TAG, "收到来自AI页面的连线志愿者请求，自动启动连线功能");
                // 清除参数，避免重复处理
                args.remove("from_ai_volunteer_connection");
                // 延迟启动连线功能，确保页面完全加载
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded() && !isVideoCallStarted) {
                        startVideoHelpMatching();
                    }
                }, 500);
            }
            
            // 检查是否来自AI页面的紧急求助请求
            if (args.getBoolean("from_ai_emergency_help", false)) {
                Log.d(TAG, "收到来自AI页面的紧急求助请求，自动启动紧急求助功能");
                // 清除参数，避免重复处理
                args.remove("from_ai_emergency_help");
                // 延迟启动紧急求助功能，确保页面完全加载
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded() && !emergencyHelpManager.isInEmergencyHelp()) {
                        startEmergencyHelp();
                    }
                }, 500);
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 移除Socket消息监听
        if (webSocketManager != null && messageListener != null) {
            webSocketManager.removeMessageListener(messageListener);
        }
        
        // 销毁紧急求助悬浮窗
        if (emergencyHelpFloatingWindow != null) {
            emergencyHelpFloatingWindow.destroy();
            emergencyHelpFloatingWindow = null;
        }
        
        // 重置紧急求助状态
        if (emergencyHelpManager != null) {
            emergencyHelpManager.resetEmergencyHelp();
        }
        
        // 重置本地状态
        isEmergencyHelpMatching = false;
    }
} 