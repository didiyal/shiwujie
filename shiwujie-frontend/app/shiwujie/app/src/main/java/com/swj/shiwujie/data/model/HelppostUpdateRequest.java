package com.swj.shiwujie.data.model;

/**
 * 求助帖更新请求模型
 */
public class HelppostUpdateRequest {
    private String helpContent;
    private String helpLocation;
    private Long helppostId;
    private String postStatus;

    public HelppostUpdateRequest() {
    }

    public HelppostUpdateRequest(String helpContent, String helpLocation, Long helppostId, String postStatus) {
        this.helpContent = helpContent;
        this.helpLocation = helpLocation;
        this.helppostId = helppostId;
        this.postStatus = postStatus;
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

    public Long getHelppostId() {
        return helppostId;
    }

    public void setHelppostId(Long helppostId) {
        this.helppostId = helppostId;
    }

    public String getPostStatus() {
        return postStatus;
    }

    public void setPostStatus(String postStatus) {
        this.postStatus = postStatus;
    }
} 