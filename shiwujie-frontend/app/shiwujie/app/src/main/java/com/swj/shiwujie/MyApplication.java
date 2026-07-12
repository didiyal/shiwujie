package com.swj.shiwujie;

import android.app.Application;
import android.util.Log;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.Setting;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化讯飞语音识别SDK（按照官方文档要求）
        try {
            SpeechUtility.createUtility(this, "appid=26fe4713");
            
            // 以下语句用于设置日志开关：仅 DEBUG 包开启，release 关闭避免泄露
            Setting.setShowLog(BuildConfig.DEBUG);
            
            Log.d(TAG, "讯飞语音识别SDK初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "讯飞语音识别SDK初始化失败: " + e.getMessage(), e);
        }
        
        // 设置全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "未捕获的异常: " + throwable.getMessage(), throwable);
                
                // 记录详细的异常信息
                Log.e(TAG, "异常线程: " + thread.getName());
                Log.e(TAG, "异常堆栈: ", throwable);
                
                // 可以在这里添加崩溃日志上报逻辑
                
                // 对于Fragment生命周期相关的异常，不强制退出APP
                if (throwable instanceof IllegalStateException && 
                    throwable.getMessage() != null && 
                    throwable.getMessage().contains("Fragment")) {
                    Log.w(TAG, "检测到Fragment生命周期异常，不强制退出APP");
                    return;
                }
                
                // 对于其他严重异常，仍然退出APP
                Log.e(TAG, "检测到严重异常，准备退出APP");
                try {
                    // 延迟退出，给日志记录一些时间
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        System.exit(1);
                    }, 1000);
                } catch (Exception e) {
                    Log.e(TAG, "延迟退出失败，立即退出", e);
                    System.exit(1);
                }
            }
        });
        
        Log.d(TAG, "MyApplication onCreate");
    }
}
