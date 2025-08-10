package com.swj.shiwujie.common.network;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.volunteer.VolunteerHomeActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket前台服务
 * 负责维护WebSocket长连接和心跳包
 */
public class WebSocketService extends Service {
    private static final String TAG = "WebSocketService";
    private static final String CHANNEL_ID = "WebSocketServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private WebSocketManager webSocketManager;
    private ScheduledExecutorService heartbeatExecutor;
    private boolean isHeartbeatStarted = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WebSocketService onCreate");
        
        try {
            // 尝试启动前台服务
            boolean foregroundStarted = false;
            try {
                startForeground(NOTIFICATION_ID, createNotification());
                Log.d(TAG, "前台服务启动成功");
                foregroundStarted = true;
            } catch (Exception e) {
                Log.e(TAG, "前台服务启动失败，将作为普通服务运行", e);
            }
            
            // 初始化WebSocketManager
            webSocketManager = WebSocketManager.getInstance();
            if (webSocketManager == null) {
                Log.e(TAG, "WebSocketManager初始化失败");
                stopSelf();
                return;
            }
            webSocketManager.setContext(this);
            
            // 创建通知渠道（仅在前台服务模式下）
            if (foregroundStarted) {
                createNotificationChannel();
            }
            
            // 初始化心跳包
            initHeartbeat();
            
            // 建立WebSocket连接
            connectWebSocket();
            
        } catch (Exception e) {
            Log.e(TAG, "WebSocketService初始化失败", e);
            // 如果初始化失败，停止服务
            stopSelf();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WebSocketService onStartCommand");
        return START_STICKY; // 服务被杀死后自动重启
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "WebSocketService onDestroy");
        
        // 停止心跳包
        stopHeartbeat();
        
        // 清理WebSocket连接
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
        
        super.onDestroy();
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WebSocket连接服务",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("保持WebSocket连接活跃");
                channel.setShowBadge(false);
                channel.setSound(null, null);
                channel.enableLights(false);
                channel.enableVibration(false);
                
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    Log.d(TAG, "通知渠道创建成功");
                } else {
                    Log.w(TAG, "NotificationManager为null");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "创建通知渠道失败", e);
        }
    }
    
    /**
     * 创建通知
     */
    private Notification createNotification() {
        try {
            Intent notificationIntent = new Intent(this, VolunteerHomeActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE
            );
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("视无界")
                .setContentText("保持连接中...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
            
            // 根据Android版本设置不同的标志
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID);
            }
            
            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "创建通知失败", e);
            // 创建一个简单的通知作为后备
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("视无界")
                .setContentText("保持连接中...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
        }
    }
    
    /**
     * 初始化心跳包
     */
    private void initHeartbeat() {
        if (isHeartbeatStarted) {
            return;
        }
        
        heartbeatExecutor = Executors.newScheduledThreadPool(1);

        heartbeatExecutor.scheduleAtFixedRate(() -> {
            sendHeartbeat();
        }, 2, 2, TimeUnit.HOURS); // 每30秒发送一次心跳包
        
        isHeartbeatStarted = true;
        Log.d(TAG, "心跳包已启动，间隔30秒");
    }
    
    /**
     * 停止心跳包
     */
    private void stopHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
            heartbeatExecutor = null;
        }
        isHeartbeatStarted = false;
        Log.d(TAG, "心跳包已停止");
    }
    
    /**
     * 发送心跳包
     */
    private void sendHeartbeat() {
        try {
            if (webSocketManager == null) {
                Log.w(TAG, "WebSocketManager为null，跳过心跳包发送");
                return;
            }
            
            if (!webSocketManager.isConnected()) {
                Log.d(TAG, "WebSocket未连接，跳过心跳包发送");
                return;
            }
            
            String phone = SharedPrefsUtil.getPhone();
            if (phone == null || phone.isEmpty()) {
                Log.w(TAG, "手机号为空，跳过心跳包发送");
                return;
            }
            
            boolean isVolunteer = !SharedPrefsUtil.isBlind();
            
            // 创建心跳包消息
            com.swj.shiwujie.data.model.SocketDataV0 heartbeatData = 
                com.swj.shiwujie.data.model.SocketDataV0.createHeartbeatMessage(phone, isVolunteer);
            
            if (heartbeatData == null) {
                Log.e(TAG, "创建心跳包消息失败");
                return;
            }
            
            Log.d(TAG, "发送心跳包: " + heartbeatData.toString());
            webSocketManager.sendMessage(heartbeatData);
            
        } catch (Exception e) {
            Log.e(TAG, "发送心跳包失败", e);
            // 不抛出异常，避免服务崩溃
        }
    }
    
    /**
     * 建立WebSocket连接
     */
    private void connectWebSocket() {
        try {
            if (SharedPrefsUtil.isLoggedIn()) {
                String phone = SharedPrefsUtil.getPhone();
                boolean isVolunteer = !SharedPrefsUtil.isBlind();
                
                if (phone != null && !phone.isEmpty()) {
                    // 检查WebSocket连接状态，避免重复连接
                    if (!webSocketManager.isConnected()) {
                        Log.d(TAG, "用户已登录，建立WebSocket连接 - 手机号: " + phone);
                        webSocketManager.connect(phone, isVolunteer);
                    } else {
                        Log.d(TAG, "WebSocket已连接，跳过重复连接");
                    }
                } else {
                    Log.w(TAG, "用户已登录但手机号为空");
                }
            } else {
                Log.d(TAG, "用户未登录，跳过WebSocket连接");
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化WebSocket连接失败", e);
        }
    }
    
    /**
     * 启动WebSocket服务
     */
    public static void startService(Context context) {
        try {
            if (context == null) {
                Log.e(TAG, "Context为null，无法启动服务");
                return;
            }
            
            Intent intent = new Intent(context, WebSocketService.class);
            
            // 检查是否有前台服务权限
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent);
                    Log.d(TAG, "前台服务启动请求已发送");
                } catch (SecurityException e) {
                    Log.e(TAG, "前台服务权限被拒绝，尝试普通服务", e);
                    startServiceAsNormal(context);
                }
            } else {
                context.startService(intent);
                Log.d(TAG, "普通服务启动请求已发送");
            }
        } catch (Exception e) {
            Log.e(TAG, "启动WebSocketService失败", e);
            // 如果前台服务启动失败，尝试普通服务
            startServiceAsNormal(context);
        }
    }
    
    /**
     * 作为普通服务启动
     */
    private static void startServiceAsNormal(Context context) {
        try {
            Intent intent = new Intent(context, WebSocketService.class);
            context.startService(intent);
            Log.d(TAG, "作为普通服务启动成功");
        } catch (Exception e) {
            Log.e(TAG, "普通服务启动也失败", e);
        }
    }
    
    /**
     * 停止WebSocket服务
     */
    public static void stopService(Context context) {
        Intent intent = new Intent(context, WebSocketService.class);
        context.stopService(intent);
    }
}
