package com.swj.shiwujie.model.VO.community.activity;


import com.swj.shiwujie.model.enums.community.ActivityStatusEnum;
import lombok.Data;
import java.util.Date;

/**
 * 活动VO
 */
@Data
public class ActivityVO {
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
     * 活动状态 0-未开始 1-进行中 2-已结束 3-已取消
     */
    private String activityStatus;

    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 设置活动状态名称
     */
    public void setActivityStatus(Integer activityStatus) {
        this.activityStatus = ActivityStatusEnum.getById(activityStatus).getName();
    }
}