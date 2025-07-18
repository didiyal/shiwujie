package com.swj.shiwujie.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Socket数据模型
 * 用于管理WebSocket连接状态和消息数据
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
    
    // 创建登录消息的静态方法
    public static SocketDataV0 createLoginMessage(String phone, boolean isVolunteer) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(0);
        if (isVolunteer) {
            data.setVolunteerPhone(phone);
            data.setBlindPhone(null);
        } else {
            data.setBlindPhone(phone);
            data.setVolunteerPhone(null);
        }
        data.setChannelId(0);
        return data;
    }
    
    // 创建视频初始化成功消息
    public static SocketDataV0 createVideoInitMessage(String phone, boolean isVolunteer, long channelId) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(2);
        if (isVolunteer) {
            data.setVolunteerPhone(phone);
            data.setBlindPhone(null);
        } else {
            data.setBlindPhone(phone);
            data.setVolunteerPhone(null);
        }
        data.setChannelId(channelId);
        return data;
    }
    
    // 创建视频求助请求消息
    public static SocketDataV0 createVideoHelpRequest(String blindPhone) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(3);
        data.setBlindPhone(blindPhone);
        data.setVolunteerPhone(null);
        data.setCallStatus(1); // 等待匹配
        data.setTimestamp(System.currentTimeMillis());
        data.setMessage("视频求助请求");
        return data;
    }
    
    // 创建志愿者接听消息
    public static SocketDataV0 createVolunteerAccept(String volunteerPhone, String blindPhone, String callId) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(4);
        data.setVolunteerPhone(volunteerPhone);
        data.setBlindPhone(blindPhone);
        data.setCallId(callId);
        data.setCallStatus(2); // 通话中
        data.setTimestamp(System.currentTimeMillis());
        data.setMessage("志愿者已接听");
        return data;
    }
    
    // 创建通话结束消息
    public static SocketDataV0 createCallEnd(String callId, String phone, boolean isVolunteer) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(5);
        data.setCallId(callId);
        data.setCallStatus(3); // 通话结束
        data.setTimestamp(System.currentTimeMillis());
        data.setMessage("通话已结束");
        if (isVolunteer) {
            data.setVolunteerPhone(phone);
            data.setBlindPhone(null);
        } else {
            data.setBlindPhone(phone);
            data.setVolunteerPhone(null);
        }
        return data;
    }
    
    // 创建匹配成功消息
    public static SocketDataV0 createMatchSuccess(String blindPhone, String volunteerPhone, String callId) {
        SocketDataV0 data = new SocketDataV0();
        data.setRequestType(6);
        data.setBlindPhone(blindPhone);
        data.setVolunteerPhone(volunteerPhone);
        data.setCallId(callId);
        data.setCallStatus(2); // 通话中
        data.setTimestamp(System.currentTimeMillis());
        data.setMessage("匹配成功，开始通话");
        return data;
    }
    
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
                ", isSocketConnected=" + isSocketConnected +
                ", callStatus=" + callStatus +
                ", callId='" + callId + '\'' +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                '}';
    }
} 