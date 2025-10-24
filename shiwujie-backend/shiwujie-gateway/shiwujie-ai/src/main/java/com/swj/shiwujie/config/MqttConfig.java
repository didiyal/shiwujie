package com.swj.shiwujie.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT连接配置类
 * 提供MQTT客户端连接选项的配置
 */
@Configuration
public class MqttConfig {

    /**
     * MQTT服务器URI，从配置文件读取，默认为tcp://localhost:1883
     */
    @Value("${mqtt.server.uri:tcp://localhost:1883}")
    private String serverUri;

    /**
     * 创建MQTT连接选项Bean
     * @return 配置好的MqttConnectOptions实例
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{serverUri});
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(20);
        return options;
    }
}
