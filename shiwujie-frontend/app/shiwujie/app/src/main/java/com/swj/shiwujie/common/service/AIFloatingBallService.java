package com.swj.shiwujie.common.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.swj.shiwujie.R;
import com.swj.shiwujie.blind.BlindHomeActivity;

/**
 * AI悬浮球服务
 * 提供全局悬浮球功能，支持后台保持
 */
public class AIFloatingBallService extends Service {
    private static final String TAG = "AIFloatingBallService";
    private static final String CHANNEL_ID = "ai_floating_ball_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private boolean isShowing = false;
    
    // 触摸事件相关
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private long lastClickTime = 0;
    
    // 广播接收器
    private BroadcastReceiver floatingBallReceiver;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AI悬浮球服务创建");
        
        try {
            // 验证用户身份：只有盲人端用户才能启动AI悬浮球服务
            if (!isBlindUser()) {
                Log.w(TAG, "非盲人端用户，停止AI悬浮球服务");
                stopSelf();
                return;
            }
            
            // 创建通知渠道（Android 8.0+）
            createNotificationChannel();
            
            // 构建通知
            Notification notification = createNotification();
            if (notification != null) {
                // 启动前台服务（必须在5秒内调用）
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "前台服务启动成功");
            } else {
                Log.e(TAG, "通知创建失败，无法启动前台服务");
                stopSelf();
                return;
            }
            
            // 初始化悬浮球
            initFloatingBall();
            
