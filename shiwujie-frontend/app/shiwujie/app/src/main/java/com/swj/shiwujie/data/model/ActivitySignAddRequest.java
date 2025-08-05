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
     * 报名人ID（盲人ID或志愿者ID）
     */
    private Long signUserId;

    /**
     * 报名人类型（blind或volunteer）
     */
    private String signUserType;

    /**
     * 社区ID
     */
    private Long communityId;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getSignUserId() {
        return signUserId;
    }

    public void setSignUserId(Long signUserId) {
        this.signUserId = signUserId;
    }

    public String getSignUserType() {
        return signUserType;
    }

    public void setSignUserType(String signUserType) {
        this.signUserType = signUserType;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }
} 