package com.swj.shiwujie.data.model;

import java.util.List;

/**
 * 障碍物检测结果响应 - 严格按照原Python代码的process_frame返回结构
 * 改造说明：将Python后端的JSON响应数据结构转换为Android原生数据模型
 * 对应原代码：return jsonify({
 *     "success": True,
 *     "result_image_url": result_image_url,
 *     "detected_objects": detections,
 *     "nearest_unknown_obstacle": unknown_obstacle
 * })
 */
public class ObstacleDetectionResultResponse {
    private boolean success;
    private String resultImageUrl;           // 对应原代码的result_image_url
    private List<String> detectedObjects;   // 对应原代码的detected_objects
    private String nearestUnknownObstacle;  // 对应原代码的nearest_unknown_obstacle
    
    public ObstacleDetectionResultResponse() {}
    
    public ObstacleDetectionResultResponse(boolean success, String resultImageUrl, 
                                        List<String> detectedObjects, String nearestUnknownObstacle) {
        this.success = success;
        this.resultImageUrl = resultImageUrl;
        this.detectedObjects = detectedObjects;
        this.nearestUnknownObstacle = nearestUnknownObstacle;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getResultImageUrl() { return resultImageUrl; }
    public void setResultImageUrl(String resultImageUrl) { this.resultImageUrl = resultImageUrl; }
    
    public List<String> getDetectedObjects() { return detectedObjects; }
    public void setDetectedObjects(List<String> detectedObjects) { this.detectedObjects = detectedObjects; }
    
    public String getNearestUnknownObstacle() { return nearestUnknownObstacle; }
    public void setNearestUnknownObstacle(String nearestUnknownObstacle) { this.nearestUnknownObstacle = nearestUnknownObstacle; }
}
