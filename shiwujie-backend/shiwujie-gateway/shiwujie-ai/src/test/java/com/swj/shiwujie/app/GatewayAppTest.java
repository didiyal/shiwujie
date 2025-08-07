package com.swj.shiwujie.app;

import com.swj.shiwujie.model.request.ai.GateWayImageRequest;
import com.swj.shiwujie.model.request.ai.GateWayTextRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@SpringBootTest
class GatewayAppTest {


    @Resource
    private GatewayApp gatewayApp;

    @Test
    void analysisText() {
        GateWayTextRequest gateWayTextRequest = new GateWayTextRequest("你叫什么名字",10000L);
        log.info("question: "+gateWayTextRequest.getText());
        String s = gatewayApp.analysisText(gateWayTextRequest);
        log.info("answer: "+s);

        GateWayImageRequest gateWayImageRequest = new GateWayImageRequest("https://img1.baidu.com/it/u=2114716004,220994554&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=666",10000L);
        log.info("question: 图像识别"+gateWayImageRequest.getImageUrl());
        s = gatewayApp.analysisImage(gateWayImageRequest);
        log.info("answer: "+s);

        gateWayTextRequest.setText("我刚刚问了几个问题,给出问题的数量");
        log.info("question: "+gateWayTextRequest.getText());
        s = gatewayApp.analysisText(gateWayTextRequest);
        log.info("answer: "+s);

    }
}