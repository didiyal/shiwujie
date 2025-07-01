package com.swj.shiwujie;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@Slf4j
@EnableDubbo
public class ShiwujieUserApplication {

    public static void main(String[] args)  {
        SpringApplication.run(ShiwujieUserApplication.class, args);


    }



}
