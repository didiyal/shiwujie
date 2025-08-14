package com.swj.shiwujie.common.utils;

import android.content.Context;
import android.util.Log;
import com.swj.shiwujie.data.model.ObstacleDetectionData;
import java.util.List;

/**
 * 障碍物检测TTS管理器
 * 专门负责将障碍物检测结果转换为中文文本并通过TTS播报
 */
public class ObstacleDetectionTTSManager {
    
    private static final String TAG = "ObstacleDetectionTTSManager";
    
    private Context context;
    private TTSManager ttsManager;
    
    public ObstacleDetectionTTSManager(Context context) {
        this.context = context;
        this.ttsManager = new TTSManager(context);
    }
    
    /**
     * 处理障碍物检测数据并播报
     * @param detectionData 障碍物检测数据
     */
    public void processAndSpeak(ObstacleDetectionData detectionData) {
        try {
            if (detectionData == null) {
                Log.w(TAG, "检测数据为空，跳过TTS播报");
                return;
            }
            
            // 生成中文摘要文本
            String summaryText = generateSummary(detectionData);
            
            if (summaryText != null && !summaryText.trim().isEmpty()) {
                // 通过TTS播报
                speakSummary(summaryText);
                Log.d(TAG, "障碍物检测TTS播报已启动: " + summaryText);
            } else {
                Log.w(TAG, "生成的摘要文本为空，跳过TTS播报");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理障碍物检测数据失败", e);
        }
    }
    
    /**
     * 生成障碍物检测的中文摘要
     * 参考JavaScript版本的generateSummary函数逻辑
     * @param data 检测数据
     * @return 中文摘要文本
     */
    private String generateSummary(ObstacleDetectionData data) {
        try {
            StringBuilder summary = new StringBuilder();
            summary.append("环境分析报告，");
            
            // 检查是否有未知障碍物
            if (data.getNearestUnknownObstacle() != null) {
                ObstacleDetectionData.UnknownObstacle obs = data.getNearestUnknownObstacle();
                double distance = obs.getDistanceM();
                List<Double> location = obs.getLocationRelative();
                
                if (location != null && location.size() > 0) {
                    double relativeX = location.get(0);
                    String position = getPosition(relativeX);
                    summary.append("请注意！在您").append(position).append("方，大约");
                    summary.append(String.format("%.1f", distance)).append("米处，有一个未知障碍物。");
                }
            }
            
            // 检查检测到的物体
            List<ObstacleDetectionData.DetectedObject> detectedObjects = data.getDetectedObjects();
            if (detectedObjects != null && !detectedObjects.isEmpty()) {
                // 按距离排序（距离越近越重要）
                detectedObjects.sort((a, b) -> Double.compare(a.getDistanceM(), b.getDistanceM()));
                
                summary.append("侦测到的物体有，");
                for (ObstacleDetectionData.DetectedObject obj : detectedObjects) {
                    List<Double> boxCenter = obj.getBoxCenterRelative();
                    if (boxCenter != null && boxCenter.size() > 0) {
                        double relativeX = boxCenter.get(0);
                        String position = getPosition(relativeX);
                        String className = obj.getClassName();
                        double distance = obj.getDistanceM();
                        
                        summary.append("在您的").append(position).append("方，大约");
                        summary.append(String.format("%.1f", distance)).append("米处，有一个");
                        summary.append(translateClassName(className)).append("。");
                    }
                }
            } else {
                summary.append("未侦测到明确的物体。");
            }
            
            // 如果没有检测到任何障碍物或物体
            if (summary.toString().equals("环境分析报告，")) {
                summary.append("前方似乎是开阔地带，未检测到近距离的物体或障碍物。");
            }
            
            return summary.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "生成障碍物检测摘要失败", e);
            return "环境分析报告生成失败，请重试。";
        }
    }
    
    /**
     * 根据相对X坐标获取位置描述
     * 参考JavaScript版本的getPosition函数逻辑
     * @param relativeX 相对X坐标（0-1之间）
     * @return 位置描述（左、正前、右）
     */
    private String getPosition(double relativeX) {
        if (relativeX < 0.33) {
            return "左";
        } else if (relativeX > 0.66) {
            return "右";
        } else {
            return "正前";
        }
    }
    
