package com.swj.shiwujie.data.model;

/**
 * 活动查询请求
 */
public class ActivityQueryRequest {
    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 活动状态
     */
    private String activityStatus;

    /**
     * 活动名称关键词
     */
    private String activityName;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long pageSize;

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(String activityStatus) {
        this.activityStatus = activityStatus;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }
} 