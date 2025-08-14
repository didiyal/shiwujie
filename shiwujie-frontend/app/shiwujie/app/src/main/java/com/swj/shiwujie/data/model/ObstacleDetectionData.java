package com.swj.shiwujie.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 障碍物检测数据模型
 * 对应后端Python代码返回的JSON数据结构
 */
public class ObstacleDetectionData {
    
    @SerializedName("detected_objects")
    private List<DetectedObject> detectedObjects;
    
    @SerializedName("nearest_unknown_obstacle")
    private UnknownObstacle nearestUnknownObstacle;
    
    @SerializedName("result_image_url")
    private String resultImageUrl;
    
    @SerializedName("success")
    private boolean success;
    
    // 构造函数
    public ObstacleDetectionData() {}
    
    // Getter和Setter方法
    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }
    
    public void setDetectedObjects(List<DetectedObject> detectedObjects) {
        this.detectedObjects = detectedObjects;
    }
    
    public UnknownObstacle getNearestUnknownObstacle() {
        return nearestUnknownObstacle;
    }
    
    public void setNearestUnknownObstacle(UnknownObstacle nearestUnknownObstacle) {
        this.nearestUnknownObstacle = nearestUnknownObstacle;
    }
    
    public String getResultImageUrl() {
        return resultImageUrl;
    }
    
    public void setResultImageUrl(String resultImageUrl) {
        this.resultImageUrl = resultImageUrl;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * 检测到的物体数据模型
     */
    public static class DetectedObject {
        @SerializedName("box_center_relative")
        private List<Double> boxCenterRelative;
        
        @SerializedName("class_name")
        private String className;
        
        @SerializedName("distance_m")
        private double distanceM;
        
        @SerializedName("track_id")
        private int trackId;
        
        // 构造函数
        public DetectedObject() {}
        
        // Getter和Setter方法
        public List<Double> getBoxCenterRelative() {
            return boxCenterRelative;
        }
        
        public void setBoxCenterRelative(List<Double> boxCenterRelative) {
            this.boxCenterRelative = boxCenterRelative;
        }
        
        public String getClassName() {
            return className;
        }
        
        public void setClassName(String className) {
            this.className = className;
        }
        
        public double getDistanceM() {
            return distanceM;
        }
        
        public void setDistanceM(double distanceM) {
            this.distanceM = distanceM;
        }
        
        public int getTrackId() {
            return trackId;
        }
        
        public void setTrackId(int trackId) {
            this.trackId = trackId;
        }
    }
    
    /**
     * 未知障碍物数据模型
     */
    public static class UnknownObstacle {
        @SerializedName("distance_m")
        private double distanceM;
        
        @SerializedName("location_relative")
        private List<Double> locationRelative;
        
        // 构造函数
        public UnknownObstacle() {}
        
        // Getter和Setter方法
        public double getDistanceM() {
            return distanceM;
        }
        
        public void setDistanceM(double distanceM) {
            this.distanceM = distanceM;
        }
        
        public List<Double> getLocationRelative() {
            return locationRelative;
        }
        
        public void setLocationRelative(List<Double> locationRelative) {
            this.locationRelative = locationRelative;
        }
    }
}
