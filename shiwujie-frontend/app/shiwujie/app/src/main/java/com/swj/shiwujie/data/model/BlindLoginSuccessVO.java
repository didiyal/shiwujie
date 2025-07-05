package com.swj.shiwujie.data.model;

public class BlindLoginSuccessVO {
    private Long blindId;
    private Long communityId;
    private Long familyId;
    private Integer gender;
    private Long helpRequestCount;
    private Integer isActivelyJoined;
    private Boolean isDisabilityCard;
    private Boolean isIdCard;
    private Double latitude;
    private String locationAddress;
    private String locationUpdateTime;
    private Double longitude;
    private String name;
    private String otherInfo;
    private String password;
    private String phone;
    private String qqId;
    private String token;
    private String wechatId;

    // Getters
    public Long getBlindId() { return blindId; }
    public String getToken() { return token; }
    public String getPhone() { return phone; }
    
    // Setters
    public void setBlindId(Long blindId) { this.blindId = blindId; }
    public void setToken(String token) { this.token = token; }
    public void setPhone(String phone) { this.phone = phone; }
    
    // 其他getter和setter方法根据需要添加
} 