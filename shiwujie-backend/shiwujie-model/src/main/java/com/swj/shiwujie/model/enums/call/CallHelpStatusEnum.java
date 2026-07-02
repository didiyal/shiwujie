package com.swj.shiwujie.model.enums.call;


import lombok.Getter;

/**
 * 视频过程状态枚举类
 */
@Getter
public enum CallHelpStatusEnum {


    WAITING("待响应",0),
    HELPING("处理中",1),
    END_HELP("已完成",2),
    FALL("已取消",3);

    private String name;

    private int helpStatus;

    CallHelpStatusEnum(String name, int helpStatus) {
        this.helpStatus = helpStatus;
        this.name = name;
    }
}
