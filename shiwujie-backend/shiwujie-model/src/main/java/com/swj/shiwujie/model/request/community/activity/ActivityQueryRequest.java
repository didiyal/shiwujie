package com.swj.shiwujie.model.request.community.activity;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 活动查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityQueryRequest extends PageRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 活动状态 (对应ActivityStatusEnum的字符串描述)
     */
    private String activityStatus;
}