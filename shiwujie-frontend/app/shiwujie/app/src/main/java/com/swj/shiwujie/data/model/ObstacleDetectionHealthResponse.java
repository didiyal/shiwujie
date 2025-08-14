package com.swj.shiwujie.data.model;

/**
 * 障碍物检测健康状态响应 - 严格按照原Python代码的health_check返回结构
 * 改造说明：将Python后端的JSON响应数据结构转换为Android原生数据模型
 * 对应原代码的完整health_status字典结构
 */
public class ObstacleDetectionHealthResponse {
    private String status;                   // 对应原代码的'status': 'UP'
    private String service;                  // 对应原代码的'service': SERVICE_NAME
    private long timestamp;                  // 对应原代码的'timestamp': time.time()
    private String message;                  // 对应原代码的'message': 'Service is healthy'
    private HealthDetails details;           // 对应原代码的'details'字典
    
    public ObstacleDetectionHealthResponse() {}
    
    public ObstacleDetectionHealthResponse(String status, String service, long timestamp, 
                                        String message, HealthDetails details) {
        this.status = status;
        this.service = service;
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
    }
    
    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public HealthDetails getDetails() { return details; }
    public void setDetails(HealthDetails details) { this.details = details; }
    
    /**
     * 健康状态详细信息 - 对应原Python代码的details字典
     */
    public static class HealthDetails {
        private String cpuUsage;             // 对应原代码的'cpu_usage': f"{cpu_percent:.1f}%"
        private String memoryUsage;          // 对应原代码的'memory_usage': f"{memory.percent:.1f}%"
        private String memoryAvailable;      // 对应原代码的'memory_available': f"{memory.available / (1024**3):.1f}GB"
        private int activeSessions;          // 对应原代码的'active_sessions': active_sessions
        private ModelStatus models;          // 对应原代码的'models'字典
        private ServiceInfo serviceInfo;     // 对应原代码的'service_info'字典
        private String dubboStatus;          // 对应原代码的'dubbo_status': 'AVAILABLE' if DUBBO_AVAILABLE else 'UNAVAILABLE'
        private StreamingConfig streamingConfig; // 对应原代码的'streaming_config': STREAMING_CONFIG
        
        public HealthDetails() {}
        
        public HealthDetails(String cpuUsage, String memoryUsage, String memoryAvailable,
                           int activeSessions, ModelStatus models, ServiceInfo serviceInfo,
                           String dubboStatus, StreamingConfig streamingConfig) {
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.memoryAvailable = memoryAvailable;
            this.activeSessions = activeSessions;
            this.models = models;
            this.serviceInfo = serviceInfo;
            this.dubboStatus = dubboStatus;
            this.streamingConfig = streamingConfig;
        }
        
        // Getters and Setters
        public String getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(String cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public String getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(String memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public String getMemoryAvailable() { return memoryAvailable; }
        public void setMemoryAvailable(String memoryAvailable) { this.memoryAvailable = memoryAvailable; }
        
        public int getActiveSessions() { return activeSessions; }
        public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }
        
        public ModelStatus getModels() { return models; }
        public void setModels(ModelStatus models) { this.models = models; }
        
        public ServiceInfo getServiceInfo() { return serviceInfo; }
        public void setServiceInfo(ServiceInfo serviceInfo) { this.serviceInfo = serviceInfo; }
        
        public String getDubboStatus() { return dubboStatus; }
        public void setDubboStatus(String dubboStatus) { this.dubboStatus = dubboStatus; }
        
        public StreamingConfig getStreamingConfig() { return streamingConfig; }
        public void setStreamingConfig(StreamingConfig streamingConfig) { this.streamingConfig = streamingConfig; }
    }
    
    /**
     * 模型状态信息 - 对应原Python代码的models字典
     */
    public static class ModelStatus {
        private String yoloModel;            // 对应原代码的'yolo_model': 'OK' if yolo_model_exists else 'MISSING'
        private String depthModel;           // 对应原代码的'depth_model': 'OK' if depth_model_exists else 'MISSING'
        
        public ModelStatus() {}
        
        public ModelStatus(String yoloModel, String depthModel) {
            this.yoloModel = yoloModel;
            this.depthModel = depthModel;
        }
        
        // Getters and Setters
        public String getYoloModel() { return yoloModel; }
        public void setYoloModel(String yoloModel) { this.yoloModel = yoloModel; }
        
        public String getDepthModel() { return depthModel; }
        public void setDepthModel(String depthModel) { this.depthModel = depthModel; }
    }
    
    /**
     * 服务信息 - 对应原Python代码的service_info字典
     */
    public static class ServiceInfo {
        private String ip;                   // 对应原代码的'ip': SERVICE_IP
        private int port;                    // 对应原代码的'port': SERVICE_PORT
        private long uptime;                 // 对应原代码的'uptime': time.time() - start_time if 'start_time' in globals() else 0
        
        public ServiceInfo() {}
        
        public ServiceInfo(String ip, int port, long uptime) {
            this.ip = ip;
            this.port = port;
            this.uptime = uptime;
        }
        
        // Getters and Setters
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
    }
    
    /**
     * 流式传输配置 - 对应原Python代码的STREAMING_CONFIG
     */
    public static class StreamingConfig {
        private int defaultTargetFps;        // 对应原代码的'default_target_fps': 5
        private int maxTargetFps;            // 对应原代码的'max_target_fps': 10
        private int minTargetFps;            // 对应原代码的'min_target_fps': 1
        private boolean adaptiveProcessing;  // 对应原代码的'adaptive_processing': True
        
        public StreamingConfig() {}
        
        public StreamingConfig(int defaultTargetFps, int maxTargetFps, int minTargetFps, boolean adaptiveProcessing) {
            this.defaultTargetFps = defaultTargetFps;
            this.maxTargetFps = maxTargetFps;
            this.minTargetFps = minTargetFps;
            this.adaptiveProcessing = adaptiveProcessing;
        }
        
        // Getters and Setters
        public int getDefaultTargetFps() { return defaultTargetFps; }
        public void setDefaultTargetFps(int defaultTargetFps) { this.defaultTargetFps = defaultTargetFps; }
        
        public int getMaxTargetFps() { return maxTargetFps; }
        public void setMaxTargetFps(int maxTargetFps) { this.maxTargetFps = maxTargetFps; }
        
        public int getMinTargetFps() { return minTargetFps; }
        public void setMinTargetFps(int minTargetFps) { this.minTargetFps = minTargetFps; }
        
        public boolean isAdaptiveProcessing() { return adaptiveProcessing; }
        public void setAdaptiveProcessing(boolean adaptiveProcessing) { this.adaptiveProcessing = adaptiveProcessing; }
    }
}
