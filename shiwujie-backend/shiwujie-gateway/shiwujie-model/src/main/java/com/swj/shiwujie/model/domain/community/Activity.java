package com.swj.shiwujie.model.domain.community;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 活动信息表
 * @TableName Activity
 */
@TableName(value ="Activity")
@Data
public class Activity implements Serializable {
    /**
     * 活动ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long activityId;

    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 社区管理人员ID
     */
    private Long managerId;

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
     * 活动状态 0-未开始 1-进行中 2-已结束 3-已取消
     */
    private Integer activityStatus;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 信息更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除 0-存在 1-删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}