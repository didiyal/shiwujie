package com.swj.shiwujie.data.model;

/**
 * 活动报名请求
 */
public class ActivitySignAddRequest {
    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 视障人士ID 二选一
     */
    private Long blindId;

    /**
     * 志愿者ID 二选一
     */
    private Long volunteerId;

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
} 