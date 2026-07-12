package com.swj.shiwujie.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 紧急求助数据模型
 * 用于管理紧急求助的状态和流程
 */
public class EmergencyHelpData {
    
    @SerializedName("helpId")
    private String helpId; // 求助ID
    
    @SerializedName("blindId")
    private Long blindId; // 求助盲人ID
    
    @SerializedName("blindPhone")
    private String blindPhone; // 求助盲人手机号
    
    @SerializedName("volunteerId")
    private Long volunteerId; // 响应志愿者ID
    
    @SerializedName("volunteerPhone")
    private String volunteerPhone; // 响应志愿者手机号
    
    @SerializedName("status")
    private int status; // 求助状态：0-等待中，1-已响应，2-通话中，3-已结束，4-已取消
    
    @SerializedName("requestTime")
    private long requestTime; // 求助请求时间
    
    @SerializedName("responseTime")
    private long responseTime; // 响应时间
    
    @SerializedName("endTime")
    private long endTime; // 结束时间
    
    @SerializedName("channelId")
    private long channelId; // 视频通话频道ID
    
    @SerializedName("message")
    private String message; // 消息内容
    
    // 构造函数
    public EmergencyHelpData() {}
    
    public EmergencyHelpData(String blindPhone) {
        this.blindPhone = blindPhone;
        this.status = 0; // 等待中
        this.requestTime = System.currentTimeMillis();
    }
    
    // 创建紧急求助请求
    public static EmergencyHelpData createHelpRequest(String blindPhone) {
        EmergencyHelpData data = new EmergencyHelpData();
        data.setBlindPhone(blindPhone);
        data.setStatus(0); // 等待中
        data.setRequestTime(System.currentTimeMillis());
        data.setMessage("紧急求助请求");
        return data;
    }
    
    // 创建通话结束
    public static EmergencyHelpData createCallEnded(String blindPhone, String volunteerPhone) {
        EmergencyHelpData data = new EmergencyHelpData();
        data.setBlindPhone(blindPhone);
        data.setVolunteerPhone(volunteerPhone);
        data.setStatus(3); // 已结束
        data.setEndTime(System.currentTimeMillis());
        data.setMessage("通话已结束");
        return data;
    }
    
    // Getter和Setter方法
    
    public void setHelpId(String helpId) {
        this.helpId = helpId;
    }
    
    public void setBlindId(Long blindId) {
        this.blindId = blindId;
    }
    
    public String getBlindPhone() {
        return blindPhone;
    }
    
    public void setBlindPhone(String blindPhone) {
        this.blindPhone = blindPhone;
    }
    
    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }
    
    public String getVolunteerPhone() {
        return volunteerPhone;
    }
    
    public void setVolunteerPhone(String volunteerPhone) {
        this.volunteerPhone = volunteerPhone;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }
    
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "EmergencyHelpData{" +
                "helpId='" + helpId + '\'' +
                ", blindId=" + blindId +
                ", blindPhone='" + blindPhone + '\'' +
                ", volunteerId=" + volunteerId +
                ", volunteerPhone='" + volunteerPhone + '\'' +
                ", status=" + status +
                ", requestTime=" + requestTime +
                ", responseTime=" + responseTime +
                ", endTime=" + endTime +
                ", channelId=" + channelId +
                ", message='" + message + '\'' +
                '}';
    }
} 