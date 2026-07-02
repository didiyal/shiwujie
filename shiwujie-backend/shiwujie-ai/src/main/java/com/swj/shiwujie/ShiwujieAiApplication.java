package com.swj.shiwujie;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDubbo // 启动dubbo
@MapperScan("com.swj.shiwujie.mapper")
@EnableAsync // 启动异步任务
@EnableDiscoveryClient // 启用服务发现
public class ShiwujieAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiwujieAiApplication.class, args);
    }

}
