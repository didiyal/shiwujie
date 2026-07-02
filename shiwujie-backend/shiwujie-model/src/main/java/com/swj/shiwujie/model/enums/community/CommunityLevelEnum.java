package com.swj.shiwujie.model.enums.community;


import lombok.Getter;


/**
 * 社区类级别枚举类
 */
@Getter
public enum CommunityLevelEnum {


    PROVINCE("省级","企业,国企,个人成立的私人团体",1),
    CITY("市级","居委会或村委会",2),
    STREET("其它公益组织","其它组织",3);

    private String name;

    private String description;

    private long levelId;

    CommunityLevelEnum(String name, String description, long levelId) {
        this.name = name;
        this.description = description;
        this.levelId = levelId;
    }


    public static CommunityLevelEnum getById(long levelId){
        for (CommunityLevelEnum value : CommunityLevelEnum.values()) {
            if(value.levelId == levelId){
                return value;
            }
        }
        return null;
    }
}
