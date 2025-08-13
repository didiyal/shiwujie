package com.swj.shiwujie.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

/**
 * 讯飞在线语音合成管理器
 * 按照官方demo标准实现
 */
public class TTSManager {
    private static final String TAG = "TTSManager";
    
    private Context context;
    private SpeechSynthesizer mTts;
    private OnTTSListener ttsListener;
    private boolean isInitialized = false;
    
    // 默认发音人 (V3.0版本)
    private String voicer = "x4_yezi";  // 讯飞小露
    
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    
    // 缓冲进度和播放进度
    private int mPercentForBuffering = 0;
    private int mPercentForPlaying = 0;
    
    // 设置文件名
    private static final String PREFER_NAME = "com.iflytek.setting";
    
    public interface OnTTSListener {
        void onTTSStart();
        void onTTSProgress(int progress, int beginPos, int endPos);
        void onTTSComplete();
        void onTTSError(String error);
        void onTTSPause();
        void onTTSResume();
        void onTTSBufferProgress(int percent, int beginPos, int endPos, String info);
    }
    
    public TTSManager(Context context) {
        this.context = context;
        initSynthesizer();
    }
    
    /**
     * 初始化语音合成器
     */
    private void initSynthesizer() {
        try {
            // 创建语音合成对象
            mTts = SpeechSynthesizer.createSynthesizer(context, mTtsInitListener);
        } catch (Exception e) {
            Log.e(TAG, "创建语音合成器失败", e);
            isInitialized = false;
        }
    }
    
