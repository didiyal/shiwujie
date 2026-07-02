package com.swj.shiwujie.model.domain.community;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 活动报名签到表
 * @TableName ActivitySign
 */
@TableName(value ="ActivitySign")
@Data
public class Activitysign implements Serializable {
    /**
     * 活动报名签到ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long signId;

    /**
     * 活动id
     */
    private Long activityId;

    /**
     * 视障人士ID
     */
    private Long blindId;

    /**
     * 志愿者id
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