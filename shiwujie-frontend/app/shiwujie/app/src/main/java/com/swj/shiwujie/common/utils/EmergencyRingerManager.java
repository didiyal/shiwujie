package com.swj.shiwujie.common.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.Build;
import android.util.Log;

/**
 * 紧急求助响铃管理器
 * 负责在收到紧急求助时触发响铃和震动提醒
 */
public class EmergencyRingerManager {
    private static final String TAG = "EmergencyRingerManager";
    private static final long RING_DURATION = 30000; // 响铃持续30秒
    private static final long[] VIBRATE_PATTERN = {0, 1000, 1000}; // 震动模式：等待0ms，震动1s，停止1s
    
    private static EmergencyRingerManager instance;
    private AudioManager audioManager;
    private Vibrator vibrator;
    private MediaPlayer mediaPlayer;
    private int originalRingerMode;
    private boolean isRinging = false;
    private Handler handler;
    private Runnable stopRingerRunnable;
    
    private EmergencyRingerManager() {
        handler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized EmergencyRingerManager getInstance() {
        if (instance == null) {
            instance = new EmergencyRingerManager();
        }
        return instance;
    }
    
    /**
     * 开始紧急求助响铃和震动
     * @param context 上下文
     */
    public void startEmergencyRinger(Context context) {
        if (isRinging) {
            Log.d(TAG, "已在响铃中，忽略重复调用");
            return;
        }
        
        Log.d(TAG, "开始紧急求助响铃和震动");
        
        try {
            // 获取音频管理器
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                Log.e(TAG, "无法获取AudioManager");
                return;
            }
            
            // 获取震动器
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            
            // 保存当前铃声模式
            originalRingerMode = audioManager.getRingerMode();
            Log.d(TAG, "当前铃声模式: " + getRingerModeString(originalRingerMode));
            
            isRinging = true;
            
            // 根据当前模式决定提醒方式
            switch (originalRingerMode) {
                case AudioManager.RINGER_MODE_NORMAL:
                    // 响铃模式：播放铃声 + 震动
                    Log.d(TAG, "响铃模式：播放铃声 + 震动");
                    playRingtone(context);
                    startVibrate();
                    break;
                    
                case AudioManager.RINGER_MODE_VIBRATE:
                    // 震动模式：只震动
                    Log.d(TAG, "震动模式：只震动");
                    startVibrate();
                    break;
                    
                case AudioManager.RINGER_MODE_SILENT:
                    // 静音模式：强制震动（紧急情况）
                    Log.d(TAG, "静音模式：强制震动（紧急情况）");
                    startVibrate();
                    break;
                    
                default:
                    Log.w(TAG, "未知铃声模式: " + originalRingerMode);
                    startVibrate(); // 至少震动
                    break;
            }
            
            // 设置自动停止响铃
            stopRingerRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "响铃超时，自动停止");
                    stopEmergencyRinger();
                }
            };
            handler.postDelayed(stopRingerRunnable, RING_DURATION);
            
        } catch (Exception e) {
            Log.e(TAG, "启动响铃失败", e);
            isRinging = false;
        }
    }
    
    /**
     * 播放铃声
     */
    private void playRingtone(Context context) {
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (ringtoneUri == null) {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            
            if (ringtoneUri != null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(context, ringtoneUri);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                mediaPlayer.setLooping(true); // 循环播放
                mediaPlayer.prepare();
                mediaPlayer.start();
                Log.d(TAG, "开始播放铃声");
            } else {
                Log.w(TAG, "无法获取系统铃声URI");
            }
        } catch (Exception e) {
            Log.e(TAG, "播放铃声失败", e);
        }
    }
    
    /**
     * 开始震动
     */
    private void startVibrate() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(VIBRATE_PATTERN, 0));
                } else {
                    vibrator.vibrate(VIBRATE_PATTERN, 0);
                }
                Log.d(TAG, "开始震动");
            } else {
                Log.w(TAG, "设备不支持震动或震动器不可用");
            }
        } catch (Exception e) {
            Log.e(TAG, "启动震动失败", e);
        }
    }
    
    /**
     * 获取铃声模式字符串描述
     */
    private String getRingerModeString(int mode) {
        switch (mode) {
            case AudioManager.RINGER_MODE_NORMAL:
                return "响铃模式";
            case AudioManager.RINGER_MODE_VIBRATE:
                return "震动模式";
            case AudioManager.RINGER_MODE_SILENT:
                return "静音模式";
            default:
                return "未知模式(" + mode + ")";
        }
    }
    
    /**
     * 停止紧急求助响铃和震动
     */
    public void stopEmergencyRinger() {
        if (!isRinging) {
            return;
        }
        
        Log.d(TAG, "停止紧急求助响铃和震动");
        
        try {
            // 取消自动停止任务
            if (stopRingerRunnable != null) {
                handler.removeCallbacks(stopRingerRunnable);
                stopRingerRunnable = null;
            }
            
            // 停止铃声播放
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                    mediaPlayer = null;
                    Log.d(TAG, "铃声播放已停止");
                } catch (Exception e) {
                    Log.e(TAG, "停止铃声播放失败", e);
                }
            }
            
            // 停止震动
            if (vibrator != null) {
                try {
                    vibrator.cancel();
                    vibrator = null;
                    Log.d(TAG, "震动已停止");
                } catch (Exception e) {
                    Log.e(TAG, "停止震动失败", e);
                }
            }
            
            isRinging = false;
            audioManager = null;
            
        } catch (Exception e) {
            Log.e(TAG, "停止响铃失败", e);
        }
    }
    
    /**
     * 检查是否正在响铃
     * @return 是否正在响铃
     */
    public boolean isRinging() {
        return isRinging;
    }
    
    /**
     * 销毁管理器
     */
    public void destroy() {
        stopEmergencyRinger();
    }
}
