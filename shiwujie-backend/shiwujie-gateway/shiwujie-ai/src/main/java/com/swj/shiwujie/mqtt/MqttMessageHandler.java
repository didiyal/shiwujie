package com.swj.shiwujie.mqtt;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swj.shiwujie.app.ImageApp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MqttMessageHandler implements MqttCallbackExtended {

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageHandler.class);

    private MqttClientService mqttClientService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Resource
    private ImageApp imageApp;


    // 构造函数注入
    public MqttMessageHandler() {
    }

    // 设置MqttClientService的setter方法
    public void setMqttClientService(MqttClientService mqttClientService) {
        this.mqttClientService = mqttClientService;
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        logger.info("Connected to MQTT broker: {}", serverURI);
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.error("MQTT connection lost", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
        logger.info("收到消息: {}, QoS: {}, Retained: {},消息内容{},", topic, message.getQos(), message.isRetained(), payload);

        try {
            // 解析接收到的JSON消息
            Map<String, Object> receivedData = objectMapper.readValue(payload, Map.class);

            String type = (String) receivedData.get("type");
            if ("image".equals(type)) {
                // 处理图片类型的消息
                String base64Data = (String) receivedData.get("data");

                // 解码Base64数据
                byte[] imageData = Base64.getDecoder().decode(base64Data);

                // 保存图片并调用AI服务
                String imagePath = saveImageToFile(imageData);
                logger.info("图片已保存至: {}", imagePath);
                String processImageWithAI = processImageWithAI(imagePath);
                logger.info("AI处理结果: {}", processImageWithAI);

                // 构建成功响应
                Map<String, Object> response = new HashMap<>();
                response.put("code", 1);
                response.put("message", "success");
                response.put("data", "ok");

                String jsonResponse = objectMapper.writeValueAsString(response);
                mqttClientService.sendMessage("test/return", jsonResponse);
            } else {
                // 非图片类型的处理逻辑
                Map<String, Object> response = new HashMap<>();
                response.put("code", 1);
                response.put("message", "success");
                response.put("data", "ok");

                String jsonResponse = objectMapper.writeValueAsString(response);
                mqttClientService.sendMessage("test/return", jsonResponse);
            }
        } catch (Exception e) {
            logger.error("处理消息时出错", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 0);
            errorResponse.put("message", "处理消息失败: " + e.getMessage());
            errorResponse.put("data", "");

            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            mqttClientService.sendMessage("test/return", jsonResponse);
        }
    }



    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 消息发送完成回调
    }





    // 保存图片到本地文件系统
    private String saveImageToFile(byte[] imageData) throws IOException {
        // 使用配置的上传目录（可以从配置文件读取）
        String imageUploadPath = System.getProperty("user.home") + "/shiwujie/images";

        // 生成唯一文件名
        String fileName = "mqtt_image_" + System.currentTimeMillis() + ".png";
        String filePath = imageUploadPath + File.separator + fileName;

        // 确保目录存在
        File directory = new File(imageUploadPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 写入文件
        FileUtil.writeBytes(imageData, filePath);
        logger.info("图片已保存至: {}", filePath);

        return filePath;
    }

    // 调用AI服务处理图片
    private String processImageWithAI(String imagePath) {
        try {
            // 使用默认盲人ID或者其他标识符
            Long defaultBlindId = 0L; // 或者从配置中读取

            // 调用现有的AI处理逻辑
            String result = imageApp.doChatWithImage(imagePath, defaultBlindId);

            // 记录AI返回的结果
            return result;

        } catch (Exception e) {
            logger.error("AI处理图片失败", e);
            return "AI处理图片失败: " + e.getMessage();
        }
    }


}
