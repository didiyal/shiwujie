package com.swj.shiwujie.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class GatewayAppTest {


    @Resource
    private GatewayApp gatewayApp;


    @Test
    void doChatWithText() {
        gatewayApp.doChatWithText("你好,我叫李四",10000L);
        gatewayApp.doChatWithText("我想要买锅,一个菜刀",10000L);
        gatewayApp.doChatWithText("我想干什么来着",10000L);
    }

    @Test
    void doChatWithImage() {
        gatewayApp.doChatWithText("你好,我叫李四",10000L);
        gatewayApp.doChatWithText("我想要买锅,一个菜刀",10000L);
        gatewayApp.doChatWithText("我想干什么来着",10000L);
        gatewayApp.doChatWithImage("我叫什么来着,我想识别图片","https://java2ai.com/img/user/ai/tutorials/multimodality/multimodal.test.png",10000L);
    }
}