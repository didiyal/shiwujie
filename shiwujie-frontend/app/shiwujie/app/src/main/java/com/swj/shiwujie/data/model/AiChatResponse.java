package com.swj.shiwujie.data.model;

/**
 * AI对话响应数据模型
 */
public class AiChatResponse {
    private String response;
    private String sessionId;
    private long timestamp;
    private String messageId;
    private String aiModel; // AI模型信息
    private int tokenCount; // 使用的token数量
    
    public AiChatResponse() {
    }
    
    public AiChatResponse(String response, String sessionId) {
        this.response = response;
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis();
    }
    
    public AiChatResponse(String response, String sessionId, long timestamp) {
        this.response = response;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getAiModel() {
        return aiModel;
    }
    
    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
    
    public int getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }
}
