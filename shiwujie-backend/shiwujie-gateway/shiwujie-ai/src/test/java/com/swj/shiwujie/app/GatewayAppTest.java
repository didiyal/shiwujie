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
        log.info("question: "+"你叫什么名字");
        String s = gatewayApp.analysisText("你叫什么名字");
        log.info("answer: "+s);

        log.info("question: "+"我刚刚问了几个问题,给出问题的数量");
        s = gatewayApp.analysisText("我刚刚问了几个问题,给出问题的数量");
        log.info("answer: "+s);

    }
}