package com.swj.shiwujie.model.request.user.familyJoinReview;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 家庭加入审核表修改请求类
 */
@Data
public class FamilyJoinReviewUpdateRequest implements Serializable {
    /**
     * 家庭审核ID
     */
    private Long reviewId;

    /**
     * 审核结果
     */
    private Boolean reviewResult;

    /**
     * 审核志愿者ID
     */
    private Long reviewerId;

}