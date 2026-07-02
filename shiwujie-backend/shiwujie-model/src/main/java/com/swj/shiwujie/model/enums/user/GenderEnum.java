package com.swj.shiwujie.model.enums.user;


import lombok.Getter;

@Getter
public enum GenderEnum {


    MAN("男",0),
    WOMEN("女",1);

    private String name;

    private int content;

    GenderEnum(String name, int content) {
        this.content = content;
        this.name = name;
    }
}
