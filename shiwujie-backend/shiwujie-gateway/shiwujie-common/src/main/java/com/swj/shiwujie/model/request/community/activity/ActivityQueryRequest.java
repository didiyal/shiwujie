package com.swj.shiwujie.model.request.community.activity;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 活动查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityQueryRequest extends PageRequest {
    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 活动状态 (对应ActivityStatusEnum的字符串描述)
     */
    private String activityStatus;
}