package com.swj.shiwujie.data.model;

import java.util.Date;

/**
 * 活动报名签到VO
 */
public class ActivitysignVO {
    /**
     * 活动报名签到ID
     */
    private Long signId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 视障人士ID
     */
    private Long blindId;

    /**
     * 志愿者ID
     */
    private Long volunteerId;

    /**
     * 活动报名时间
     */
    private Date signUpTime;

    /**
     * 活动签到时间
     */
    private Date checkInTime;

    /**
     * 活动签到地点
     */
    private String checkInLocation;

    /**
     * 活动签退时间
     */
    private Date checkOutTime;

    // Getters and Setters
    public Long getSignId() {
        return signId;
    }

    public void setSignId(Long signId) {
        this.signId = signId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getBlindId() {
        return blindId;
    }

    public void setBlindId(Long blindId) {
        this.blindId = blindId;
    }

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }

    public Date getSignUpTime() {
        return signUpTime;
    }

    public void setSignUpTime(Date signUpTime) {
        this.signUpTime = signUpTime;
    }

    public Date getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Date checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getCheckInLocation() {
        return checkInLocation;
    }

    public void setCheckInLocation(String checkInLocation) {
        this.checkInLocation = checkInLocation;
    }

    public Date getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Date checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}