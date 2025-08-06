package com.swj.shiwujie.data.model;

/**
 * 求助帖更新请求
 */
public class HelppostUpdateRequest {
    private Long helppostId;
    private String helpContent;
    private String helpLocation;
    private String postStatus;
    private Long volunteerId;

    public HelppostUpdateRequest() {
    }

    public HelppostUpdateRequest(Long helppostId, String helpContent, String helpLocation, String postStatus, Long volunteerId) {
        this.helppostId = helppostId;
        this.helpContent = helpContent;
        this.helpLocation = helpLocation;
        this.postStatus = postStatus;
        this.volunteerId = volunteerId;
    }

    public Long getHelppostId() {
        return helppostId;
    }

    public void setHelppostId(Long helppostId) {
        this.helppostId = helppostId;
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

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }
} 