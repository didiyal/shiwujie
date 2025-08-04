package com.swj.shiwujie.model.enums.community;


import lombok.Getter;


/**
 * 社区类型枚举类
 */
@Getter
public enum CommunityTypeEnum {


    SOCIAL_ORGANIZATIONS("社会团体","企业,国企,个人成立的私人团体",1),
    SELF_COVER_ORGANIZATIONS("基层群众自治组织","居委会或村委会",2),
    ESTABLISHED_UNIVERSITY("高校内部成立","高校,初高中成立的组织",3),
    OTHER_ORGANIZATIONS("其它公益组织","其它组织",4);

    private String name;

    private String description;

    private long typeId;

    CommunityTypeEnum(String name, String description, long typeId) {
        this.name = name;
        this.description = description;
        this.typeId = typeId;
    }


    public static CommunityTypeEnum getByName(String name){
        for (CommunityTypeEnum value : CommunityTypeEnum.values()) {
            if(value.name.equals(name)){
                return value;
            }
        }
        return null;
    }

    public static CommunityTypeEnum getById(long typeId) {
        for (CommunityTypeEnum value : CommunityTypeEnum.values()) {
            if (value.typeId == typeId) {
                return value;
            }
        }
        return null;
    }


}
