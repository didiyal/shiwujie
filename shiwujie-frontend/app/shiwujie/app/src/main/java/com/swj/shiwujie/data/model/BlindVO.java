package com.swj.shiwujie.data.model;

import com.google.gson.annotations.SerializedName;

public class BlindVO {
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
    private String wechatId;
    private String email;
    private String token;
    
    @SerializedName("idCard")
    private String idCard = "";
    
    @SerializedName("disabilityCard")
    private String disabilityCard = "";

    // Getters and Setters
    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getDisabilityCard() {
        return disabilityCard;
    }

    public void setDisabilityCard(String disabilityCard) {
        this.disabilityCard = disabilityCard;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getBlindId() {
        return blindId;
    }

    public void setBlindId(Long blindId) {
        this.blindId = blindId;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Long getHelpRequestCount() {
        return helpRequestCount;
    }

    public void setHelpRequestCount(Long helpRequestCount) {
        this.helpRequestCount = helpRequestCount;
    }

    public Integer getIsActivelyJoined() {
        return isActivelyJoined;
    }

    public void setIsActivelyJoined(Integer isActivelyJoined) {
        this.isActivelyJoined = isActivelyJoined;
    }

    public Boolean getIsDisabilityCard() {
        return isDisabilityCard;
    }

    public void setIsDisabilityCard(Boolean isDisabilityCard) {
        this.isDisabilityCard = isDisabilityCard;
    }

    public Boolean getIsIdCard() {
        return isIdCard;
    }

    public void setIsIdCard(Boolean isIdCard) {
        this.isIdCard = isIdCard;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

    public String getLocationUpdateTime() {
        return locationUpdateTime;
    }

    public void setLocationUpdateTime(String locationUpdateTime) {
        this.locationUpdateTime = locationUpdateTime;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOtherInfo() {
        return otherInfo;
    }

    public void setOtherInfo(String otherInfo) {
        this.otherInfo = otherInfo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getQqId() {
        return qqId;
    }

    public void setQqId(String qqId) {
        this.qqId = qqId;
    }

    public String getWechatId() {
        return wechatId;
    }

    public void setWechatId(String wechatId) {
        this.wechatId = wechatId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
} 