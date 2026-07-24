package com.swj.shiwujie.common.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.SocketDataV0;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private static final String WS_URL = "ws://47.112.114.139:8100/api/ws/call";
    
    private static WebSocketManager instance;
    private WebSocketClient webSocketClient;
    private SocketDataV0 socketData;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    
    private MessageListener messageListener;
    
    // 全局消息监听器列表
    private java.util.List<MessageListener> globalMessageListeners = new java.util.ArrayList<>();
    
    // 全局连接状态监听器列表
    private java.util.List<ConnectionStatusListener> globalConnectionStatusListeners = new java.util.ArrayList<>();
    
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
    private static final long RECONNECT_DELAY = 3000; // 3秒（快速重连窗口内）
    // 快速重连用尽后转慢速持续重连，避免弱网下永久静默失活（A2）
    private static final long SLOW_RECONNECT_DELAY = 60_000; // 60秒
    
    // 业务状态相关
    private boolean isInVideoCall = false; // 是否正在进行视频通话
    private boolean isInMatching = false; // 是否正在匹配中
    private long lastBusinessActivityTime = 0; // 最后业务活动时间
    private static final long BUSINESS_ACTIVITY_TIMEOUT = 30000; // 30秒业务活动超时
    
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
                try {
                    // 检查Context是否还有效
                    if (context instanceof android.app.Activity) {
                        android.app.Activity activity = (android.app.Activity) context;
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            Log.w(TAG, "Activity已销毁，跳过Toast显示: " + message);
                            return;
                        }
                    }
                    
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.w(TAG, "显示Toast失败: " + message, e);
                }
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
        isConnecting = true;
        // chunk-2e-5：连 WS 前先经已鉴权 HTTP 换 ticket（堵 phone 冒充）。取到 ticket 再 doConnect 建 WS。
        // 失败（网络/登录态）→ 不建 WS，触发重连自愈（重连会再取 ticket）。
        fetchWsTicket(new TicketCallback() {
            @Override
            public void onTicket(String ticket) {
                doConnect(phone, isVolunteer, ticket);
            }

            @Override
            public void onError(String reason) {
                Log.e(TAG, "WS ticket 获取失败：" + reason + "，触发重连自愈");
                isConnecting = false;
                socketData.updateConnectionStatus(false);
                if (canReconnect()) {
                    scheduleReconnect();
                }
            }
        });
    }

    /**
     * chunk-2e-5：拿到 ticket 后真正建 WS（原 connect 主体）。
     */
    private void doConnect(String phone, boolean isVolunteer, String ticket) {
        try {
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
                    sendLoginMessage(phone, isVolunteer, ticket);
                    
                    // 显示连接成功提示
                   /* showToast("WebSocket连接成功");*/
                    
                    // 通知所有全局连接状态监听器
                    mainHandler.post(() -> {
                        for (ConnectionStatusListener listener : globalConnectionStatusListeners) {
                            try {
                                listener.onConnected();
                            } catch (Exception e) {
                                Log.e(TAG, "通知连接状态监听器失败", e);
                            }
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
                    
                    // 删除Toast提示，避免在页面跳转时显示不必要的提示
                    // 只在日志中记录连接关闭信息
                    Log.d(TAG, "WebSocket连接已关闭，代码: " + code + ", 原因: " + reason);
                    
                    mainHandler.post(() -> {
                        for (ConnectionStatusListener listener : globalConnectionStatusListeners) {
                            try {
                                listener.onDisconnected(code, reason);
                            } catch (Exception e) {
                                Log.e(TAG, "通知连接状态监听器失败", e);
                            }
                        }
                    });
                    
                    // 自动重连：业务中不重连（canReconnect 兜底）；快速重连用尽后转慢速持续重连，不再静默放弃（A2）
                    if (remote && canReconnect()) {
                        scheduleReconnect();
                    } else if (!canReconnect()) {
                        Log.d(TAG, "业务活动进行中，跳过自动重连");
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
                    
                    // 删除Toast提示，避免显示不必要的错误信息
                    // 只在日志中记录连接错误信息
                    Log.w(TAG, "WebSocket连接错误: " + ex.getMessage());
                    
                    mainHandler.post(() -> {
                        for (ConnectionStatusListener listener : globalConnectionStatusListeners) {
                            try {
                                listener.onError(ex);
                            } catch (Exception e) {
                                Log.e(TAG, "通知连接状态监听器失败", e);
                            }
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
     * 发送登录消息（chunk-2e-5：带 ticket，服务端校验后绑 session）
     */
    private void sendLoginMessage(String phone, boolean isVolunteer, String ticket) {
        SocketDataV0 loginData = SocketDataV0.createLoginMessage(phone, isVolunteer, ticket);
        sendMessage(loginData);
    }

    // region chunk-2e-5 WS ticket 鉴权

    /** ticket 获取回调。 */
    private interface TicketCallback {
        void onTicket(String ticket);
        void onError(String reason);
    }

    /**
     * 经已鉴权 HTTP（复用 JWT）换 WS ticket。异步（Retrofit enqueue），回调在主线程由调用方自行处理。
     * <p>登录态缺失 → 直接 onError（不建 WS，触发重连自愈重试）。</p>
     */
    private void fetchWsTicket(TicketCallback cb) {
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            cb.onError("登录态异常（token 为空）");
            return;
        }
        RetrofitClient.getInstance().createService(ApiService.class)
                .fetchWsTicket("Bearer " + token)
                .enqueue(new Callback<BaseResponse<String>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<String>> call, Response<BaseResponse<String>> response) {
                        BaseResponse<String> body = response.body();
                        if (response.isSuccessful() && body != null
                                && body.getCode() == 1 && body.getData() != null && !body.getData().isEmpty()) {
                            Log.d(TAG, "WS ticket 获取成功");
                            cb.onTicket(body.getData());
                        } else {
                            String msg = body != null ? body.getMessage() : ("HTTP " + response.code());
                            cb.onError(msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                        cb.onError(t.getMessage() == null ? "网络异常" : t.getMessage());
                    }
                });
    }

    // endregion
    
    /**
     * 发送消息
     */
    public void sendMessage(SocketDataV0 data) {
        Log.d(TAG, "=== sendMessage方法被调用 ===");
        
        if (data == null) {
            Log.e(TAG, "消息数据为null，无法发送");
            return;
        }
        
        Log.d(TAG, "连接状态检查: isConnected=" + isConnected + ", webSocketClient=" + (webSocketClient != null));
        Log.d(TAG, "准备发送消息 - 连接状态: " + isConnected + ", WebSocket客户端: " + (webSocketClient != null));
        
        if (!isConnected || webSocketClient == null) {
            Log.w(TAG, "WebSocket not connected, cannot send message");
            Log.w(TAG, "isConnected: " + isConnected + ", webSocketClient: " + (webSocketClient != null));
            return;
        }
        
        try {
            // 使用toSendJson方法，只发送后端期望的4个字段
            String jsonMessage = data.toSendJson();
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
            // 不抛出异常，避免调用方崩溃
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

                        // 紧急求助信令分发：type=2（家属接听/匹配成功）取消 60s 求助超时，避免通话中误触发（A1）。
                        // 原本 EmergencyHelpManager.handleSocketMessage 是死代码，求助超时会在通话进行中 fire、误复位。
                        EmergencyHelpManager.getInstance().handleSocketMessage(
                                data.getRequestType(), data.getBlindPhone(),
                                data.getVolunteerPhone(), data.getChannelId());
                        
                        // 处理心跳包响应
                        if (data.getRequestType() == -1) {
                            Log.d(TAG, "收到心跳包响应，连接状态正常");
                        } else {
                            // 更新业务活动时间（非心跳包消息）
                            updateBusinessActivity();
                        }
                        
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

                    // 紧急求助信令分发：type=2 取消 60s 求助超时，避免通话中误触发（A1，对称第二处）
                    EmergencyHelpManager.getInstance().handleSocketMessage(
                            data.getRequestType(), data.getBlindPhone(),
                            data.getVolunteerPhone(), data.getChannelId());
                    
                    // 处理心跳包响应
                    if (data.getRequestType() == -1) {
                        Log.d(TAG, "收到心跳包响应，连接状态正常");
                    } else {
                        // 更新业务活动时间（非心跳包消息）
                        updateBusinessActivity();
                    }
                    
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
        } else if (message.contains("心跳") || message.contains("heartbeat") || message.contains("pong")) {
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
                
                // 通知所有全局连接状态监听器
                mainHandler.post(() -> {
                    for (ConnectionStatusListener listener : globalConnectionStatusListeners) {
                        try {
                            listener.onConnected();
                        } catch (Exception e) {
                            Log.e(TAG, "通知连接状态监听器失败", e);
                        }
                    }
                });
            } else {
                // 连接状态变为断开，尝试自动重连
                Log.d(TAG, "检测到连接断开，尝试自动重连");
                if (!isConnecting && canReconnect()) {
                    scheduleReconnect();
                } else if (!canReconnect()) {
                    Log.d(TAG, "业务活动进行中，跳过连接状态检查重连");
                }
                
                // 通知所有全局连接状态监听器
                mainHandler.post(() -> {
                    for (ConnectionStatusListener listener : globalConnectionStatusListeners) {
                        try {
                            listener.onDisconnected(-1, "连接状态检查失败");
                        } catch (Exception e) {
                            Log.e(TAG, "通知连接状态监听器失败", e);
                        }
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
        // 快速重连窗口（< MAX 次）用 3s，用尽后转 60s 慢速持续重连，不再静默放弃（A2）
        boolean fastMode = reconnectAttempts < MAX_RECONNECT_ATTEMPTS;
        long delay = fastMode ? RECONNECT_DELAY : SLOW_RECONNECT_DELAY;
        Log.d(TAG, "Scheduling reconnect attempt " + reconnectAttempts
                + " (delay=" + delay + "ms, " + (fastMode ? "fast" : "slow") + ")");

        // 进入慢速重连的首次（快速窗口刚用尽）：通知 UI 连接需要关注（如提示网络异常）
        if (!fastMode && reconnectAttempts == MAX_RECONNECT_ATTEMPTS) {
            mainHandler.post(() -> {
                for (ConnectionStatusListener listener : globalConnectionStatusListeners) {
                    try {
                        listener.onReconnectNeeded();
                    } catch (Exception e) {
                        Log.e(TAG, "通知连接状态监听器失败", e);
                    }
                }
            });
        }

        mainHandler.postDelayed(() -> {
            if (!isConnected && !isConnecting && canReconnect()) {
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
                        for (ConnectionStatusListener listener : globalConnectionStatusListeners) {
                            try {
                                listener.onReconnectNeeded();
                            } catch (Exception e) {
                                Log.e(TAG, "通知连接状态监听器失败", e);
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "无法自动重连：context为空");
                    for (ConnectionStatusListener listener : globalConnectionStatusListeners) {
                        try {
                            listener.onReconnectNeeded();
                        } catch (Exception e) {
                            Log.e(TAG, "通知连接状态监听器失败", e);
                        }
                    }
                }
            } else if (!canReconnect()) {
                Log.d(TAG, "业务活动进行中，取消重连");
            }
        }, delay);
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
     * 添加全局连接状态监听器
     */
    public void addConnectionStatusListener(ConnectionStatusListener listener) {
        if (listener != null && !globalConnectionStatusListeners.contains(listener)) {
            globalConnectionStatusListeners.add(listener);
            Log.d(TAG, "添加全局连接状态监听器，当前监听器数量: " + globalConnectionStatusListeners.size());
        }
    }
    
    /**
     * 移除全局连接状态监听器
     */
    public void removeConnectionStatusListener(ConnectionStatusListener listener) {
        if (listener != null) {
            globalConnectionStatusListeners.remove(listener);
            Log.d(TAG, "移除全局连接状态监听器，当前监听器数量: " + globalConnectionStatusListeners.size());
        }
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
     * 设置视频通话状态
     */
    public void setVideoCallStatus(boolean inVideoCall) {
        this.isInVideoCall = inVideoCall;
        if (inVideoCall) {
            lastBusinessActivityTime = System.currentTimeMillis();
        }
        Log.d(TAG, "视频通话状态更新: " + inVideoCall);
    }
    
    /**
     * 设置匹配状态
     */
    public void setMatchingStatus(boolean inMatching) {
        this.isInMatching = inMatching;
        if (inMatching) {
            lastBusinessActivityTime = System.currentTimeMillis();
        }
        Log.d(TAG, "匹配状态更新: " + inMatching);
    }
    
    /**
     * 更新业务活动时间
     */
    public void updateBusinessActivity() {
        lastBusinessActivityTime = System.currentTimeMillis();
        Log.d(TAG, "业务活动时间已更新");
    }
    
    /**
     * 检查是否可以重连
     * 只有在没有重要业务活动时才允许重连
     */
    private boolean canReconnect() {
        // 如果正在进行视频通话，不允许重连
        if (isInVideoCall) {
            Log.d(TAG, "正在进行视频通话，跳过重连");
            return false;
        }
        
        // 如果正在匹配中，不允许重连
        if (isInMatching) {
            Log.d(TAG, "正在匹配中，跳过重连");
            return false;
        }
        
        // 检查业务活动超时
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBusinessActivityTime < BUSINESS_ACTIVITY_TIMEOUT) {
            Log.d(TAG, "业务活动未超时，跳过重连");
            return false;
        }
        
        return true;
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