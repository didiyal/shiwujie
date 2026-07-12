package com.swj.shiwujie.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Socket数据模型
 * 用于管理WebSocket连接状态和消息数据
 *
 * requestType 真值表（前端视角，与 问题.md 协议表对齐；正式 product 契约码表待前后端联调落档）：
 *   -1  心跳                收/发   createHeartbeatMessage（客户端发）/ WebSocketService 心跳
 *    0  Socket 登录          收/发   createLoginMessage（客户端发）/ VideoCallManager.handleSocketLogin
 *    1  匹配成功通知          收     VideoCallManager.handleMatchSuccess；createMatchSuccess 死代码
 *    2  视频初始化成功        收     VideoCallManager.handleVideoInitSuccess；并触发 EmergencyHelpManager
 *                                    取消求助超时（A1）；createVideoInitMessage 死代码
 *    3  紧急求助来电通知      收     volunteer/HomeFragment 弹接听框；createVideoHelpRequest 死代码
 *    4  紧急求助取消通知      收     volunteer/HomeFragment 撤回弹框；createVolunteerAccept 死代码（命名误导）
 *    5  通话结束通知          收     blind/HomeFragment；createCallEnd 死代码
 *   5001~5006  AI/页面跳转类通知     收（见下方常量）
 * 注：3/4/5 为紧急求助通知（接收型，客户端不发），不在 VideoCallManager 职责内（见 A6 结论）。
 */
public class SocketDataV0 {
    
    @SerializedName("requestType")
    private int requestType;
    
    @SerializedName("blindPhone")
    private String blindPhone;
    
    @SerializedName("volunteerPhone")
    private String volunteerPhone;
    
    @SerializedName("channelId")
    private long channelId;
    
    @SerializedName("blindId")
    private Long blindId;
    
    // 新增字段：管理用户socket连接状况
    private boolean isSocketConnected = false;
    
    // 视频通话相关字段
    @SerializedName("callStatus")
    private int callStatus; // 0:空闲 1:等待匹配 2:通话中 3:通话结束
    
    @SerializedName("callId")
    private String callId; // 通话ID
    
    @SerializedName("timestamp")
    private long timestamp; // 时间戳
    
    @SerializedName("message")
    private String message; // 消息内容
    
    // 构造函数
    public SocketDataV0() {}
    
    public SocketDataV0(int requestType, String blindPhone, String volunteerPhone, long channelId) {
        this.requestType = requestType;
        this.blindPhone = blindPhone;
        this.volunteerPhone = volunteerPhone;
        this.channelId = channelId;
    }
    
