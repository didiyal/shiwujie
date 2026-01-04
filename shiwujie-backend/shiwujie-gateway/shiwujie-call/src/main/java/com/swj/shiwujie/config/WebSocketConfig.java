package com.swj.shiwujie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 * 用于配置Spring WebSocket的核心组件
 */
@Configuration
public class WebSocketConfig {

    /**
     * 注册ServerEndpointExporter，用于扫描和注册WebSocket端点
     * @return ServerEndpointExporter实例
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}