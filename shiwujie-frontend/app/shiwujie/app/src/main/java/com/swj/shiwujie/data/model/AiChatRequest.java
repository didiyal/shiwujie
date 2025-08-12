package com.swj.shiwujie.data.model;

/**
 * AI对话请求数据模型
 */
public class AiChatRequest {
    private String message;
    private String userId;
    private String sessionId;
    private String userType; // "blind" 或 "volunteer"
    
    public AiChatRequest() {
    }
    
    public AiChatRequest(String message) {
        this.message = message;
    }
    
    public AiChatRequest(String message, String userId, String sessionId, String userType) {
        this.message = message;
        this.userId = userId;
        this.sessionId = sessionId;
        this.userType = userType;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserType() {
        return userType;
    }
    
    public void setUserType(String userType) {
        this.userType = userType;
    }
}
