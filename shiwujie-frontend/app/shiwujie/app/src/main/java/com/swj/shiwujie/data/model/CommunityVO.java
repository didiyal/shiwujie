package com.swj.shiwujie.data.model;

/**
 * 社区信息数据模型
 */
public class CommunityVO {
    private Long communityId;
    private String communityName;
    private String communityDescription;
    private String province;
    private String city;
    private String district;
    private String address;
    private Integer communityStatus;
    private Long registerVolunteerId;
    private String registerVolunteerName;

    public CommunityVO() {
    }

    public CommunityVO(Long communityId, String communityName, String communityDescription) {
        this.communityId = communityId;
        this.communityName = communityName;
        this.communityDescription = communityDescription;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    public String getCommunityDescription() {
        return communityDescription;
    }

    public void setCommunityDescription(String communityDescription) {
        this.communityDescription = communityDescription;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getCommunityStatus() {
        return communityStatus;
    }

    public void setCommunityStatus(Integer communityStatus) {
        this.communityStatus = communityStatus;
    }

    public Long getRegisterVolunteerId() {
        return registerVolunteerId;
    }

    public void setRegisterVolunteerId(Long registerVolunteerId) {
        this.registerVolunteerId = registerVolunteerId;
    }

    public String getRegisterVolunteerName() {
        return registerVolunteerName;
    }

    public void setRegisterVolunteerName(String registerVolunteerName) {
        this.registerVolunteerName = registerVolunteerName;
    }
} 