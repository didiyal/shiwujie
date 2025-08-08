package com.swj.shiwujie;

import android.app.Application;
import android.util.Log;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 设置全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "未捕获的异常: " + throwable.getMessage(), throwable);
                
                // 记录详细的异常信息
                Log.e(TAG, "异常线程: " + thread.getName());
                Log.e(TAG, "异常堆栈: ", throwable);
                
                // 可以在这里添加崩溃日志上报逻辑
                
                // 确保应用能够正常退出
                System.exit(1);
            }
        });
        
        Log.d(TAG, "MyApplication onCreate");
    }
}
