package com.swj.shiwujie.model.VO.community.communityJoinReview;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 社区加入审核返回类
 */
@Data
public class CommunityJoinReviewVO implements Serializable {



    /**
     * 社区审核ID
     */
    private Long reviewId;

    /**
     * 社区id
     */
    private Long communityId;

    /**
     * 审核视障人士ID
     */
    private Long blindId;

    /**
     * 审核志愿者ID
     */
    private Long volunteerId;

    /**
     * 请求加入时间
     */
    private Date applyTime;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 审核志愿者ID
     */
    private Long reviewerId;

    /**
     * 审核状态 0-待审核 1-已通过 2-已拒绝
     */
    private String reviewStatus;



    private static final long serialVersionUID = 1L;
}