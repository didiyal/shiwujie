package com.swj.shiwujie.model.request.community.activity;

import lombok.Data;
import java.util.Date;

/**
 * 活动更新请求
 */
@Data
public class ActivityUpdateRequest {
    /**
     * 活动ID
     */
    private Long activityId;

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
     * 活动状态 (对应ActivityStatusEnum的字符串描述)
     */
    private String activityStatus;
}