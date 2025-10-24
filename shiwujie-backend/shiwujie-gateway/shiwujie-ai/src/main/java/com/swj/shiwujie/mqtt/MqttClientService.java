package com.swj.shiwujie.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MqttClientService {

    private static final Logger logger = LoggerFactory.getLogger(MqttClientService.class);
    private static final String CLIENT_ID = "SpringBootMQTTReceiver";
    
    @Autowired
    private MqttConnectOptions mqttConnectOptions;
    
    @Autowired
    private MqttMessageHandler mqttMessageHandler;
    
    private MqttClient mqttClient;

    @PostConstruct
    public void connect() {
        try {
            mqttClient = new MqttClient(
                    mqttConnectOptions.getServerURIs()[0],
                    CLIENT_ID,
                    new MemoryPersistence()
            );
            mqttClient.setCallback(mqttMessageHandler);
            // 设置反向引用
            mqttMessageHandler.setMqttClientService(this);
            mqttClient.connect(mqttConnectOptions);

            // 订阅主题
            mqttClient.subscribe("test/topic", 1);
            logger.info("Successfully connected and subscribed to topic: test/topic");
        } catch (MqttException e) {
            logger.error("Failed to connect to MQTT broker at {}",
                    mqttConnectOptions.getServerURIs()[0], e);
        }
    }

    // 添加发送消息的方法
    public void sendMessage(String topic, String message) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                // 使用明确的UTF-8编码转换字符串为字节
                MqttMessage mqttMessage = new MqttMessage(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                mqttMessage.setQos(1);
                mqttClient.publish(topic, mqttMessage);
                logger.info("Message sent to topic '{}': {}", topic, message);
            } else {
                logger.warn("MQTT client is not connected. Cannot send message.");
            }
        } catch (MqttException e) {
            logger.error("Failed to send message to topic '{}'", topic, e);
        }
    }


    // 提供获取客户端连接状态的方法
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }
    @PreDestroy
    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                logger.info("Disconnected from MQTT broker");
            }
        } catch (MqttException e) {
            logger.error("Error while disconnecting from MQTT broker", e);
        }
    }
}
