package com.swj.shiwujie.common.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.swj.shiwujie.data.model.SocketDataV0;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket管理器
 * 负责WebSocket连接管理、消息发送接收、状态监控等
 */
public class WebSocketManager {
    private static final String TAG = "WebSocketManager";
    private static final String WS_URL = "ws://43.139.38.62:8100/api/ws/call";
    
    private static WebSocketManager instance;
    private WebSocketClient webSocketClient;
    private SocketDataV0 socketData;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    
    // 连接状态监听器
    private ConnectionStatusListener connectionStatusListener;
    private MessageListener messageListener;
    
    // 全局消息监听器列表
    private java.util.List<MessageListener> globalMessageListeners = new java.util.ArrayList<>();
    
    // 定时器，用于定期检查连接状态
    private ScheduledExecutorService connectionChecker;
    private Handler mainHandler;
    
    // 上下文，用于显示Toast
    private Context context;
    
    // 视频通话管理器
    private VideoCallManager videoCallManager;
    
    // 重连相关
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY = 3000; // 3秒
    
    private WebSocketManager() {
        mainHandler = new Handler(Looper.getMainLooper());
        socketData = new SocketDataV0();
        videoCallManager = VideoCallManager.getInstance();
        initConnectionChecker();
    }
    
    /**
     * 设置上下文，用于显示Toast
     */
    public void setContext(Context context) {
        this.context = context;
        videoCallManager.setContext(context);
    }
    
    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        if (context != null) {
            mainHandler.post(() -> {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }
    
    /**
     * 初始化连接状态检查器
     */
    private void initConnectionChecker() {
        connectionChecker = Executors.newScheduledThreadPool(1);
        connectionChecker.scheduleAtFixedRate(() -> {
            checkConnectionStatus();
        }, 5, 10, TimeUnit.SECONDS); // 每10秒检查一次连接状态
    }
    
    /**
     * 建立WebSocket连接
     * @param phone 用户手机号
     * @param isVolunteer 是否为志愿者
     */
    public void connect(String phone, boolean isVolunteer) {
        Log.e(TAG, "=== 开始建立WebSocket连接 ===");
        Log.e(TAG, "手机号: " + phone + ", 用户类型: " + (isVolunteer ? "志愿者" : "视障人士"));
        Log.e(TAG, "当前连接状态: isConnecting=" + isConnecting + ", isConnected=" + isConnected);
        Log.e(TAG, "WebSocket URL: " + WS_URL);
        
        if (isConnecting || isConnected) {
            Log.e(TAG, "WebSocket already connecting or connected");
            return;
        }
        
        try {
            isConnecting = true;
            Log.e(TAG, "Connecting to WebSocket: " + WS_URL);
            
            webSocketClient = new WebSocketClient(new URI(WS_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.e(TAG, "=== WebSocket连接成功 ===");
                    Log.e(TAG, "WebSocket connected successfully");
                    Log.e(TAG, "握手数据: " + handshakedata.getHttpStatus() + " " + handshakedata.getHttpStatusMessage());
                    isConnecting = false;
                    isConnected = true;
                    reconnectAttempts = 0;
                    
                    // 更新连接状态
                    socketData.updateConnectionStatus(true);
                    
                    // 发送登录消息
                    sendLoginMessage(phone, isVolunteer);
                    
                    // 显示连接成功提示
                    showToast("WebSocket连接成功");
                    
                    // 通知监听器
                    mainHandler.post(() -> {
                        if (connectionStatusListener != null) {
                            connectionStatusListener.onConnected();
                        }
                    });
                }
                
                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "=== 收到WebSocket消息 ===");
                    Log.d(TAG, "收到WebSocket消息: " + message);
                    Log.d(TAG, "消息长度: " + message.length());
                    handleMessage(message);
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.e(TAG, "=== WebSocket连接关闭 ===");
                    Log.e(TAG, "WebSocket closed: " + code + " - " + reason + " (remote: " + remote + ")");
                    isConnecting = false;
                    isConnected = false;
                    socketData.updateConnectionStatus(false);
                    
                    // 显示连接关闭提示
                    showToast("WebSocket连接已关闭: " + reason);
                    
                    mainHandler.post(() -> {
                        if (connectionStatusListener != null) {
                            connectionStatusListener.onDisconnected(code, reason);
                        }
                    });
                    
                    // 自动重连
                    if (remote && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        scheduleReconnect();
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "=== WebSocket连接错误 ===");
                    Log.e(TAG, "WebSocket error: " + ex.getMessage(), ex);
                    Log.e(TAG, "错误堆栈: ", ex);
                    isConnecting = false;
                    isConnected = false;
                    socketData.updateConnectionStatus(false);
                    
                    // 显示连接错误提示
                    showToast("WebSocket连接失败: " + ex.getMessage());
                    
                    mainHandler.post(() -> {
                        if (connectionStatusListener != null) {
                            connectionStatusListener.onError(ex);
                        }
                    });
                }
            };
            
            Log.d(TAG, "开始连接WebSocket...");
            webSocketClient.connect();
            
        } catch (Exception e) {
            Log.e(TAG, "=== 创建WebSocket连接失败 ===");
            Log.e(TAG, "Failed to create WebSocket connection: " + e.getMessage(), e);
            isConnecting = false;
            socketData.updateConnectionStatus(false);
        }
    }
    
