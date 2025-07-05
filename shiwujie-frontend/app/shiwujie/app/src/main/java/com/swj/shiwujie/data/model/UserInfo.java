package com.swj.shiwujie.data.model;

public class UserInfo {
    private final Long blindId;
    private final Long familyId;
    private final String wechatId;
    private final String qqId;
    private final Boolean isIdCard;
    private final String token;

    public UserInfo(Long blindId, Long familyId, String wechatId, String qqId, Boolean isIdCard, String token) {
        this.blindId = blindId;
        this.familyId = familyId;
        this.wechatId = wechatId;
        this.qqId = qqId;
        this.isIdCard = isIdCard;
        this.token = token;
    }

    public Long getBlindId() {
        return blindId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public String getWechatId() {
        return wechatId;
    }

    public String getQqId() {
        return qqId;
    }

    public Boolean getIsIdCard() {
        return isIdCard;
    }

    public String getToken() {
        return token;
    }
} 