package com.swj.shiwujie.model.request.community.activitysign;

import lombok.Data;
import java.util.Date;

/**
 * 活动报名请求
 */
@Data
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
     * 志愿者id 二选一
     */
    private Long volunteerId;


}