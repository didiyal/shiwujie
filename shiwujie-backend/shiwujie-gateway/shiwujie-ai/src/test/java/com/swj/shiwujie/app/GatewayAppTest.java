package com.swj.shiwujie.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class GatewayAppTest {


    @Resource
    private GatewayApp gatewayApp;

    @Test
    void analysisText() {
        gatewayApp.analysisText("这是复杂的问题",10000L);
        gatewayApp.analysisText("这是简单的问题",10000L);
        gatewayApp.analysisImage("https://img1.baidu.com/it/u=2114716004,220994554&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=666",10000L);
        gatewayApp.analysisText("我刚刚问了几个问题,现在调用工具,并给出结果",10000L);
    }
}