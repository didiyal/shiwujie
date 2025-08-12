package com.swj.shiwujie.common.utils;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.iflytek.cloud.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 讯飞语音识别管理类
 * 按照官方文档要求实现语音听写功能
 */
public class SpeechRecognitionManager {
    private static final String TAG = "SpeechRecognitionManager";
    
    private SpeechRecognizer mIat;
    private Context mContext;
    private OnRecognitionListener mListener;
    private RecognizerListener mRecognizerListener;
    private InitListener mInitListener;
    
    // 按照官方demo实现结果累积
    private Map<Integer, String> resultMap = new HashMap<>();
    private int currentSequence = 0;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface OnRecognitionListener {
        void onResult(String result);
        void onPartialResult(String result); // 添加中间结果回调
        void onError(String error);
        void onStart();
        void onEnd();
        void onBeginOfSpeech();
        void onEndOfSpeech();
        void onVolumeChanged(int volume);
    }
    
    public SpeechRecognitionManager(Context context) {
        this.mContext = context;
        initSpeechRecognizer();
    }
    
    /**
     * 初始化讯飞语音识别
     * 按照官方文档要求配置参数
     */
    private void initSpeechRecognizer() {
        // 初始化讯飞语音识别
        SpeechUtility.createUtility(mContext, SpeechConstant.APPID + "=26fe4713");
        
        // 初始化监听器
        mInitListener = new InitListener() {
            @Override
            public void onInit(int code) {
                Log.d(TAG, "SpeechRecognizer init() code = " + code);
                if (code != com.iflytek.cloud.ErrorCode.SUCCESS) {
                    Log.e(TAG, "初始化失败，错误码：" + code);
                } else {
                    Log.d(TAG, "SpeechRecognizer初始化成功");
                }
            }
        };
        
        // 根据官方demo，createRecognizer需要传入InitListener
        mIat = SpeechRecognizer.createRecognizer(mContext, mInitListener);
        
        // 按照官方文档设置参数
        mIat.setParameter(SpeechConstant.CLOUD_GRAMMAR, null);
        mIat.setParameter(SpeechConstant.SUBJECT, null);
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        mIat.setParameter(SpeechConstant.VAD_BOS, "2000");
        mIat.setParameter(SpeechConstant.VAD_EOS, "6000");
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");
        
        // 设置语音听写监听器
        mRecognizerListener = new RecognizerListener() {
            @Override
            public void onResult(RecognizerResult result, boolean isLast) {
                if (mListener != null) {
                    // 使用IatResultParser解析讯飞返回的结果
                    String text = IatResultParser.parseIatResult(result.getResultString());
                    
                    if (isLast) {
                        // 最终结果，合并所有中间结果
                        String finalResult = mergeResults(text);
                        mListener.onResult(finalResult);
                        
                        // 强制调用onEnd，确保状态更新
                        mListener.onEnd();
                        
                        Log.d(TAG, "最终识别结果: " + finalResult);
                        
                        // 清空结果缓存
                        resultMap.clear();
                        currentSequence = 0;
                    } else {
                        // 中间结果，累积并显示
                        currentSequence++;
                        resultMap.put(currentSequence, text);
                        
                        // 显示累积的中间结果
                        String partialResult = mergeResults(text);
                        mListener.onPartialResult(partialResult);
                        Log.d(TAG, "中间识别结果: " + partialResult);
                    }
                }
            }
            
            @Override
            public void onError(SpeechError error) {
                if (mListener != null) {
                    mListener.onError(error.getErrorDescription());
                    // 确保异常情况下状态正确
                    mListener.onEnd();
                }
                Log.e(TAG, "语音识别错误: " + error.getErrorDescription());
            }
            
            @Override
            public void onBeginOfSpeech() {
                if (mListener != null) {
                    mListener.onBeginOfSpeech();
                }
                Log.d(TAG, "开始说话");
            }
            
            @Override
            public void onEndOfSpeech() {
                if (mListener != null) {
                    mListener.onEndOfSpeech();
                }
                Log.d(TAG, "结束说话");
            }
            
            @Override
            public void onVolumeChanged(int volume, byte[] data) {
                if (mListener != null) {
                    mListener.onVolumeChanged(volume);
                }
                Log.d(TAG, "音量变化: " + volume);
            }
            
            @Override
            public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
                // 事件回调
                Log.d(TAG, "语音识别事件: " + eventType);
            }
        };
        // 注意：根据官方demo，不需要调用setRecognizerListener
        // RecognizerListener是在startListening时作为参数传入的
    }
    
    /**
     * 合并所有中间结果，按照官方demo的方式
     */
    private String mergeResults(String currentText) {
        StringBuilder sb = new StringBuilder();
        
        // 按顺序合并所有中间结果
        for (int i = 1; i <= currentSequence; i++) {
            String text = resultMap.get(i);
            if (text != null) {
                sb.append(text);
            }
        }
        
        // 添加当前结果
        if (currentText != null && !currentText.isEmpty()) {
            sb.append(currentText);
        }
        
        return sb.toString();
    }
    
    /**
     * 开始语音识别
     */
    public void startListening() {
        if (mIat != null) {
            // 重置状态
            resultMap.clear();
            currentSequence = 0;
            
            // 根据官方demo，startListening需要传入RecognizerListener
            int ret = mIat.startListening(mRecognizerListener);
            if (ret == com.iflytek.cloud.ErrorCode.SUCCESS) {
                if (mListener != null) {
                    mListener.onStart();
                }
                Log.d(TAG, "开始语音识别");
            } else {
                Log.e(TAG, "开始语音识别失败，错误码：" + ret);
            }
        }
    }
    
    /**
     * 停止语音识别
     */
    public void stopListening() {
        if (mIat != null) {
            mIat.stopListening();
            Log.d(TAG, "停止语音识别");
        }
    }
    
    /**
     * 设置语音识别监听器
     */
    public void setOnRecognitionListener(OnRecognitionListener listener) {
        this.mListener = listener;
    }
    
    /**
     * 销毁语音识别器
     */
    public void destroy() {
        if (mIat != null) {
            mIat.destroy();
            Log.d(TAG, "语音识别器已销毁");
        }
    }
}
