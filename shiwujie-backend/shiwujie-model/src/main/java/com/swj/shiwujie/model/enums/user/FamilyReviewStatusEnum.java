package com.swj.shiwujie.model.enums.user;

import lombok.Getter;

/**
 * 家庭审核枚举类
 */
@Getter
public enum FamilyReviewStatusEnum {



    WAIT_REVIEW("待审核",0),
    PASSED("已通过",1),
    REJECTED("已拒绝",2);


    /**
     * 名字
     */
    private String name;

    /**
     * 审核状态 0-待审核 1-已通过 2-已拒绝
     */
    private Integer reviewStatus;


    FamilyReviewStatusEnum(String name, Integer reviewStatus) {
        this.name = name;
        this.reviewStatus = reviewStatus;
    }
}
