package com.swj.shiwujie.model.request.community.activitysign;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 活动报名签到分页查询请求
 */
@Data
public class ActivitySignQueryRequest extends PageRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 活动ID
     */
    private Long activityId;


    private Long volunteerId;


    private Long blindId;

}