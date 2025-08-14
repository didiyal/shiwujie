package com.swj.shiwujie.data.model;

/**
 * 障碍物检测会话响应 - 对应原Python代码的start_session返回结构
 * 改造说明：将Python后端的JSON响应数据结构转换为Android原生数据模型
 * 对应原代码：return jsonify({'success': True, 'session_id': session_id})
 */
public class ObstacleDetectionSessionResponse {
    private boolean success;
    private String sessionId;
    
    public ObstacleDetectionSessionResponse() {}
    
    public ObstacleDetectionSessionResponse(boolean success, String sessionId) {
        this.success = success;
        this.sessionId = sessionId;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
