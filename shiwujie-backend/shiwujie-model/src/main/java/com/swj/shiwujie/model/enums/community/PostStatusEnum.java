package com.swj.shiwujie.model.enums.community;


import lombok.Getter;

/**
 * 求助帖求助状态枚举类
 */
@Getter
public enum PostStatusEnum {


    WAITING("待响应",0),
    HELPING("处理中",1),
    END_HELP("已完成",2),
    FALL("已取消",3);

    private String name;

    private int postStatus;

    PostStatusEnum(String name, int postStatus) {
        this.postStatus = postStatus;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getPostStatus() {
        return postStatus;
    }

    public static PostStatusEnum getByName(String name){
        for (PostStatusEnum value : PostStatusEnum.values()) {
            if(value.name.equals(name)){
                return value;
            }
        }
        return null;
    }

    public static PostStatusEnum getById(long postStatus) {
        for (PostStatusEnum value : PostStatusEnum.values()) {
            if (value.postStatus == postStatus) {
                return value;
            }
        }
        return null;
    }
}
