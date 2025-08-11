package com.swj.shiwujie;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
@MapperScan("com.swj.shiwujie.mapper")
public class ShiwujieAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiwujieAiApplication.class, args);
    }

}
