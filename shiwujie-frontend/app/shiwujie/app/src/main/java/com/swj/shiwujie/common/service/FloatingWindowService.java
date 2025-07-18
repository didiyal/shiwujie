package com.swj.shiwujie.common.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BaseResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 系统级悬浮窗服务
 * 用于显示志愿者等待匹配的状态和动态等待时间
 */
public class FloatingWindowService extends Service {
    private static final String TAG = "FloatingWindowService";
    
    private WindowManager windowManager;
    private View floatingView;
    private TextView tvWaitingTime;
    private TextView tvStatus;
    
    private Handler handler;
    private Runnable timeRunnable;
    private int waitingSeconds = 0;
    
    private boolean isWaiting = true;
    
    // 悬浮窗参数
    private WindowManager.LayoutParams params;
    
    @Override
    public void onCreate() {
        super.onCreate();
        initFloatingWindow();
        startTimer();
    }
    
    private void initFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_waiting_window, null);
        
        tvWaitingTime = floatingView.findViewById(R.id.tv_waiting_time);
        tvStatus = floatingView.findViewById(R.id.tv_status);
        
        // 设置悬浮窗参数
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 200;
        
        // 添加触摸事件
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long lastClickTime = 0;
            
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
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastClickTime < 200) {
                            // 点击事件 - 取消匹配
                            onFloatingWindowClick();
                        }
                        return true;
                }
                return false;
            }
        });
        
        // 添加悬浮窗到屏幕
        windowManager.addView(floatingView, params);
    }
    
    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }
    
    private void startTimer() {
        handler = new Handler(Looper.getMainLooper());
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                if (isWaiting) {
                    waitingSeconds++;
                    updateWaitingTime();
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(timeRunnable);
    }
    
    private void updateWaitingTime() {
        int minutes = waitingSeconds / 60;
        int seconds = waitingSeconds % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);
        tvWaitingTime.setText(timeText);
    }
    
    private void onFloatingWindowClick() {
        // 用户点击悬浮窗，取消匹配
        cancelMatching();
    }
    
    private void cancelMatching() {
        if (!isWaiting) return;
        
        isWaiting = false;
        tvStatus.setText("正在取消...");
        
        // 立即停止定时器
        if (handler != null && timeRunnable != null) {
            handler.removeCallbacks(timeRunnable);
        }
        
        // 发送取消匹配请求
        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        String token = "Bearer " + SharedPrefsUtil.getToken();
        
        apiService.volunteerLeaveVideohelp(token).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Boolean> result = response.body();
                    if (result.getCode() == 1 && Boolean.TRUE.equals(result.getData())) {
                        Toast.makeText(FloatingWindowService.this, "已取消匹配", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FloatingWindowService.this, "取消匹配失败: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FloatingWindowService.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                }
                // 无论成功失败都要停止服务
                stopSelf();
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                Toast.makeText(FloatingWindowService.this, "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // 网络失败也要停止服务
                stopSelf();
            }
        });
    }
    
    public void stopWaiting() {
        isWaiting = false;
        if (handler != null && timeRunnable != null) {
            handler.removeCallbacks(timeRunnable);
        }
        stopSelf();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        if (handler != null && timeRunnable != null) {
            handler.removeCallbacks(timeRunnable);
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 