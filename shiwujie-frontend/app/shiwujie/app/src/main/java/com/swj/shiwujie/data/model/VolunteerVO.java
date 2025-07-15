package com.swj.shiwujie.data.model;

import java.math.BigDecimal;

/**
 * 志愿者信息表
 */
public class VolunteerVO {
    /**
     * 志愿者ID
     */
    private Long volunteerId;

    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 是否主动加入社区
     */
    private Integer isActivelyJoined;

    /**
     * 家庭ID
     */
    private Long familyId;

    /**
     * 名字
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别 0-男 1-女
     */
    private Integer gender;

    /**
     * 微信账号
     */
    private String wechatId;

    /**
     * QQ账号
     */
    private String qqId;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 其它信息
     */
    private String otherInfo;

    /**
     * 在线状态（用于区分是否匹配） 0-离线 1-在线 2-忙碌
     */
    private Integer onlineStatus;

    /**
     * 帮助次数
     */
    private Long helpCount;

    /**
     * 志愿者评分
     */
    private BigDecimal rating;

    /**
     * 纬度坐标
     */
    private BigDecimal latitude;

    /**
     * 经度坐标
     */
    private BigDecimal longitude;

    /**
     * 位置地址（省市区+详细地址）
     */
    private String locationAddress;

    /**
     * 位置更新时间
     */
    private String locationUpdateTime;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;

    /**
     * 是否已实名认证
     */
    private Boolean isIdCard;

    /**
     * 邮箱
     */
    private String email;
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public Integer getIsActivelyJoined() {
        return isActivelyJoined;
    }

    public void setIsActivelyJoined(Integer isActivelyJoined) {
        this.isActivelyJoined = isActivelyJoined;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getWechatId() {
        return wechatId;
    }

    public void setWechatId(String wechatId) {
        this.wechatId = wechatId;
    }

    public String getQqId() {
        return qqId;
    }

    public void setQqId(String qqId) {
        this.qqId = qqId;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getOtherInfo() {
        return otherInfo;
    }

    public void setOtherInfo(String otherInfo) {
        this.otherInfo = otherInfo;
    }

    public Integer getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(Integer onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public Long getHelpCount() {
        return helpCount;
    }

    public void setHelpCount(Long helpCount) {
        this.helpCount = helpCount;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
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

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public Boolean getIsIdCard() {
        return isIdCard;
    }

    public void setIsIdCard(Boolean isIdCard) {
        this.isIdCard = isIdCard;
    }
} 