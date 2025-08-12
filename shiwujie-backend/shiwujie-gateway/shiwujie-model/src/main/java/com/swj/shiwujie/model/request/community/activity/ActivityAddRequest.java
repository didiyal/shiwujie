package com.swj.shiwujie.model.request.community.activity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 活动创建请求
 */
@Data
public class ActivityAddRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 活动名字
     */
    private String activityName;

    /**
     * 活动内容
     */
    private String activityContent;

    /**
     * 活动地点
     */
    private String activityLocation;

    /**
     * 活动限定人数
     */
    private Long maxParticipants;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 社区ID
     */
    private Long communityId;
}