    /**
     * 发送登录消息
     */
    private void sendLoginMessage(String phone, boolean isVolunteer) {
        SocketDataV0 loginData = SocketDataV0.createLoginMessage(phone, isVolunteer);
        sendMessage(loginData);
    }
    
    /**
     * 发送消息
     */
    public void sendMessage(SocketDataV0 data) {
        Log.d(TAG, "=== sendMessage方法被调用 ===");
        Log.d(TAG, "连接状态检查: isConnected=" + isConnected + ", webSocketClient=" + (webSocketClient != null));
        Log.d(TAG, "准备发送消息 - 连接状态: " + isConnected + ", WebSocket客户端: " + (webSocketClient != null));
        
        if (!isConnected || webSocketClient == null) {
            Log.w(TAG, "WebSocket not connected, cannot send message");
            Log.w(TAG, "isConnected: " + isConnected + ", webSocketClient: " + (webSocketClient != null));
            return;
        }
        
        try {
            Gson gson = new Gson();
            String jsonMessage = gson.toJson(data);
            Log.d(TAG, "=== JSON序列化成功 ===");
            Log.d(TAG, "发送消息内容: " + jsonMessage);
            Log.d(TAG, "原始JSON字符串: " + jsonMessage);
            Log.d(TAG, "消息类型: " + data.getRequestType() + 
                  ", 盲人手机号: " + data.getBlindPhone() + 
                  ", 志愿者手机号: " + data.getVolunteerPhone() + 
                  ", 频道ID: " + data.getChannelId());
            
            webSocketClient.send(jsonMessage);
            Log.d(TAG, "=== 消息发送成功 ===");
        } catch (Exception e) {
            Log.e(TAG, "发送消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessage(String message) {
        Log.d(TAG, "开始处理消息: " + message);
        Log.d(TAG, "原始接收到的消息: " + message);
        
        // 检查是否为纯字符串消息（非JSON格式）
        if (isPlainStringMessage(message)) {
            Log.d(TAG, "收到字符串确认消息: " + message);
            handleStringMessage(message);
            return;
        }
        
        try {
            Gson gson = new Gson();
            
            // 首先尝试解析为包装格式
            java.util.Map<String, Object> wrapper = null;
            try {
                // 解析包装格式：{"code":0,"message":"初始化成功","socketData":{...}}
                wrapper = gson.fromJson(message, java.util.Map.class);
                Log.d(TAG, "解析包装格式成功: " + wrapper);
                
                if (wrapper != null && wrapper.containsKey("socketData")) {
                    // 提取socketData字段并解析为SocketDataV0
                    Object socketDataObj = wrapper.get("socketData");
                    String socketDataJson = gson.toJson(socketDataObj);
                    Log.d(TAG, "提取的socketData JSON: " + socketDataJson);
                    
                    SocketDataV0 data = gson.fromJson(socketDataJson, SocketDataV0.class);
                    if (data != null) {
                        Log.d(TAG, "消息解析成功: " + data.toString());
                        Log.d(TAG, "消息详情 - 类型: " + data.getRequestType() + 
                              ", 盲人手机号: " + data.getBlindPhone() + 
                              ", 志愿者手机号: " + data.getVolunteerPhone() + 
                              ", 频道ID: " + data.getChannelId());
                        
                        // 更新Socket数据
                        socketData = data;
                        
                        // 处理视频通话消息
                        Log.d(TAG, "开始处理视频通话消息");
                        videoCallManager.handleWebSocketMessage(data);
                        
                        // 通知消息监听器
                        mainHandler.post(() -> {
                            Log.d(TAG, "通知消息监听器，监听器数量: " + (globalMessageListeners.size() + (messageListener != null ? 1 : 0)));
                            
                            if (messageListener != null) {
                                try {
                                    messageListener.onMessageReceived(data);
                                    Log.d(TAG, "主消息监听器通知成功");
                                } catch (Exception e) {
                                    Log.e(TAG, "主消息监听器通知失败: " + e.getMessage(), e);
                                }
                            }
                            
                            // 通知全局消息监听器
                            for (MessageListener listener : globalMessageListeners) {
                                try {
                                    listener.onMessageReceived(data);
                                    Log.d(TAG, "全局消息监听器通知成功");
                                } catch (Exception e) {
                                    Log.e(TAG, "全局消息监听器通知失败: " + e.getMessage(), e);
                                }
                            }
                        });
                        return; // 成功解析，退出
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "包装格式解析失败: " + e.getMessage());
                wrapper = null;
            }
            
            // 如果包装格式解析失败或没有socketData字段，尝试直接解析为SocketDataV0
            if (wrapper == null || !wrapper.containsKey("socketData")) {
                SocketDataV0 data = gson.fromJson(message, SocketDataV0.class);
                
                if (data != null) {
                    Log.d(TAG, "直接解析成功: " + data.toString());
                    Log.d(TAG, "消息详情 - 类型: " + data.getRequestType() + 
                          ", 盲人手机号: " + data.getBlindPhone() + 
                          ", 志愿者手机号: " + data.getVolunteerPhone() + 
                          ", 频道ID: " + data.getChannelId());
                    
                    // 更新Socket数据
                    socketData = data;
                    
                    // 处理视频通话消息
                    Log.d(TAG, "开始处理视频通话消息");
                    videoCallManager.handleWebSocketMessage(data);
                    
                    // 通知消息监听器
                    mainHandler.post(() -> {
                        Log.d(TAG, "通知消息监听器，监听器数量: " + (globalMessageListeners.size() + (messageListener != null ? 1 : 0)));
                        
                        if (messageListener != null) {
                            try {
                                messageListener.onMessageReceived(data);
                                Log.d(TAG, "主消息监听器通知成功");
                            } catch (Exception e) {
                                Log.e(TAG, "主消息监听器通知失败: " + e.getMessage(), e);
                            }
                        }
                        
                        // 通知全局消息监听器
                        for (MessageListener listener : globalMessageListeners) {
                            try {
                                listener.onMessageReceived(data);
                                Log.d(TAG, "全局消息监听器通知成功");
                            } catch (Exception e) {
                                Log.e(TAG, "全局消息监听器通知失败: " + e.getMessage(), e);
                            }
                        }
                    });
                } else {
                    Log.e(TAG, "消息解析失败，data为null");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON消息处理失败: " + e.getMessage(), e);
            // 如果JSON解析失败，尝试作为字符串消息处理
            Log.d(TAG, "尝试作为字符串消息处理");
            handleStringMessage(message);
        }
    }
    
    /**
     * 检查是否为纯字符串消息（非JSON格式）
     */
    private boolean isPlainStringMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = message.trim();
        
        // 如果消息不是以 { 或 [ 开头，且不是JSON格式，则认为是字符串消息
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return true;
        }
        
        // 尝试解析JSON，如果失败则认为是字符串消息
        try {
            new Gson().fromJson(trimmed, Object.class);
            return false; // 成功解析为JSON
        } catch (Exception e) {
            return true; // 解析失败，认为是字符串消息
        }
    }
    
    /**
     * 处理字符串消息
     */
    private void handleStringMessage(String message) {
        Log.d(TAG, "处理字符串消息: " + message);
        
        // 根据消息内容进行不同的处理
        if (message.contains("连接成功") || message.contains("connected")) {
            Log.d(TAG, "收到连接成功确认消息");
            // 可以在这里处理连接成功的逻辑
        } else if (message.contains("心跳") || message.contains("heartbeat")) {
            Log.d(TAG, "收到心跳确认消息");
            // 可以在这里处理心跳确认的逻辑
        } else if (message.contains("消息已收到") || message.contains("received")) {
            Log.d(TAG, "收到消息确认");
            // 可以在这里处理消息确认的逻辑
        } else {
            Log.d(TAG, "收到未知字符串消息: " + message);
            // 可以在这里处理其他字符串消息
        }
        
        // 通知字符串消息监听器（如果需要的话）
        // 这里可以根据需要添加字符串消息的监听器
    }
    
    /**
     * 检查连接状态
     */
    private void checkConnectionStatus() {
        boolean currentStatus = isConnected && webSocketClient != null && webSocketClient.isOpen();
        boolean previousStatus = socketData.isSocketConnected();
        
        if (currentStatus != previousStatus) {
            socketData.updateConnectionStatus(currentStatus);
            Log.d(TAG, "Connection status updated: " + currentStatus);
            
            if (currentStatus) {
                // 连接状态变为已连接，只更新状态，不发送任何消息
                Log.d(TAG, "连接状态恢复，仅更新状态");
                
                // 通知连接状态变化
                mainHandler.post(() -> {
                    if (connectionStatusListener != null) {
                        connectionStatusListener.onConnected();
                    }
                });
            } else {
                // 连接状态变为断开，尝试自动重连
                Log.d(TAG, "检测到连接断开，尝试自动重连");
                if (!isConnecting && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    scheduleReconnect();
                }
                
                // 通知连接状态变化
                mainHandler.post(() -> {
                    if (connectionStatusListener != null) {
                        connectionStatusListener.onDisconnected(-1, "连接状态检查失败");
                    }
                });
            }
        }
    }
    
    /**
     * 安排重连
     */
    private void scheduleReconnect() {
        reconnectAttempts++;
        Log.d(TAG, "Scheduling reconnect attempt " + reconnectAttempts);
        
        mainHandler.postDelayed(() -> {
            if (!isConnected && !isConnecting) {
                Log.d(TAG, "Attempting to reconnect...");
                // 重新获取用户信息来重连
                if (context != null) {
                    String phone = SharedPrefsUtil.getPhone();
                    boolean isVolunteer = !SharedPrefsUtil.isBlind();
                    if (phone != null && !phone.isEmpty()) {
                        Log.d(TAG, "自动重连 - 手机号: " + phone + ", 用户类型: " + (isVolunteer ? "志愿者" : "视障人士"));
                        connect(phone, isVolunteer);
                    } else {
                        Log.w(TAG, "无法自动重连：用户信息为空");
                        if (connectionStatusListener != null) {
                            connectionStatusListener.onReconnectNeeded();
                        }
                    }
                } else {
                    Log.w(TAG, "无法自动重连：context为空");
                    if (connectionStatusListener != null) {
                        connectionStatusListener.onReconnectNeeded();
                    }
                }
            }
        }, RECONNECT_DELAY);
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        isConnected = false;
        isConnecting = false;
        socketData.updateConnectionStatus(false);
    }
    
    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return isConnected && webSocketClient != null && webSocketClient.isOpen();
    }
    
    /**
     * 获取SocketData
     */
    public SocketDataV0 getSocketData() {
        return socketData;
    }
    
    /**
     * 设置连接状态监听器
     */
    public void setConnectionStatusListener(ConnectionStatusListener listener) {
        this.connectionStatusListener = listener;
    }
    
    /**
     * 设置消息监听器
     */
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }
    
    /**
     * 添加全局消息监听器
     */
    public void addMessageListener(MessageListener listener) {
        if (!globalMessageListeners.contains(listener)) {
            globalMessageListeners.add(listener);
        }
    }
    
    /**
     * 移除全局消息监听器
     */
    public void removeMessageListener(MessageListener listener) {
        globalMessageListeners.remove(listener);
    }
    
    /**
     * 清理资源
     */
    public void destroy() {
        disconnect();
        if (connectionChecker != null) {
            connectionChecker.shutdown();
        }
        videoCallManager.destroy();
        instance = null;
    }
    
    /**
     * 静态方法：建立WebSocket连接
     * @param context 上下文
     * @param phone 用户手机号
     * @param isVolunteer 是否为志愿者
     */
    public static void connectWebSocket(Context context, String phone, boolean isVolunteer) {
        try {
            Log.e(TAG, "=== 静态方法connectWebSocket被调用 ===");
            Log.e(TAG, "开始建立WebSocket连接 - 手机号: " + phone + ", 用户类型: " + (isVolunteer ? "志愿者" : "视障人士"));
            Log.e(TAG, "context: " + (context != null ? "非空" : "空"));
            
            WebSocketManager webSocketManager = getInstance();
            Log.e(TAG, "WebSocketManager实例: " + (webSocketManager != null ? "非空" : "空"));
            
            webSocketManager.setContext(context);
            Log.e(TAG, "context设置完成");
            
            webSocketManager.connect(phone, isVolunteer);
            Log.e(TAG, "connect方法调用完成");
            
        } catch (Exception e) {
            Log.e(TAG, "=== 建立WebSocket连接失败 ===");
            Log.e(TAG, "建立WebSocket连接失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取视频通话管理器
     */
    public VideoCallManager getVideoCallManager() {
        return videoCallManager;
    }
    
    /**
     * 连接状态监听器接口
     */
    public interface ConnectionStatusListener {
        void onConnected();
        void onDisconnected(int code, String reason);
        void onError(Exception e);
        void onReconnectNeeded();
    }
    
    /**
     * 消息监听器接口
     */
    public interface MessageListener {
        void onMessageReceived(SocketDataV0 data);
    }
} 