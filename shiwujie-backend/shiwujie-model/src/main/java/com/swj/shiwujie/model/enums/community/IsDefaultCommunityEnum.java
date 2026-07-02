package com.swj.shiwujie.model.enums.community;


import lombok.Getter;


/**
 * 默认社区判断枚举
 */
@Getter
public enum IsDefaultCommunityEnum {


    TRUE("是",0),
    FALSE("不是",1);

    private String name;


    private int isDefaultCommunity;

    IsDefaultCommunityEnum(String name, int isDefaultCommunity) {
        this.name = name;
        this.isDefaultCommunity = isDefaultCommunity;
    }



}