    // 创建登录消息的静态方法（requestType=0，客户端发送）
    public static SocketDataV0 createLoginMessage(String phone, boolean isVolunteer) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(REQUEST_TYPE_LOGIN);
        if (isVolunteer) {
            data.setVolunteerPhone(phone);
            data.setBlindPhone("");
        } else {
            data.setBlindPhone(phone);
            data.setVolunteerPhone("");
        }
        data.setChannelId(0);
        return data;
    }
    
    /**
     * 创建视频初始化成功消息（requestType=2，服务端下发；客户端不发）。
     * @deprecated 零调用点（死代码）。type=2 实际由服务端下发、VideoCallManager.handleVideoInitSuccess 接收，
     *             并触发 EmergencyHelpManager 取消求助超时。保留待批次 D 统一清理。
     */
    @Deprecated
    public static SocketDataV0 createVideoInitMessage(String phone, boolean isVolunteer, long channelId) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(REQUEST_TYPE_VIDEO_INIT);
        if (isVolunteer) {
            data.setVolunteerPhone(phone);
            data.setBlindPhone("");
        } else {
            data.setBlindPhone(phone);
            data.setVolunteerPhone("");
        }
        data.setChannelId(channelId);
        return data;
    }
    
    /**
     * 创建视频求助请求消息（requestType=3，服务端下发的「紧急求助来电」通知；客户端不发）。
     * @deprecated 零调用点（死代码）。type=3 实际由 volunteer/HomeFragment 接收并弹接听框。
     *             保留待批次 D 统一清理。
     */
    @Deprecated
    public static SocketDataV0 createVideoHelpRequest(String blindPhone) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(REQUEST_TYPE_EMERGENCY_INCOMING);
        data.setBlindPhone(blindPhone);
        data.setVolunteerPhone("");
        data.setChannelId(0);
        return data;
    }
    
    /**
     * createVolunteerAccept（requestType=4，服务端下发的「紧急求助取消」通知；客户端不发）。
     * 命名误导：名为"接听"实为"取消"。type=4 实际由 volunteer/HomeFragment 接收并撤回接听弹框。
     * @deprecated 零调用点（死代码）。保留待批次 D 统一清理。
     */
    @Deprecated
    public static SocketDataV0 createVolunteerAccept(String volunteerPhone, String blindPhone, String callId) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(REQUEST_TYPE_EMERGENCY_CANCELLED);
        data.setVolunteerPhone(volunteerPhone);
        data.setBlindPhone(blindPhone);
        data.setChannelId(0);
        return data;
    }
    
    /**
     * 创建通话结束消息（requestType=5，服务端下发的通话结束通知；客户端不发）。
     * @deprecated 零调用点（死代码）。type=5 实际由 blind/HomeFragment 接收。保留待批次 D 统一清理。
     */
    @Deprecated
    public static SocketDataV0 createCallEnd(String callId, String phone, boolean isVolunteer) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(REQUEST_TYPE_CALL_END);
        if (isVolunteer) {
            data.setVolunteerPhone(phone);
            data.setBlindPhone("");
        } else {
            data.setBlindPhone(phone);
            data.setVolunteerPhone("");
        }
        data.setChannelId(0);
        return data;
    }
    
    /**
     * 创建匹配成功消息（requestType=1，服务端下发；客户端不发）。
     * @deprecated 零调用点（死代码）。type=1 实际由 VideoCallManager.handleMatchSuccess 接收。
     *             保留待批次 D 统一清理。
     */
    @Deprecated
    public static SocketDataV0 createMatchSuccess(String blindPhone, String volunteerPhone, String callId) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(REQUEST_TYPE_MATCH_SUCCESS);
        data.setBlindPhone(blindPhone);
        data.setVolunteerPhone(volunteerPhone);
        data.setChannelId(0);
        return data;
    }
    
    // 创建心跳包消息（requestType=-1，客户端发送）
    public static SocketDataV0 createHeartbeatMessage(String phone, boolean isVolunteer) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(REQUEST_TYPE_HEARTBEAT);
        if (isVolunteer) {
            data.setVolunteerPhone(phone);
            data.setBlindPhone("");
        } else {
            data.setBlindPhone(phone);
            data.setVolunteerPhone("");
        }
        data.setChannelId(0);
        return data;
    }
    
    // ===== 核心信令码常量（requestType 真值，命名按实际语义；见类头真值表）=====
    public static final int REQUEST_TYPE_HEARTBEAT = -1;          // 心跳（收/发）
    public static final int REQUEST_TYPE_LOGIN = 0;               // Socket 登录（收/发）
    public static final int REQUEST_TYPE_MATCH_SUCCESS = 1;       // 匹配成功通知（收）
    public static final int REQUEST_TYPE_VIDEO_INIT = 2;          // 视频初始化成功（收；兼触发取消求助超时）
    public static final int REQUEST_TYPE_EMERGENCY_INCOMING = 3;  // 紧急求助来电通知（收）
    public static final int REQUEST_TYPE_EMERGENCY_CANCELLED = 4; // 紧急求助取消通知（收）
    public static final int REQUEST_TYPE_CALL_END = 5;            // 通话结束通知（收）

    // AI/页面跳转类通知码常量定义
    public static final int REQUEST_TYPE_AI_PHOTO_RECOGNITION = 5001; // 启动AI页面的拍照识别按钮和功能
    public static final int REQUEST_TYPE_JUMP_TO_BLINDHOME = 5002;    // 跳转到blindhome页面并开启连线志愿者按钮和功能
    public static final int REQUEST_TYPE_EMERGENCY_HELP = 5003;       // 紧急求助，跳转到blindhome页面并开启紧急求助功能
    public static final int REQUEST_TYPE_APP_JUMP = 5004;             // APP跳转
    public static final int REQUEST_TYPE_EDIT_PROFILE = 5005;         // 跳转到用户信息修改页面
    public static final int REQUEST_TYPE_NAVIGATION_REQUEST = 5006;   // 导航请求
    
    // Getter和Setter方法
    public int getRequestType() {
        return requestType;
    }
    
    public void setRequestType(int requestType) {
        this.requestType = requestType;
    }
    
    public String getBlindPhone() {
        return blindPhone;
    }
    
    public void setBlindPhone(String blindPhone) {
        this.blindPhone = blindPhone;
    }
    
    public String getVolunteerPhone() {
        return volunteerPhone;
    }
    
    public void setVolunteerPhone(String volunteerPhone) {
        this.volunteerPhone = volunteerPhone;
    }
    
    public long getChannelId() {
        return channelId;
    }
    
    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }
    
    public boolean isSocketConnected() {
        return isSocketConnected;
    }
    
    public void setSocketConnected(boolean socketConnected) {
        isSocketConnected = socketConnected;
    }
    
    public Long getBlindId() {
        return blindId;
    }
    public void setBlindId(Long blindId) {
        this.blindId = blindId;
    }
    
    // 视频通话相关字段的getter和setter
    public int getCallStatus() {
        return callStatus;
    }
    
    public void setCallStatus(int callStatus) {
        this.callStatus = callStatus;
    }
    
    public String getCallId() {
        return callId;
    }
    
    public void setCallId(String callId) {
        this.callId = callId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * 获取消息内容（兼容性方法，与message字段相同）
     */
    public String getContent() {
        return message;
    }
    
    /**
     * 设置消息内容（兼容性方法，与message字段相同）
     */
    public void setContent(String content) {
        this.message = content;
    }
    
    // 更新连接状态的方法
    public void updateConnectionStatus(boolean connected) {
        this.isSocketConnected = connected;
    }
    
    // 检查连接状态的方法
    public boolean checkConnectionStatus() {
        return isSocketConnected;
    }
    
    @Override
    public String toString() {
        return "SocketDataV0{" +
                "requestType=" + requestType +
                ", blindPhone='" + blindPhone + '\'' +
                ", volunteerPhone='" + volunteerPhone + '\'' +
                ", channelId=" + channelId +
                ", blindId=" + blindId +
                ", isSocketConnected=" + isSocketConnected +
                ", callStatus=" + callStatus +
                ", callId='" + callId + '\'' +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                '}';
    }
    
    /**
     * 转换为发送给后端的JSON格式
     * 只包含后端期望的4个字段
     */
    public String toSendJson() {
        java.util.Map<String, Object> sendData = new java.util.HashMap<>();
        sendData.put("requestType", requestType);
        sendData.put("blindPhone", blindPhone != null ? blindPhone : "");
        sendData.put("volunteerPhone", volunteerPhone != null ? volunteerPhone : "");
        sendData.put("channelId", channelId);
        return new com.google.gson.Gson().toJson(sendData);
    }
    
    /**
     * 获取额外数据（用于临时存储远程用户ID等）
     */
    public String getExtraData() {
        // 这里可以根据需要返回额外的数据
        // 目前返回null，可以根据实际需求修改
        return null;
    }
} 