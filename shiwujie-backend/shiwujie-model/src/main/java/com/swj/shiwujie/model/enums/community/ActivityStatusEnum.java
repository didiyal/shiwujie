package com.swj.shiwujie.model.enums.community;


import lombok.Getter;

/**
 * 活动状态枚举类
 */
@Getter
public enum ActivityStatusEnum {


    WAITING("未开始",0),
    DOING("进行中",1),
    END_HELP("已结束",2),
    FALL("已取消",3);

    private String name;

    private int postStatus;

    ActivityStatusEnum(String name, int postStatus) {
        this.postStatus = postStatus;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getPostStatus() {
        return postStatus;
    }

    public static ActivityStatusEnum getByName(String name){
        for (ActivityStatusEnum value : ActivityStatusEnum.values()) {
            if(value.name.equals(name)){
                return value;
            }
        }
        return null;
    }

    public static ActivityStatusEnum getById(long postStatus) {
        for (ActivityStatusEnum value : ActivityStatusEnum.values()) {
            if (value.postStatus == postStatus) {
                return value;
            }
        }
        return null;
    }
}
