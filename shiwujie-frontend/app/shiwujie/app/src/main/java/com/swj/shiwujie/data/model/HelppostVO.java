package com.swj.shiwujie.data.model;

/**
 * 求助帖响应数据
 */
public class HelppostVO {
    private Long helppostId;
    private Long blindId;
    private Long volunteerId;
    private Long communityId;
    private String helpContent;
    private String helpLocation;
    private String postStatus;
    private String helpStatus;
    private java.util.Date createTime;

    public HelppostVO() {
    }

    public Long getHelppostId() {
        return helppostId;
    }

    public void setHelppostId(Long helppostId) {
        this.helppostId = helppostId;
    }

    public Long getBlindId() {
        return blindId;
    }

    public void setBlindId(Long blindId) {
        this.blindId = blindId;
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

    public String getHelpContent() {
        return helpContent;
    }

    public void setHelpContent(String helpContent) {
        this.helpContent = helpContent;
    }

    public String getHelpLocation() {
        return helpLocation;
    }

    public void setHelpLocation(String helpLocation) {
        this.helpLocation = helpLocation;
    }

    public String getPostStatus() {
        return postStatus;
    }

    public void setPostStatus(String postStatus) {
        this.postStatus = postStatus;
    }

    public String getHelpStatus() {
        return helpStatus;
    }

    public void setHelpStatus(String helpStatus) {
        this.helpStatus = helpStatus;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }
} 