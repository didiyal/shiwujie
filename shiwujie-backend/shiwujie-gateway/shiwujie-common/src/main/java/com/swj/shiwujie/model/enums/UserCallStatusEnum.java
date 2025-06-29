package com.swj.shiwujie.model.enums;


import lombok.Getter;

@Getter
public enum UserCallStatusEnum {
    NO_CALLING("视频通话不在线",0),
    WAIT_CALL("视频通话等待中",1),
    CALLING("正在视频通话中",2);

    private final String callStatus;

    private final Integer value;


    UserCallStatusEnum(String callStatus, Integer value) {
        this.callStatus = callStatus;
        this.value = value;
    }
}
