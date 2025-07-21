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
    
    // 创建志愿者响应
    public static EmergencyHelpData createVolunteerResponse(String volunteerPhone, String blindPhone) {
        EmergencyHelpData data = new EmergencyHelpData();
        data.setVolunteerPhone(volunteerPhone);
        data.setBlindPhone(blindPhone);
        data.setStatus(1); // 已响应
        data.setResponseTime(System.currentTimeMillis());
        data.setMessage("志愿者已响应");
        return data;
    }
    
    // 创建通话开始
    public static EmergencyHelpData createCallStarted(String volunteerPhone, String blindPhone, long channelId) {
        EmergencyHelpData data = new EmergencyHelpData();
        data.setVolunteerPhone(volunteerPhone);
        data.setBlindPhone(blindPhone);
        data.setChannelId(channelId);
        data.setStatus(2); // 通话中
        data.setMessage("通话已开始");
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
    
    // 创建求助取消
    public static EmergencyHelpData createHelpCancelled(String blindPhone) {
        EmergencyHelpData data = new EmergencyHelpData();
        data.setBlindPhone(blindPhone);
        data.setStatus(4); // 已取消
        data.setEndTime(System.currentTimeMillis());
        data.setMessage("求助已取消");
        return data;
    }
    
    // Getter和Setter方法
    public String getHelpId() {
        return helpId;
    }
    
    public void setHelpId(String helpId) {
        this.helpId = helpId;
    }
    
    public Long getBlindId() {
        return blindId;
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
    
    public Long getVolunteerId() {
        return volunteerId;
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
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public long getRequestTime() {
        return requestTime;
    }
    
    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }
    
    public long getResponseTime() {
        return responseTime;
    }
    
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public long getChannelId() {
        return channelId;
    }
    
    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // 状态检查方法
    public boolean isWaiting() {
        return status == 0;
    }
    
    public boolean isResponded() {
        return status == 1;
    }
    
    public boolean isInCall() {
        return status == 2;
    }
    
    public boolean isEnded() {
        return status == 3;
    }
    
    public boolean isCancelled() {
        return status == 4;
    }
    
    // 获取状态描述
    public String getStatusDescription() {
        switch (status) {
            case 0: return "等待中";
            case 1: return "已响应";
            case 2: return "通话中";
            case 3: return "已结束";
            case 4: return "已取消";
            default: return "未知状态";
        }
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