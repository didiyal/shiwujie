package com.swj.shiwujie.common.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.EmergencyHelpManager;

/**
 * 紧急求助悬浮窗
 * 显示等待时间并提供取消功能
 */
public class EmergencyHelpFloatingWindow {
    private static final String TAG = "EmergencyHelpFloatingWindow";
    
    private Context context;
    private WindowManager windowManager;
    private View floatingView;
    private TextView tvStatus;
    private TextView tvWaitTime;
    private Button btnCancel;
    
    private Handler handler;
    private Runnable timeUpdateRunnable;
    private long startTime;
    private boolean isShowing = false;
    
    private EmergencyHelpManager emergencyHelpManager;
    
    public EmergencyHelpFloatingWindow(Context context) {
        Log.d(TAG, "创建紧急求助悬浮窗，context: " + context.getClass().getSimpleName());
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        this.emergencyHelpManager = EmergencyHelpManager.getInstance();
        
        Log.d(TAG, "windowManager: " + (windowManager != null ? "成功获取" : "获取失败"));
        
        initView();
        initTimeUpdate();
        Log.d(TAG, "紧急求助悬浮窗创建完成");
    }
    
    /**
     * 初始化视图
     */
    private void initView() {
        floatingView = LayoutInflater.from(context).inflate(R.layout.emergency_help_floating_window, null);
        
        tvStatus = floatingView.findViewById(R.id.tvStatus);
        tvWaitTime = floatingView.findViewById(R.id.tvWaitTime);
        btnCancel = floatingView.findViewById(R.id.btnCancel);
        
        // 设置取消按钮点击事件
        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "用户点击取消求助按钮");
            cancelEmergencyHelp();
        });
    }
    
    /**
     * 初始化时间更新
     */
    private void initTimeUpdate() {
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isShowing) {
                    updateWaitTime();
                    handler.postDelayed(this, 1000); // 每秒更新一次
                }
            }
        };
    }
    
    /**
     * 显示悬浮窗
     */
    public void show() {
        if (isShowing) {
            Log.w(TAG, "悬浮窗已显示，忽略重复显示请求");
            return;
        }
        
        try {
            Log.d(TAG, "显示紧急求助悬浮窗");
            
            // 设置窗口参数
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    android.graphics.PixelFormat.TRANSLUCENT
            );
            
            // 设置位置（顶部居中）
            params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
            params.y = 100; // 距离顶部100像素
            
            // 添加到窗口
            windowManager.addView(floatingView, params);
            isShowing = true;
            
            // 记录开始时间
            startTime = System.currentTimeMillis();
            
            // 开始时间更新
            handler.post(timeUpdateRunnable);
            
            Log.d(TAG, "紧急求助悬浮窗显示成功");
            
        } catch (Exception e) {
            Log.e(TAG, "显示紧急求助悬浮窗失败", e);
            Log.e(TAG, "错误详情: " + e.getMessage());
            e.printStackTrace();
            
            // 尝试使用不同的窗口类型
            try {
                Log.d(TAG, "尝试使用TYPE_APPLICATION_PANEL窗口类型");
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        android.graphics.PixelFormat.TRANSLUCENT
                );
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                params.y = 100;
                
                windowManager.addView(floatingView, params);
                isShowing = true;
                startTime = System.currentTimeMillis();
                handler.post(timeUpdateRunnable);
                
                Log.d(TAG, "使用TYPE_APPLICATION_PANEL显示悬浮窗成功");
                
            } catch (Exception e2) {
                Log.e(TAG, "使用TYPE_APPLICATION_PANEL也失败", e2);
                // Toast.makeText(context, "显示悬浮窗失败: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 隐藏悬浮窗
     */
    public void hide() {
        if (!isShowing) {
            Log.w(TAG, "悬浮窗未显示，忽略隐藏请求");
            return;
        }
        
        try {
            Log.d(TAG, "隐藏紧急求助悬浮窗");
            
            // 停止时间更新
            handler.removeCallbacks(timeUpdateRunnable);
            
            // 从窗口移除
            windowManager.removeView(floatingView);
            isShowing = false;
            
            Log.d(TAG, "紧急求助悬浮窗隐藏成功");
            
        } catch (Exception e) {
            Log.e(TAG, "隐藏紧急求助悬浮窗失败", e);
        }
    }
    
    /**
     * 更新等待时间显示
     */
    private void updateWaitTime() {
        if (!isShowing) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        
        // 转换为分:秒格式
        long minutes = elapsedTime / (1000 * 60);
        long seconds = (elapsedTime / 1000) % 60;
        
        String timeText = String.format("等待时间: %02d:%02d", minutes, seconds);
        tvWaitTime.setText(timeText);
    }
    
    /**
     * 更新状态显示
     */
    public void updateStatus(String status) {
        if (isShowing && tvStatus != null) {
            tvStatus.setText(status);
        }
    }
    
    /**
     * 取消紧急求助
     */
    private void cancelEmergencyHelp() {
        Log.d(TAG, "开始取消紧急求助");
        
        // 显示取消中状态
        updateStatus("正在取消求助...");
        btnCancel.setEnabled(false);
        btnCancel.setText("取消中...");
        
        // 直接调用取消求助，不覆盖回调
        emergencyHelpManager.cancelEmergencyHelp();
        
        // 延迟隐藏悬浮窗，让HomeFragment的回调处理
        handler.postDelayed(() -> {
            if (isShowing) {
                Log.d(TAG, "延迟隐藏悬浮窗");
                hide();
            }
        }, 1000);
    }
    
    /**
     * 设置响应模式（志愿者端）
     */
    public void setRespondMode(Runnable onRespond) {
        if (btnCancel != null) {
            btnCancel.setText("响应");
            btnCancel.setEnabled(true);
            btnCancel.setOnClickListener(v -> {
                if (onRespond != null) onRespond.run();
            });
        }
    }
    
    /**
     * 检查是否正在显示
     */
    public boolean isShowing() {
        return isShowing;
    }
    
    /**
     * 销毁悬浮窗
     */
    public void destroy() {
        Log.d(TAG, "销毁紧急求助悬浮窗");
        
        if (isShowing) {
            hide();
        }
        
        // 清理资源
        handler.removeCallbacks(timeUpdateRunnable);
        context = null;
        windowManager = null;
        floatingView = null;
        tvStatus = null;
        tvWaitTime = null;
        btnCancel = null;
        emergencyHelpManager = null;
    }
} 