package com.swj.shiwujie.volunteer;

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

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.VideoCallManager;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.SocketDataV0;

import org.ar.rtc.Constants;
import org.ar.rtc.IRtcEngineEventHandler;
import org.ar.rtc.RtcEngine;
import org.ar.rtc.video.VideoCanvas;
import org.webrtc.TextureViewRenderer;

import java.util.HashMap;

/**
 * 志愿者端视频通话Activity
 * 集成AnyRTC SDK实现视频通话功能
 */
public class VideoCallActivity extends AppCompatActivity {
    private static final String TAG = "VolunteerVideoCallActivity";
    
    // UI组件
    private FrameLayout localVideoContainer;
    private FrameLayout remoteVideoContainer;
    private Button btnHangup;
    private Button btnMute;
    private Button btnSwitchCamera;
    private TextView tvCallStatus;
    private TextView tvCallDuration;
    private TextView tvBlindInfo;
    
    // AnyRTC SDK
    private RtcEngine mRtcEngine;
    private HashMap<String, TextureViewRenderer> renderers = new HashMap<>();
    
    // 网络管理
    private WebSocketManager webSocketManager;
    private VideoCallManager videoCallManager;
    
    // 通话状态
    private boolean isMuted = false;
    private boolean isFrontCamera = true;
    private long callStartTime = 0;
    private String blindPhone = "";
    private String remoteUserId = "";
    


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_video_call);
        
        initViews();
        initManagers();
        initEngineAndJoinChannel();
    }
    
    private void initViews() {
        localVideoContainer = findViewById(R.id.local_video_container);
        remoteVideoContainer = findViewById(R.id.remote_video_container);
        btnHangup = findViewById(R.id.btn_hangup);
        btnMute = findViewById(R.id.btn_mute);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        tvCallStatus = findViewById(R.id.tv_call_status);
        tvCallDuration = findViewById(R.id.tv_call_duration);
        tvBlindInfo = findViewById(R.id.tv_blind_info);
        
        // 设置点击事件
        btnHangup.setOnClickListener(v -> hangupCall());
        btnMute.setOnClickListener(v -> toggleMute());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());
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
            Toast.makeText(this, "初始化引擎失败", Toast.LENGTH_SHORT).show();
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
            mRtcEngine.setDefaultAudioRoutetoSpeakerphone(false);
            
            // 启用本地预览
            mRtcEngine.startPreview();
            
            // 设置音频初始状态
            mRtcEngine.adjustRecordingSignalVolume(100);
            mRtcEngine.adjustPlaybackSignalVolume(100);
            mRtcEngine.muteLocalAudioStream(false);
            
            Log.d(TAG, "AnyRTC SDK初始化成功");
            
        } catch (Exception e) {
            Log.e(TAG, "AnyRTC SDK初始化失败: " + e.getMessage());
            throw new RuntimeException("RTC SDK初始化失败：" + e.getMessage());
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
            
            // 使用RtcEngine.CreateRendererView创建视图
            TextureView remoteView = RtcEngine.CreateRendererView(getBaseContext());
            remoteVideoContainer.addView(remoteView);
            renderers.put(uid, (TextureViewRenderer) remoteView);
            
            // 设置远程视频视图
            VideoCanvas videoCanvas = new VideoCanvas(
                remoteView,
                Constants.RENDER_MODE_HIDDEN,
                uid
            );
            mRtcEngine.setupRemoteVideo(videoCanvas);
            
            runOnUiThread(() -> {
                Log.d(TAG, "远程视频设置成功");
            });
        } catch (Exception e) {
            Log.e(TAG, "设置远程视频失败: " + e.getMessage());
        }
    }
    
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, final String uid, int elapsed) {
            runOnUiThread(() -> {
                Log.i(TAG, "加入频道成功 - 频道: " + channel + ", 用户ID: " + uid);
                Toast.makeText(VideoCallActivity.this, "成功加入视频通话", Toast.LENGTH_SHORT).show();
                
                // 发送视频初始化成功消息
                sendVideoInitMessage(channel);
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
                Log.i(TAG, "远程用户离开 - 用户ID: " + uid);
                Toast.makeText(VideoCallActivity.this, "对方已离开通话", Toast.LENGTH_SHORT).show();
                removeRemoteVideo(uid);
            });
        }
        
        @Override
        public void onRemoteVideoStateChanged(String uid, int state, int reason, int elapsed) {
            runOnUiThread(() -> {
                if (state == Constants.REMOTE_VIDEO_STATE_DECODING) {
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onError(int err) {
            runOnUiThread(() -> {
                Log.e(TAG, "发生错误: " + err);
                Toast.makeText(VideoCallActivity.this, "视频通话引擎错误: " + err, Toast.LENGTH_SHORT).show();
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
                statusText = "等待接听...";
                break;
            case VideoCallManager.CALL_STATUS_IN_CALL:
                statusText = "通话中";
                if (callStartTime == 0) {
                    callStartTime = System.currentTimeMillis();
                    startCallDurationTimer();
                }
                break;
            case VideoCallManager.CALL_STATUS_ENDED:
                statusText = "通话结束";
                endCall();
                break;
        }
        
        tvCallStatus.setText(statusText);
        if (blindPhone != null && !blindPhone.isEmpty()) {
            tvBlindInfo.setText("视障用户: " + blindPhone);
        }
        Log.d(TAG, "通话状态更新: " + statusText);
    }
    
    private void handleWebSocketMessage(SocketDataV0 data) {
        Log.d(TAG, "处理WebSocket消息: " + data.toString());
        
        switch (data.getRequestType()) {
            case 1: // 匹配成功
                handleMatchSuccess(data);
                break;
            case 5: // 通话结束
                handleCallEnd(data);
                break;
        }
    }
    
    private void handleMatchSuccess(SocketDataV0 data) {
        Log.d(TAG, "匹配成功，开始视频通话");
        Toast.makeText(this, "匹配成功，开始视频通话", Toast.LENGTH_SHORT).show();
        
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
    
    private void sendVideoInitMessage(String channelId) {
        try {
            // 从Intent中获取通话信息
            long channelIdLong = getIntent().getLongExtra("channelId", 0);
            String blindPhone = getIntent().getStringExtra("blindPhone");
            String volunteerPhone = getIntent().getStringExtra("volunteerPhone");
            
            // 创建视频初始化成功消息
            SocketDataV0 initData = new SocketDataV0();
            initData.setRequestType(2); // 视频初始化成功通知
            initData.setBlindPhone(blindPhone);
            initData.setVolunteerPhone(volunteerPhone);
            initData.setChannelId(channelIdLong);
            
            Log.d(TAG, "发送视频初始化成功消息");
            Log.d(TAG, "消息内容: requestType=" + initData.getRequestType() + 
                  ", blindPhone=" + initData.getBlindPhone() + 
                  ", volunteerPhone=" + initData.getVolunteerPhone() + 
                  ", channelId=" + initData.getChannelId());
            
            webSocketManager.sendMessage(initData);
            Log.d(TAG, "视频初始化成功消息已发送");
            
        } catch (Exception e) {
            Log.e(TAG, "发送视频初始化消息失败: " + e.getMessage());
        }
    }
    
    private void hangupCall() {
        Log.d(TAG, "用户主动挂断通话");
        
        // 发送挂断消息
        SocketDataV0 hangupData = SocketDataV0.createCallEnd(
            videoCallManager.getCurrentCallId(),
            SharedPrefsUtil.getPhone(),
            true // 志愿者用户
        );
        webSocketManager.sendMessage(hangupData);
        
        // 结束通话
        endCall();
    }
    
    private void toggleMute() {
        if (mRtcEngine != null) {
            isMuted = !isMuted;
            mRtcEngine.muteLocalAudioStream(isMuted);
            
            btnMute.setText(isMuted ? "取消静音" : "静音");
            Toast.makeText(this, isMuted ? "已静音" : "已取消静音", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void switchCamera() {
        if (mRtcEngine != null) {
            isFrontCamera = !isFrontCamera;
            mRtcEngine.switchCamera();
            
            Toast.makeText(this, "切换摄像头", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startCallDurationTimer() {
        // 这里可以添加定时器来更新通话时长显示
        // 简化实现，实际可以使用Handler或Timer
    }
    
    private void endCall() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
        
        callStartTime = 0;
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 移除监听器
        if (videoCallManager != null) {
            videoCallManager.removeStatusListener(null);
        }
        
        if (webSocketManager != null) {
            webSocketManager.removeMessageListener(null);
        }
        
        // 销毁RtcEngine
        if (mRtcEngine != null) {
            RtcEngine.destroy();
            mRtcEngine = null;
        }
        
        Log.d(TAG, "VideoCallActivity销毁");
    }
} 