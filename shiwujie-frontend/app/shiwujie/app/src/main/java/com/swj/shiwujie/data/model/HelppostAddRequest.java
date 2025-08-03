package com.swj.shiwujie.data.model;

/**
 * 求助帖创建请求
 */
public class HelppostAddRequest {
    private String helpContent;
    private String helpLocation;

    public HelppostAddRequest() {
    }

    public HelppostAddRequest(String helpContent, String helpLocation) {
        this.helpContent = helpContent;
        this.helpLocation = helpLocation;
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
} 