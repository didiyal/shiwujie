package com.swj.shiwujie.model.VO.user.familyJoinReview;

import com.baomidou.mybatisplus.annotation.*;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 家庭加入审核返回类
 */
@Data
public class FamilyJoinReviewVO implements Serializable {
    private static final long serialVersionUID = -7405795888806528774L;
    /**
     * 家庭审核ID
     */
    private Long reviewId;

    /**
     * 家庭id
     */
    private Long familyId;

    /**
     * 审核视障人士
     */
    private Long blindId;

    /**
     * 审核志愿者
     */
    private Long volunteerId;

    /**
     * 请求加入时间
     */
    private Date applyTime;

    /**
     * 审核志愿者ID
     */
    private Long reviewerId;

    /**
     * 审核状态 0-待审核 1-已通过 2-已拒绝
     */
    private String reviewStatus;


}