            // 注册广播接收器
            registerFloatingBallReceiver();
            
        } catch (Exception e) {
            Log.e(TAG, "服务创建失败", e);
            stopSelf();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AI悬浮球服务启动");
        // 启动时再次校验用户身份，防止被误启动
        try {
            if (!isBlindUser()) {
                Log.w(TAG, "非盲人或未登录，停止AI悬浮球服务");
                stopSelf();
                return START_NOT_STICKY;
            }
        } catch (Exception e) {
            Log.e(TAG, "启动时身份校验异常，停止服务", e);
            stopSelf();
            return START_NOT_STICKY;
        }
        // 返回非粘性，避免在不满足条件时被系统自动重启
        return START_NOT_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AI悬浮球服务销毁");
        
        // 移除悬浮球
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                Log.w(TAG, "移除悬浮球失败", e);
            }
        }
        
        // 注销广播接收器
        unregisterFloatingBallReceiver();
    }
    
    /**
     * 验证用户是否为盲人端用户
     */
    private boolean isBlindUser() {
        try {
            // 检查用户登录状态和身份
            if (!com.swj.shiwujie.common.utils.SharedPrefsUtil.isLoggedIn()) {
                Log.w(TAG, "用户未登录");
                return false;
            }
            
            // 检查是否为盲人端用户
            boolean isBlind = com.swj.shiwujie.common.utils.SharedPrefsUtil.isBlind();
            Log.d(TAG, "用户身份验证: " + (isBlind ? "盲人端" : "志愿者端"));
            return isBlind;
            
        } catch (Exception e) {
            Log.e(TAG, "验证用户身份失败", e);
            return false;
        }
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "AI悬浮球前台服务",
                        NotificationManager.IMPORTANCE_LOW
                    );
                    channel.setDescription("用于保持AI悬浮球在后台运行");
                    notificationManager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "创建通知渠道失败", e);
            }
        }
    }
    
        /**
     * 创建通知
     */
    private Notification createNotification() {
        try {
            // 创建点击事件，返回盲人端主界面
            Intent intent = new Intent(this, BlindHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // 添加身份验证标记，确保只能跳转到盲人端
            intent.putExtra("user_type", "blind");
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AI悬浮球运行中")
                .setContentText("点击返回应用主界面")
                .setSmallIcon(R.drawable.ai)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setSilent(true);
            
            return builder.build();
            
        } catch (Exception e) {
            Log.e(TAG, "创建前台服务通知失败", e);
            // 创建失败时返回基础通知
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AI悬浮球服务")
                .setContentText("服务正在运行")
                .setSmallIcon(R.drawable.ai)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
        }
    }
    
    /**
     * 初始化悬浮球
     */
    private void initFloatingBall() {
        try {
            Log.d(TAG, "开始初始化AI悬浮球...");
            
            // 获取WindowManager
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (windowManager == null) {
                Log.e(TAG, "无法获取WindowManager");
                return;
            }
            
            // 加载悬浮球布局
            floatingView = LayoutInflater.from(this).inflate(R.layout.ai_floating_ball, null);
            if (floatingView == null) {
                Log.e(TAG, "悬浮球布局加载失败");
                return;
            }
            
            // 设置触摸事件
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            lastClickTime = System.currentTimeMillis();
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            // 拖拽移动悬浮球
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            try {
                                windowManager.updateViewLayout(floatingView, params);
                            } catch (Exception e) {
                                Log.w(TAG, "更新悬浮球位置失败", e);
                            }
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastClickTime < 200) {
                                // 点击事件 - 跳转到AI页面
                                onAIBallClick();
                            }
                            return true;
                    }
                    return false;
                }
            });
            
            // 为无障碍模式添加点击监听器
            floatingView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "无障碍模式点击事件触发");
                    onAIBallClick();
                }
            });
            
            // 设置无障碍模式下的必要属性
            floatingView.setClickable(true);
            floatingView.setFocusable(true);
            floatingView.setFocusableInTouchMode(true);
            floatingView.setContentDescription("AI助手悬浮球，点击进入AI功能页面");
            
            // 显示悬浮球
            showFloatingBall();
            
            Log.d(TAG, "AI悬浮球初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "初始化AI悬浮球失败", e);
        }
    }
    
    /**
     * 显示悬浮球
     */
    private void showFloatingBall() {
        if (isShowing) {
            Log.w(TAG, "AI悬浮球已显示");
            return;
        }
        
        try {
            Log.d(TAG, "显示AI悬浮球");
            
            // 设置窗口参数
            params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 100;
            params.y = 300;
            
            // 添加悬浮球到屏幕
            windowManager.addView(floatingView, params);
            isShowing = true;
            
            Log.d(TAG, "AI悬浮球显示成功");
            
        } catch (Exception e) {
            Log.e(TAG, "显示AI悬浮球失败", e);
            // Toast.makeText(this, "显示AI悬浮球失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 隐藏悬浮球
     */
    private void hideFloatingBall() {
        if (!isShowing) {
            Log.w(TAG, "AI悬浮球未显示");
            return;
        }
        
        try {
            Log.d(TAG, "隐藏AI悬浮球");
            
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
            
            isShowing = false;
            Log.d(TAG, "AI悬浮球隐藏成功");
            
        } catch (Exception e) {
            Log.e(TAG, "隐藏AI悬浮球失败", e);
        }
    }
    
    /**
     * 获取窗口类型
     */
    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }
    
    /**
     * 处理悬浮球点击事件
     */
    private void onAIBallClick() {
        try {
            Log.d(TAG, "用户点击AI悬浮球，准备跳转到AI页面");
            
            // 直接跳转到AI页面，避免中间过渡页面
            Intent intent = new Intent(this, BlindHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("navigate_to_ai", true);
            intent.putExtra("direct_to_ai", true);  // 添加直接跳转标志
            startActivity(intent);
            
            Log.d(TAG, "AI悬浮球跳转成功");
            
        } catch (Exception e) {
            Log.e(TAG, "跳转失败", e);
            // Toast.makeText(this, "跳转失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 注册广播接收器
     */
    private void registerFloatingBallReceiver() {
        try {
            floatingBallReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action != null) {
                        switch (action) {
                            case "com.swj.shiwujie.SHOW_AI_FLOATING_BALL":
                                showFloatingBall();
                                break;
                            case "com.swj.shiwujie.HIDE_AI_FLOATING_BALL":
                                hideFloatingBall();
                                break;
                        }
                    }
                }
            };
            
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.swj.shiwujie.SHOW_AI_FLOATING_BALL");
            filter.addAction("com.swj.shiwujie.HIDE_AI_FLOATING_BALL");
            registerReceiver(floatingBallReceiver, filter);
            
            Log.d(TAG, "广播接收器注册成功");
        } catch (Exception e) {
            Log.e(TAG, "注册广播接收器失败", e);
        }
    }
    
    /**
     * 注销广播接收器
     */
    private void unregisterFloatingBallReceiver() {
        try {
            if (floatingBallReceiver != null) {
                unregisterReceiver(floatingBallReceiver);
                floatingBallReceiver = null;
                Log.d(TAG, "广播接收器注销成功");
            }
        } catch (Exception e) {
            Log.e(TAG, "注销广播接收器失败", e);
        }
    }
}