    /**
     * 初始化监听器
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "初始化失败,错误码：" + code);
                isInitialized = false;
                if (ttsListener != null) {
                    ttsListener.onTTSError("初始化失败,错误码：" + code);
                }
            } else {
                Log.d(TAG, "初始化成功");
                isInitialized = true;
            }
        }
    };
    
    /**
     * 合成回调监听器
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            Log.d(TAG, "开始播放");
            if (ttsListener != null) {
                ttsListener.onTTSStart();
            }
        }
        
        @Override
        public void onSpeakPaused() {
            Log.d(TAG, "暂停播放");
            if (ttsListener != null) {
                ttsListener.onTTSPause();
            }
        }
        
        @Override
        public void onSpeakResumed() {
            Log.d(TAG, "继续播放");
            if (ttsListener != null) {
                ttsListener.onTTSResume();
            }
        }
        
        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
            // 合成进度
            Log.d(TAG, "缓冲进度: " + percent + "%");
            mPercentForBuffering = percent;
            if (ttsListener != null) {
                ttsListener.onTTSBufferProgress(percent, beginPos, endPos, info);
            }
        }
        
        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度（移除调试日志）
            mPercentForPlaying = percent;
            if (ttsListener != null) {
                ttsListener.onTTSProgress(percent, beginPos, endPos);
            }
        }
        
        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                Log.d(TAG, "播放完成");
                if (ttsListener != null) {
                    ttsListener.onTTSComplete();
                }
            } else {
                Log.e(TAG, "播放出错: " + error.getPlainDescription(true));
                if (ttsListener != null) {
                    ttsListener.onTTSError(error.getPlainDescription(true));
                }
            }
        }
        
        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                Log.d(TAG, "session id =" + sid);
            }
        }
    };
    
    /**
     * 开始语音合成
     * @param text 要合成的文本
     */
    public void startSpeaking(String text) {
        if (mTts == null) {
            Log.e(TAG, "语音合成器未初始化");
            if (ttsListener != null) {
                ttsListener.onTTSError("语音合成器未初始化");
            }
            return;
        }
        
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "合成文本为空");
            return;
        }
        
        try {
            Log.d(TAG, "开始语音合成，文本: " + text);
            
            // 设置参数
            setParam();
            
            // 开始合成
            int code = mTts.startSpeaking(text, mTtsListener);
            
            if (code != ErrorCode.SUCCESS) {
                String errorMsg = "语音合成失败,错误码: " + code;
                Log.e(TAG, errorMsg);
                if (ttsListener != null) {
                    ttsListener.onTTSError(errorMsg);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "开始语音合成失败", e);
            if (ttsListener != null) {
                ttsListener.onTTSError("语音合成失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 参数设置
     */
    private void setParam() {
        if (mTts == null) return;
        
        try {
            // 清空参数
            mTts.setParameter(SpeechConstant.PARAMS, null);
            
            // 根据合成引擎设置相应参数
            if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
                mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
                
                // 设置在线合成发音人 (V3.0版本)
                mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
                
                // 设置合成语速
                mTts.setParameter(SpeechConstant.SPEED, getSpeedPreference());
                
                // 设置合成音调
                mTts.setParameter(SpeechConstant.PITCH, getPitchPreference());
                
                // 设置合成音量
                mTts.setParameter(SpeechConstant.VOLUME, getVolumePreference());
                
                // V3.0版本特殊配置
                mTts.setParameter("vcn", voicer);  // 兼容V3.0版本
                
            } else {
                mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
                mTts.setParameter(SpeechConstant.VOICE_NAME, "");
            }
            
            // 设置播放器音频流类型
            mTts.setParameter(SpeechConstant.STREAM_TYPE, getStreamPreference());
            
            // 设置播放合成音频打断音乐播放，默认为true
            mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "false");
            
            // 设置音频格式
            mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "pcm");
            
            Log.d(TAG, "语音合成器参数设置完成");
        } catch (Exception e) {
            Log.e(TAG, "设置语音合成器参数失败", e);
        }
    }
    
    /**
     * 获取语速设置
     */
    private String getSpeedPreference() {
        SharedPreferences prefs = context.getSharedPreferences(PREFER_NAME, Context.MODE_PRIVATE);
        return prefs.getString("speed_preference", "50");
    }
    
    /**
     * 获取音调设置
     */
    private String getPitchPreference() {
        SharedPreferences prefs = context.getSharedPreferences(PREFER_NAME, Context.MODE_PRIVATE);
        return prefs.getString("pitch_preference", "50");
    }
    
    /**
     * 获取音量设置
     */
    private String getVolumePreference() {
        SharedPreferences prefs = context.getSharedPreferences(PREFER_NAME, Context.MODE_PRIVATE);
        return prefs.getString("volume_preference", "50");
    }
    
    /**
     * 获取音频流类型设置
     */
    private String getStreamPreference() {
        SharedPreferences prefs = context.getSharedPreferences(PREFER_NAME, Context.MODE_PRIVATE);
        return prefs.getString("stream_preference", "3");
    }
    
    /**
     * 停止语音合成
     */
    public void stopSpeaking() {
        if (mTts != null && isInitialized) {
            try {
                mTts.stopSpeaking();
                Log.d(TAG, "停止语音合成");
            } catch (Exception e) {
                Log.e(TAG, "停止语音合成失败", e);
            }
        }
    }
    
    /**
     * 暂停语音合成
     */
    public void pauseSpeaking() {
        if (mTts != null && isInitialized) {
            try {
                mTts.pauseSpeaking();
                Log.d(TAG, "暂停语音合成");
            } catch (Exception e) {
                Log.e(TAG, "暂停语音合成失败", e);
            }
        }
    }
    
    /**
     * 恢复语音合成
     */
    public void resumeSpeaking() {
        if (mTts != null && isInitialized) {
            try {
                mTts.resumeSpeaking();
                Log.d(TAG, "恢复语音合成");
            } catch (Exception e) {
                Log.e(TAG, "恢复语音合成失败", e);
            }
        }
    }
    
    /**
     * 是否正在播放
     */
    public boolean isSpeaking() {
        return mTts != null && mTts.isSpeaking();
    }
    
    /**
     * 设置发音人
     */
    public void setVoicer(String voicer) {
        this.voicer = voicer;
        Log.d(TAG, "设置发音人: " + voicer);
    }
    

    
    /**
     * 获取当前发音人
     */
    public String getVoicer() {
        return voicer;
    }
    
    /**
     * 设置引擎类型
     */
    public void setEngineType(String engineType) {
        this.mEngineType = engineType;
        Log.d(TAG, "设置引擎类型: " + engineType);
    }
    
    /**
     * 获取当前引擎类型
     */
    public String getEngineType() {
        return mEngineType;
    }
    
    /**
     * 获取缓冲进度
     */
    public int getBufferProgress() {
        return mPercentForBuffering;
    }
    
    /**
     * 获取播放进度
     */
    public int getPlayProgress() {
        return mPercentForPlaying;
    }
    
    /**
     * 设置语音合成监听器
     */
    public void setOnTTSListener(OnTTSListener listener) {
        this.ttsListener = listener;
    }
    
    /**
     * 释放资源
     */
    public void destroy() {
        try {
            if (mTts != null) {
                mTts.stopSpeaking();
                mTts.destroy();
                mTts = null;
            }
            isInitialized = false;
            Log.d(TAG, "语音合成器已销毁");
        } catch (Exception e) {
            Log.e(TAG, "销毁语音合成器失败", e);
        }
    }
}
