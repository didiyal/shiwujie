package com.swj.shiwujie.model.domain.community;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 社区加入审核表
 * @TableName CommunityJoinReview
 */
@TableName(value ="CommunityJoinReview")
@Data
public class Communityjoinreview implements Serializable {
    /**
     * 社区审核ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long reviewId;

    /**
     * 视障人士ID
     */
    private Long blindId;

    /**
     * 志愿者ID
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
     * 审核状态 0-待审核 1-已通过 2-已拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核志愿者ID
     */
    private Long reviewerId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 信息更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除 0-存在 1-删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}