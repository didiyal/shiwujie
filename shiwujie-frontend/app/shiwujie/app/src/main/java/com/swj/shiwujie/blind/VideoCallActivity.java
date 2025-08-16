package com.swj.shiwujie.blind;

import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.VideoCallManager;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.network.EmergencyHelpManager;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.common.utils.PermissionManager;
import com.swj.shiwujie.data.model.SocketDataV0;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.data.model.BaseResponse;

import org.ar.rtc.Constants;
import org.ar.rtc.IRtcEngineEventHandler;
import org.ar.rtc.RtcEngine;
import org.ar.rtc.video.VideoCanvas;
import org.webrtc.TextureViewRenderer;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.Intent;
import android.widget.ImageButton;
import org.ar.rtc.Constants;

/**
 * 盲人端视频通话Activity
 * 集成AnyRTC SDK实现视频通话功能
 */
public class VideoCallActivity extends AppCompatActivity {
    private static final String TAG = "VideoCallActivity";
    
    // 静态标志，防止多个实例同时存在
    private static boolean isActivityRunning = false;


    // UI组件
    private FrameLayout localVideoContainer;
    private FrameLayout remoteVideoContainer;
    private ImageButton btnHangup;
    private ImageButton btnMute;
    private ImageButton btnSwitchCamera;
    private ImageButton btnSpeaker;
    private TextView tvCallStatus;
    private TextView tvCallDuration;
    
    // RTC相关
    private RtcEngine mRtcEngine;
    private HashMap<String, TextureViewRenderer> renderers = new HashMap<>();
    
    // 管理器
    private WebSocketManager webSocketManager;
    private VideoCallManager videoCallManager;
    
    // 状态标志
    private boolean isMuted = false;
    private boolean isFrontCamera = true;
    private boolean isSpeakerOn = true; // 默认开启扬声器
    private long callStartTime = 0;
    private android.os.Handler callDurationHandler;
    private Runnable callDurationRunnable;
    private String remoteUserId = "";
    
    // 匹配信息
    private long matchChannelId = 0;
    private String matchVolunteerPhone = "";
    private boolean isInitialized = false; // 防止重复初始化

    private boolean isVideoInit = false;

    private boolean hasHandledMatchSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查是否已有Activity在运行
        if (isActivityRunning) {
            Log.w(TAG, "VideoCallActivity已在运行，结束当前实例");
            finish();
            return;
        }
        
        // 防止重复初始化
        if (isInitialized) {
            Log.w(TAG, "VideoCallActivity已初始化，跳过重复初始化");
            return;
        }
        
        // 最后检查权限，确保AnyRTC SDK能正常初始化
        if (!PermissionManager.hasVideoCallPermissions(this)) {
            Log.e(TAG, "视频通话权限不足，退出Activity");
            finish();
            return;
        }
        
        setContentView(R.layout.activity_video_call);
        
        // 从Intent中获取匹配信息
        Intent intent = getIntent();
        if (intent != null) {
            matchChannelId = intent.getLongExtra("channelId", 0);
            matchVolunteerPhone = intent.getStringExtra("volunteerPhone");
            
            Log.d(TAG, "从Intent获取匹配信息 - channelId: " + matchChannelId + 
                  ", volunteerPhone: " + matchVolunteerPhone);
        }
        
        initViews();
        initManagers();
        initEngineAndJoinChannel();
        
