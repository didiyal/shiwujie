package com.swj.shiwujie.model.request.community.communityJoinReview;

import lombok.Data;

import java.io.Serializable;

/**
 * 社区加入审核表修改请求类
 */
@Data
public class CommunityJoinReviewUpdateRequest implements Serializable {
    /**
     * 社区审核ID
     */
    private Long reviewId;

    /**
     * 审核结果
     */
    private Boolean reviewResult;

    /**
     * 审核管理员ID
     */
    private Long reviewerId;

    private static final long serialVersionUID = 1L;
}