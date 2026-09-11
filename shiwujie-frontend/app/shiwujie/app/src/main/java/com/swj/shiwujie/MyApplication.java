package com.swj.shiwujie;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.Setting;
import com.swj.shiwujie.common.service.AIFloatingBallService;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    // App 前后台判定：started Activity 计数（2026-09-12 悬浮球改为退出软件后才显示）
    private int startedActivityCount = 0;
    private boolean isRotating = false;

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

        // 全局前后台监听：应用退到后台 → 显示 AI 悬浮球；回到前台 → 隐藏。
        // 替代原 AiFragment 页面级 show/hide（在软件内所有页面都不显示悬浮球）。
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                startedActivityCount++;
                if (startedActivityCount == 1 && !isRotating) {
                    // 0 → 1：应用回到前台
                    Log.d(TAG, "应用回到前台，隐藏AI悬浮球");
                    sendBallVisibilityBroadcast(false);
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                isRotating = activity.isChangingConfigurations();
                startedActivityCount--;
                if (startedActivityCount == 0 && !isRotating) {
                    // 1 → 0：应用退到后台（退出软件）
                    Log.d(TAG, "应用退到后台，显示AI悬浮球");
                    sendBallVisibilityBroadcast(true);
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });

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

    /** 通知悬浮球服务显示（true）/隐藏（false）；服务未运行（志愿者端/未登录）时广播无人接收，无副作用 */
    private void sendBallVisibilityBroadcast(boolean show) {
        try {
            Intent intent = new Intent(show ? AIFloatingBallService.ACTION_SHOW_BALL
                                            : AIFloatingBallService.ACTION_HIDE_BALL);
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "发送悬浮球显隐广播失败", e);
        }
    }
}