        // 如果从Intent获取到了频道信息，立即加入频道
        if (matchChannelId > 0) {
            Log.d(TAG, "从Intent获取到频道信息，立即加入频道");
            // 延迟一点时间，确保RtcEngine初始化完成
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                String channelId = String.valueOf(matchChannelId);
                joinVideoChannel(channelId);
            }, 1000);
        }
        
        isInitialized = true;
        isActivityRunning = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isVideoInit) {
            isVideoInit = true;
            initViews();
            initManagers();
        }
    }
    
    private void initViews() {
        localVideoContainer = findViewById(R.id.local_video_container);
        remoteVideoContainer = findViewById(R.id.remote_video_container);
        btnHangup = findViewById(R.id.btn_hangup);
        btnMute = findViewById(R.id.btn_mute);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        btnSpeaker = findViewById(R.id.btn_speaker);
        tvCallStatus = findViewById(R.id.tv_call_status);
        tvCallDuration = findViewById(R.id.tv_call_duration);
        
        // 设置按钮点击事件
        btnHangup.setOnClickListener(v -> hangupCall());
        btnMute.setOnClickListener(v -> toggleMute());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());
        btnSpeaker.setOnClickListener(v -> toggleSpeaker());
        
        // 初始化计时器
        callDurationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        callDurationRunnable = new Runnable() {
            @Override
            public void run() {
                updateCallDuration();
                callDurationHandler.postDelayed(this, 1000);
            }
        };
    }
    
    private void initManagers() {
        webSocketManager = WebSocketManager.getInstance();
        videoCallManager = webSocketManager.getVideoCallManager();
        
        // 添加全局Socket消息监听
        webSocketManager.addMessageListener(new WebSocketManager.MessageListener() {
            @Override
            public void onMessageReceived(SocketDataV0 data) {
                runOnUiThread(() -> handleWebSocketMessage(data));
            }
        });
        
        // 添加状态监听器
        videoCallManager.addStatusListener(new VideoCallManager.VideoCallStatusListener() {
            @Override
            public void onCallStatusChanged(int status, String callId, String blindPhone, String volunteerPhone) {
                runOnUiThread(() -> updateCallStatus(status, callId, blindPhone, volunteerPhone));
            }
        });
    }
    

    
    private void initEngineAndJoinChannel() {
        try {
            initializeEngine();
            setupLocalVideo();
            // 等待WebSocket消息触发加入频道
        } catch (Exception e) {
            Log.e(TAG, "初始化引擎失败: " + e.getMessage());
            finish();
        }
    }
    
    private void initializeEngine() {
        try {
            String appId = "0e1f64f46fe60c00cfa143e535cbd22f";
            mRtcEngine = RtcEngine.create(getBaseContext(), appId, mRtcEventHandler);
            
            // 启用音频和视频
            mRtcEngine.enableAudio();
            mRtcEngine.enableVideo();
            
            // 设置音频参数
            mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT, Constants.AUDIO_SCENARIO_DEFAULT);
            mRtcEngine.setDefaultAudioRoutetoSpeakerphone(true); // 默认使用扬声器
            
            // 启用本地预览
            mRtcEngine.startPreview();
            
            // 设置音频初始状态
            mRtcEngine.adjustRecordingSignalVolume(100);
            mRtcEngine.adjustPlaybackSignalVolume(100);
            mRtcEngine.muteLocalAudioStream(false);
            
            // 设置视频参数
            mRtcEngine.setParameters("{\"che.video.lowBitRateStreamParameter\":{\"width\":320,\"height\":180,\"frameRate\":15,\"bitRate\":140}}");
            
            Log.d(TAG, "RTC引擎初始化成功");
            
        } catch (Exception e) {
            Log.e(TAG, "RTC引擎初始化失败: " + e.getMessage(), e);
        //    Toast.makeText(this, "视频引擎初始化失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupLocalVideo() {
        try {
            // 使用RtcEngine.CreateRendererView创建视图
            TextureView localView = RtcEngine.CreateRendererView(getBaseContext());
            localVideoContainer.addView(localView);
            renderers.put("local", (TextureViewRenderer) localView);

            // 设置本地视频视图
            VideoCanvas videoCanvas = new VideoCanvas(
                localView,
                Constants.RENDER_MODE_HIDDEN,
                SharedPrefsUtil.getUserId().toString()
            );
            mRtcEngine.setupLocalVideo(videoCanvas);
            
            Log.d(TAG, "本地视频设置完成");
        } catch (Exception e) {
            Log.e(TAG, "设置本地视频失败: " + e.getMessage());
        }
    }
    
    private void setupRemoteVideo(String uid) {
        try {
            Log.d(TAG, "开始设置远程视频 - 用户ID: " + uid);
            Log.d(TAG, "当前RtcEngine状态: " + (mRtcEngine != null ? "已初始化" : "未初始化"));
            Log.d(TAG, "远程视频容器状态: " + (remoteVideoContainer != null ? "已创建" : "未创建"));
            
            // 使用RtcEngine.CreateRendererView创建视图
            TextureView remoteView = RtcEngine.CreateRendererView(getBaseContext());
            Log.d(TAG, "远程视频视图创建成功");
            
            remoteVideoContainer.addView(remoteView);
            renderers.put(uid, (TextureViewRenderer) remoteView);
            Log.d(TAG, "远程视频视图已添加到容器，渲染器已保存");
            
            // 设置远程视频视图
            VideoCanvas videoCanvas = new VideoCanvas(
                remoteView,
                Constants.RENDER_MODE_HIDDEN,
                uid
            );
            Log.d(TAG, "VideoCanvas创建成功 - uid: " + uid);
            
            mRtcEngine.setupRemoteVideo(videoCanvas);
            Log.d(TAG, "setupRemoteVideo调用完成");
            
            runOnUiThread(() -> {
                Log.d(TAG, "远程视频设置成功 - 用户ID: " + uid);
            });
        } catch (Exception e) {
            Log.e(TAG, "设置远程视频失败: " + e.getMessage(), e);
            runOnUiThread(() -> {
                Toast.makeText(VideoCallActivity.this, "远程视频设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, final String uid, int elapsed) {
            runOnUiThread(() -> {
                Log.i(TAG, "加入频道成功 - 频道: " + channel + ", 用户ID: " + uid);
          //      Toast.makeText(VideoCallActivity.this, "成功加入视频通话", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onUserJoined(final String uid, int elapsed) {
            runOnUiThread(() -> {
                Log.i(TAG, "远程用户加入 - 用户ID: " + uid);
                remoteUserId = uid;
            });
        }

        @Override
        public void onUserOffline(final String uid, int reason) {
            runOnUiThread(() -> {
                Log.i(TAG, "远程用户离开 - 用户ID: " + uid + ", 原因: " + reason);
                Toast.makeText(VideoCallActivity.this, "对方已离开通话", Toast.LENGTH_SHORT).show();
                removeRemoteVideo(uid);
                
                // 立即销毁RTC实例并关闭页面
                Log.d(TAG, "检测到对方离开，立即销毁RTC实例并关闭页面");
                if (mRtcEngine != null) {
                    try {
                        RtcEngine.destroy();
                        mRtcEngine = null;
                        Log.d(TAG, "RTC实例已销毁");
                    } catch (Exception e) {
                        Log.e(TAG, "销毁RTC实例时发生异常: " + e.getMessage(), e);
                    }
                }
                
                // 立即关闭页面
                finish();
            });
        }
        
        @Override
        public void onRemoteVideoStateChanged(String uid, int state, int reason, int elapsed) {
            runOnUiThread(() -> {
                String stateStr = "";
                switch (state) {
                    case Constants.REMOTE_VIDEO_STATE_STOPPED:
                        stateStr = "已停止";
                        break;
                    case Constants.REMOTE_VIDEO_STATE_STARTING:
                        stateStr = "正在启动";
                        break;
                    case Constants.REMOTE_VIDEO_STATE_DECODING:
                        stateStr = "正在解码";
                        break;
                    case Constants.REMOTE_VIDEO_STATE_FROZEN:
                        stateStr = "已冻结";
                        break;
                    case Constants.REMOTE_VIDEO_STATE_FAILED:
                        stateStr = "失败";
                        break;
                    default:
                        stateStr = "未知状态(" + state + ")";
                        break;
                }
                
                Log.d(TAG, "远程视频状态改变 - 用户ID: " + uid + ", 状态: " + stateStr + ", 原因: " + reason + ", 延迟: " + elapsed);
                
                // 如果视频状态是正在解码或正在启动，都尝试设置远程视频
                if (state == Constants.REMOTE_VIDEO_STATE_DECODING || state == Constants.REMOTE_VIDEO_STATE_STARTING) {
                    Log.d(TAG, "尝试设置远程视频 - 用户ID: " + uid + ", 状态: " + stateStr);
                    setupRemoteVideo(uid);
                } else if (state == Constants.REMOTE_VIDEO_STATE_FAILED) {
                    Log.e(TAG, "远程视频解码失败 - 用户ID: " + uid + ", 原因: " + reason);
                }
            });
        }

        @Override
        public void onError(int err) {
            runOnUiThread(() -> {
                Log.e(TAG, "发生错误: " + err);
             //   Toast.makeText(VideoCallActivity.this, "视频通话引擎错误: " + err, Toast.LENGTH_SHORT).show();
            });
        }
    };
    
    private void removeRemoteVideo(String uid) {
        try {
            TextureViewRenderer view = renderers.get(uid);
            if (view != null) {
                // 从父容器中移除视图
                ViewGroup parent = (ViewGroup) view.getParent();
                if (parent != null) {
                    parent.removeView(view);
                }
                renderers.remove(uid);
                Log.d(TAG, "移除远程视频视图 - 用户ID: " + uid);
            }
        } catch (Exception e) {
            Log.e(TAG, "移除远程视频视图失败: " + e.getMessage());
        }
    }
    
    private void updateCallStatus(int status, String callId, String blindPhone, String volunteerPhone) {
        String statusText = "";
        switch (status) {
            case VideoCallManager.CALL_STATUS_IDLE:
                statusText = "空闲";
                break;
            case VideoCallManager.CALL_STATUS_WAITING:
                statusText = "等待志愿者接听...";
                break;
            case VideoCallManager.CALL_STATUS_IN_CALL:
                statusText = "通话中";
                // 直接使用VideoCallManager的callStartTime，不再自己管理
                startCallDurationTimer();
                break;
            case VideoCallManager.CALL_STATUS_ENDED:
                statusText = "通话结束";
                endCall();
                break;
        }
        
        tvCallStatus.setText(statusText);
        Log.d(TAG, "通话状态更新: " + statusText);
    }
    
    private void handleWebSocketMessage(SocketDataV0 data) {
        Log.d(TAG, "处理WebSocket消息: " + data.getRequestType());
        
        // 检查Activity是否还在运行
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Activity已结束，跳过消息处理");
            return;
        }
        
        switch (data.getRequestType()) {
            case 1: // 匹配成功
                Log.d(TAG, "收到匹配成功消息");
                handleMatchSuccess(data);
                break;
            case 2: // 视频初始化成功
                Log.d(TAG, "收到视频初始化成功消息");
                handleVideoInit(data);
                break;
            case 3: // 视频求助请求
                Log.d(TAG, "收到视频求助请求消息");
                // 盲人端不需要处理视频求助请求
                break;
            case 4: // 志愿者接听
                Log.d(TAG, "收到志愿者接听消息");
                // 盲人端不需要处理志愿者接听消息
                break;
            case 5: // 通话结束
                Log.d(TAG, "收到通话结束消息");
                handleCallEnd(data);
                break;
            default:
                Log.d(TAG, "未处理的消息类型: " + data.getRequestType());
                break;
        }
    }
    
    private void handleVideoInit(SocketDataV0 data) {
        Log.d(TAG, "视频初始化成功");
        // 视频初始化成功后，可以开始设置远程视频
        // 假设data中包含远程用户的uid
        String remoteUid = data.getExtraData(); // 假设extraData中包含远程用户的uid
        if (remoteUid != null && !remoteUid.isEmpty()) {
            setupRemoteVideo(remoteUid);
        } else {
            Log.w(TAG, "视频初始化成功，但未收到远程用户ID");
        }
    }
    
    private void handleMatchSuccess(SocketDataV0 data) {
        Log.d(TAG, "处理匹配成功消息");
        
        // 检查是否已经处理过
        if (hasHandledMatchSuccess) {
            Log.w(TAG, "已处理过匹配成功消息，跳过重复处理");
            return;
        }
        
        hasHandledMatchSuccess = true;
        
        // 检查Activity是否还在运行
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Activity已结束，跳过匹配成功处理");
            return;
        }
        
        Log.d(TAG, "匹配成功，准备加入视频频道");
        Log.d(TAG, "频道ID: " + data.getChannelId());
        
        // 加入视频通话频道，使用channelId
        String channelId = String.valueOf(data.getChannelId());
        joinVideoChannel(channelId);
    }
    
    private void handleCallEnd(SocketDataV0 data) {
        Log.d(TAG, "通话结束");
        Toast.makeText(this, "通话已结束", Toast.LENGTH_SHORT).show();
        endCall();
    }
    
    private void joinVideoChannel(String channelId) {
        try {
            // 加入频道 - 使用正确的API调用
            mRtcEngine.joinChannel("", channelId, "Extra Optional Data", SharedPrefsUtil.getUserId().toString());
            
            Log.d(TAG, "加入视频频道: " + channelId);
            
        } catch (Exception e) {
            Log.e(TAG, "加入视频频道失败: " + e.getMessage());
            Toast.makeText(this, "加入视频频道失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void hangupCall() {
        Log.d(TAG, "用户主动挂断通话");
        boolean isEmergencyHelp = getIntent().getBooleanExtra("isEmergencyHelp", false);
        
        if (isEmergencyHelp) {
            // 紧急求助挂断 - 使用EmergencyHelpManager处理
           
            EmergencyHelpManager emergencyHelpManager = EmergencyHelpManager.getInstance();
            emergencyHelpManager.hangupEmergencyHelp();
        } else {
            // 普通视频挂断
            String token = SharedPrefsUtil.getToken();
            if (token != null && !token.isEmpty()) {
                String authToken = "Bearer " + token;
                ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
                apiService.hangupVideohelp(authToken).enqueue(new Callback<BaseResponse<Boolean>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                        Log.d(TAG, "挂断API响应 - HTTP状态码: " + response.code());
                    }
                    @Override
                    public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                        Log.e(TAG, "挂断API调用失败", t);
                    }
                });
            } else {
                Log.e(TAG, "Token为空，无法调用挂断API");
            }
        }
        
        // 结束通话
        endCall();
    }





    private void toggleMute() {
        if (mRtcEngine != null) {
            isMuted = !isMuted;
            mRtcEngine.muteLocalAudioStream(isMuted);
            
            // 更新按钮图标
            btnMute.setImageResource(isMuted ? R.drawable.icon_mic_off : R.drawable.icon_mic_on);
            
        }
    }
    
    private void switchCamera() {
        if (mRtcEngine != null) {
            isFrontCamera = !isFrontCamera;
            mRtcEngine.switchCamera();
            
        }
    }
    
    private void startCallDurationTimer() {
        if (callDurationHandler != null && callDurationRunnable != null) {
            callDurationHandler.post(callDurationRunnable);
        }
    }
    
    private void updateCallDuration() {
        // 直接使用VideoCallManager的callStartTime
        long managerCallStartTime = videoCallManager.getCallStartTime();
        if (managerCallStartTime > 0) {
            long duration = System.currentTimeMillis() - managerCallStartTime;
            long seconds = duration / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            String timeText = String.format("%02d:%02d", minutes, seconds);
            tvCallDuration.setText(timeText);
        }
    }
    
    private void toggleSpeaker() {
        if (mRtcEngine != null) {
            isSpeakerOn = !isSpeakerOn;
            mRtcEngine.setDefaultAudioRoutetoSpeakerphone(isSpeakerOn);
            
            // 更新按钮图标
            btnSpeaker.setImageResource(isSpeakerOn ? R.drawable.icon_speaker_on : R.drawable.icon_speaker_off);
            
        }
    }
    
    private void endCall() {
        Log.d(TAG, "开始结束通话");
        
        try {
            if (mRtcEngine != null) {
                Log.d(TAG, "调用leaveChannel");
                int result = mRtcEngine.leaveChannel();
                Log.d(TAG, "leaveChannel调用结果: " + result);
                
                if (result != 0) {
                    Log.w(TAG, "leaveChannel返回非零结果: " + result + "，但继续执行清理");
                }
            } else {
                Log.w(TAG, "RtcEngine为null，跳过leaveChannel调用");
            }
        } catch (Exception e) {
            Log.e(TAG, "调用leaveChannel时发生异常: " + e.getMessage(), e);
            // 即使发生异常也继续执行清理
        }
        
        // 清理资源
        try {
            // 清理视频视图
            if (localVideoContainer != null) {
                localVideoContainer.removeAllViews();
            }
            if (remoteVideoContainer != null) {
                remoteVideoContainer.removeAllViews();
            }
            
            // 清理渲染器
            renderers.clear();
            
            Log.d(TAG, "资源清理完成");
        } catch (Exception e) {
            Log.e(TAG, "清理资源时发生异常: " + e.getMessage(), e);
        }
        
        // 销毁RTC实例
        try {
            if (mRtcEngine != null) {
                Log.d(TAG, "销毁RTC实例");
                RtcEngine.destroy();
                mRtcEngine = null;
                Log.d(TAG, "RTC实例已销毁");
            }
        } catch (Exception e) {
            Log.e(TAG, "销毁RTC实例时发生异常: " + e.getMessage(), e);
        }
        
        // 停止计时器
        if (callDurationHandler != null && callDurationRunnable != null) {
            callDurationHandler.removeCallbacks(callDurationRunnable);
        }
        
        // 延迟一点时间再结束Activity，给leaveChannel一些时间
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "结束VideoCallActivity");
            finish();
        }, 500);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 重置静态标志
        isActivityRunning = false;
        
        // 停止计时器
        if (callDurationHandler != null && callDurationRunnable != null) {
            callDurationHandler.removeCallbacks(callDurationRunnable);
        }
        
        // 移除监听器
        if (videoCallManager != null) {
            videoCallManager.removeStatusListener(null);
        }
        
        if (webSocketManager != null) {
            webSocketManager.removeMessageListener(null);
        }
        
        // 销毁RtcEngine
        if (mRtcEngine != null) {
            try {
                RtcEngine.destroy();
                mRtcEngine = null;
                Log.d(TAG, "RTC实例已销毁");
            } catch (Exception e) {
                Log.e(TAG, "销毁RTC实例时发生异常: " + e.getMessage(), e);
            }
        }
        
        // 重置HomeFragment的isVideoCallStarted标志
        com.swj.shiwujie.blind.ui.home.HomeFragment.resetVideoCallStartedFlag();
        Log.d(TAG, "VideoCallActivity销毁");
    }
} 