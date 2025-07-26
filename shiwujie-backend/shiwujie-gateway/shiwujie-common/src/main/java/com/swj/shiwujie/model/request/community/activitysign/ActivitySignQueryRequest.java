package com.swj.shiwujie.model.request.community.activitysign;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;

/**
 * 活动报名签到分页查询请求
 */
@Data
public class ActivitySignQueryRequest extends PageRequest {
    /**
     * 活动ID
     */
    private Long activityId;

}