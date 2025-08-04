package com.swj.shiwujie.model.VO.community.activitysign;

import lombok.Data;
import java.util.Date;

/**
 * 活动报名签到VO
 */
@Data
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
}