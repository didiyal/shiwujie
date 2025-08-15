package com.swj.shiwujie.common.utils;

import android.content.Context;
import android.util.Log;
import com.swj.shiwujie.data.model.ObstacleDetectionData;
import java.util.List;
import java.util.ArrayList;

/**
 * 障碍物检测TTS管理器
 * 专门负责将障碍物检测结果转换为中文文本并通过TTS播报
 */
public class ObstacleDetectionTTSManager {
    
    private static final String TAG = "ObstacleDetectionTTSManager";
    
    private Context context;
    private TTSManager ttsManager;
    private String lastSpokenText = ""; // 记录上次播报的文本，防止重复播报
    private long lastSpokenTime = 0; // 记录上次播报的时间
    private boolean isCurrentlySpeaking = false; // 标记当前是否正在播报
    
    public ObstacleDetectionTTSManager(Context context) {
        this.context = context;
        this.ttsManager = new TTSManager(context);
        // 设置TTS语速为1.5倍速
        this.ttsManager.setSpeed(75);
        
        // 设置TTS监听器，管理播放状态
        this.ttsManager.setOnTTSListener(new TTSManager.OnTTSListener() {
            @Override
            public void onTTSStart() {
                isCurrentlySpeaking = true;
                Log.d(TAG, "TTS播报开始");
            }
            
            @Override
            public void onTTSProgress(int progress, int beginPos, int endPos) {
                // 播报进度更新
            }
            
            @Override
            public void onTTSComplete() {
                isCurrentlySpeaking = false;
                Log.d(TAG, "TTS播报完成");
            }
            
            @Override
            public void onTTSError(String error) {
                isCurrentlySpeaking = false;
                Log.e(TAG, "TTS播报出错: " + error);
            }
            
            @Override
            public void onTTSPause() {
                isCurrentlySpeaking = false;
                Log.d(TAG, "TTS播报暂停");
            }
            
            @Override
            public void onTTSResume() {
                isCurrentlySpeaking = true;
                Log.d(TAG, "TTS播报恢复");
            }
            
            @Override
            public void onTTSBufferProgress(int percent, int beginPos, int endPos, String info) {
                // 缓冲进度更新
            }
        });
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
            
            // 检查是否有有用的检测信息
            if (!hasUsefulDetectionInfo(detectionData)) {
                Log.d(TAG, "没有有用的检测信息，跳过TTS播报");
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
     * 判断是否有有用的检测信息需要播报
     * @param data 检测数据
     * @return true表示有有用信息，false表示没有有用信息
     */
    private boolean hasUsefulDetectionInfo(ObstacleDetectionData data) {
        try {
            // 检查是否有检测到的物体
            List<ObstacleDetectionData.DetectedObject> detectedObjects = data.getDetectedObjects();
            if (detectedObjects != null && !detectedObjects.isEmpty()) {
                return true;
            }
            
            // 即使没有检测到物体，也需要播报提示信息
            // 这样可以提醒用户保持机身平稳
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "判断检测信息是否有用失败", e);
            return false;
        }
    }
    
    /**
     * 生成障碍物检测的中文摘要
     * 只播报detected_objects中的已识别物体，选择距离用户最近的2条信息
     * @param data 检测数据
     * @return 中文摘要文本
     */
    private String generateSummary(ObstacleDetectionData data) {
        try {
            StringBuilder summary = new StringBuilder();
            
            // 优先处理detected_objects中的已识别物体
            List<ObstacleDetectionData.DetectedObject> detectedObjects = data.getDetectedObjects();
            if (detectedObjects != null && !detectedObjects.isEmpty()) {
                // 按距离排序（距离越近越重要）
                detectedObjects.sort((a, b) -> Double.compare(a.getDistanceM(), b.getDistanceM()));
                
                // 选择距离最近的两个物体
                int count = Math.min(2, detectedObjects.size());
                String lastPosition = null; // 记录上一个物体的方向
                
                for (int i = 0; i < count; i++) {
                    ObstacleDetectionData.DetectedObject obj = detectedObjects.get(i);
                    List<Double> boxCenter = obj.getBoxCenterRelative();
                    if (boxCenter != null && boxCenter.size() > 0) {
                        double relativeX = boxCenter.get(0);
                        String position = getPosition(relativeX);
                        String className = obj.getClassName();
                        double distance = obj.getDistanceM();
                        
                        String obstacleInfo;
                        if (i == 0) {
                            // 第一个物体，完整格式
                            obstacleInfo = String.format("在您%s方，大约%.1f米处，有一个%s", position, distance, translateClassName(className));
                            lastPosition = position;
                        } else {
                            // 第二个物体，检查是否与第一个同方向
                            if (position.equals(lastPosition)) {
                                // 同方向，简化格式
                                obstacleInfo = String.format("大约%.1f米处，有一个%s", distance, translateClassName(className));
                            } else {
                                // 不同方向，完整格式
                                obstacleInfo = String.format("在您%s方，大约%.1f米处，有一个%s", position, distance, translateClassName(className));
                                lastPosition = position;
                            }
                        }
                        
                        if (i == 0) {
                            summary.append(obstacleInfo);
                        } else {
                            summary.append("。").append(obstacleInfo);
                        }
                    }
                }
                
                // 如果detected_objects中有物体，直接返回，不再处理未知障碍物
                if (summary.length() > 0) {
                    summary.append("。");
                    return summary.toString();
                }
            }
            
            // 如果没有检测到物体，播报提示信息
            return "实时检测中，请保持机身平稳";
            
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
                // 防重复播报：如果文本相同且时间间隔小于2秒，则跳过
                long currentTime = System.currentTimeMillis();
                if (summary.equals(lastSpokenText) && (currentTime - lastSpokenTime) < 2000) {
                    Log.d(TAG, "检测到重复播报请求，跳过: " + summary);
                    return;
                }
                
                // 如果当前正在播报，立即停止并释放资源
                if (isCurrentlySpeaking || ttsManager.isSpeaking()) {
                    Log.d(TAG, "检测到TTS正在播放，立即停止当前播放并释放资源");
                    ttsManager.stopSpeaking();
                    isCurrentlySpeaking = false;
                    
                    // 给TTS一点时间完全释放资源，避免语音重叠
                    try {
                        Thread.sleep(50); // 等待50毫秒，确保资源完全释放
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // 标记开始播报
                isCurrentlySpeaking = true;
                
                // 立即开始播报新的检测结果
                ttsManager.startSpeaking(summary);
                
                // 更新记录
                lastSpokenText = summary;
                lastSpokenTime = currentTime;
                
                Log.d(TAG, "TTS播报已启动: " + summary);
            } else {
                Log.w(TAG, "TTS管理器未初始化或播报文本为空");
            }
        } catch (Exception e) {
            Log.e(TAG, "TTS播报摘要失败", e);
            isCurrentlySpeaking = false; // 出错时重置状态
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
                // 停止当前播放
                if (isCurrentlySpeaking) {
                    ttsManager.stopSpeaking();
                    isCurrentlySpeaking = false;
                }
                
                ttsManager.destroy();
                ttsManager = null;
            }
            
            // 重置状态
            lastSpokenText = "";
            lastSpokenTime = 0;
            isCurrentlySpeaking = false;
            
        } catch (Exception e) {
            Log.e(TAG, "销毁障碍物检测TTS管理器失败", e);
        }
    }
}