    /**
     * 翻译物体类别名称
     * 将英文类别名翻译为中文
     * @param className 英文类别名
     * @return 中文类别名
     */
    private String translateClassName(String className) {
        if (className == null) return "未知物体";
        
        switch (className.toLowerCase()) {
            case "person":
                return "人";
            case "car":
                return "汽车";
            case "truck":
                return "卡车";
            case "bus":
                return "公交车";
            case "motorcycle":
                return "摩托车";
            case "bicycle":
                return "自行车";
            case "dog":
                return "狗";
            case "cat":
                return "猫";
            case "chair":
                return "椅子";
            case "table":
                return "桌子";
            case "refrigerator":
                return "冰箱";
            case "tv":
            case "television":
                return "电视";
            case "laptop":
                return "笔记本电脑";
            case "cell phone":
            case "phone":
                return "手机";
            case "book":
                return "书";
            case "cup":
                return "杯子";
            case "bottle":
                return "瓶子";
            case "bowl":
                return "碗";
            case "fork":
                return "叉子";
            case "knife":
                return "刀子";
            case "spoon":
                return "勺子";
            case "clock":
                return "时钟";
            case "vase":
                return "花瓶";
            case "scissors":
                return "剪刀";
            case "teddy bear":
                return "泰迪熊";
            case "hair drier":
                return "吹风机";
            case "toothbrush":
                return "牙刷";
            case "umbrella":
                return "雨伞";
            case "handbag":
                return "手提包";
            case "backpack":
                return "背包";
            case "suitcase":
                return "行李箱";
            case "frisbee":
                return "飞盘";
            case "skis":
                return "滑雪板";
            case "snowboard":
                return "滑雪板";
            case "sports ball":
                return "运动球";
            case "kite":
                return "风筝";
            case "baseball bat":
                return "棒球棒";
            case "baseball glove":
                return "棒球手套";
            case "skateboard":
                return "滑板";
            case "surfboard":
                return "冲浪板";
            case "tennis racket":
                return "网球拍";
            case "wine glass":
                return "酒杯";
            case "banana":
                return "香蕉";
            case "apple":
                return "苹果";
            case "sandwich":
                return "三明治";
            case "orange":
                return "橙子";
            case "broccoli":
                return "西兰花";
            case "carrot":
                return "胡萝卜";
            case "hot dog":
                return "热狗";
            case "pizza":
                return "披萨";
            case "donut":
                return "甜甜圈";
            case "cake":
                return "蛋糕";
            case "couch":
            case "sofa":
                return "沙发";
            case "potted plant":
                return "盆栽植物";
            case "bed":
                return "床";
            case "dining table":
                return "餐桌";
            case "toilet":
                return "马桶";
            case "remote":
                return "遥控器";
            case "keyboard":
                return "键盘";
            case "mouse":
                return "鼠标";
            case "microwave":
                return "微波炉";
            case "oven":
                return "烤箱";
            case "toaster":
                return "烤面包机";
            case "sink":
                return "水槽";
            case "fire hydrant":
                return "消防栓";
            case "stop sign":
                return "停车标志";
            case "parking meter":
                return "停车计时器";
            case "bench":
                return "长凳";
            case "bird":
                return "鸟";
            case "horse":
                return "马";
            case "sheep":
                return "羊";
            case "cow":
                return "牛";
            case "elephant":
                return "大象";
            case "bear":
                return "熊";
            case "zebra":
                return "斑马";
            case "giraffe":
                return "长颈鹿";
            case "airplane":
                return "飞机";
            case "train":
                return "火车";
            case "boat":
                return "船";
            case "traffic light":
                return "交通信号灯";
            case "fire truck":
                return "消防车";
            case "ambulance":
                return "救护车";
            case "police car":
                return "警车";
            default:
                return className; // 如果无法翻译，返回原文
        }
    }
    
    /**
     * 通过TTS播报摘要
     * @param summary 要播报的摘要文本
     */
    private void speakSummary(String summary) {
        try {
            if (ttsManager != null && summary != null && !summary.trim().isEmpty()) {
                // 停止当前正在播放的TTS（如果有）
                if (ttsManager.isSpeaking()) {
                    ttsManager.stopSpeaking();
                    // 等待一小段时间确保停止完成
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // 开始播报新的检测结果
                ttsManager.startSpeaking(summary);
                
                Log.d(TAG, "TTS播报已启动: " + summary);
            } else {
                Log.w(TAG, "TTS管理器未初始化或播报文本为空");
            }
        } catch (Exception e) {
            Log.e(TAG, "TTS播报摘要失败", e);
        }
    }
    
    /**
     * 设置TTS监听器
     * @param listener TTS监听器
     */
    public void setOnTTSListener(TTSManager.OnTTSListener listener) {
        if (ttsManager != null) {
            ttsManager.setOnTTSListener(listener);
        }
    }
    
    /**
     * 释放资源
     */
    public void destroy() {
        try {
            if (ttsManager != null) {
                ttsManager.destroy();
                ttsManager = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "销毁障碍物检测TTS管理器失败", e);
        }
    }
}
