package com.swj.shiwujie.model.enums;


import lombok.Getter;

@Getter
public enum CallHelpStatusEnum {

    WAITING_CALL_HELP("盲人求助人等待求助中",0),
    CALL_HELPING("盲人与帮助家属正在通话",1),
    CALL_HELP_END("求助正常结束",3),
    CALL_HELP_CANCEL("求助人主动取消求助",2);

    private final String status;
    private final Integer value;

    CallHelpStatusEnum(String status, Integer value){
        this.status = status;
        this.value = value;
    }
}
