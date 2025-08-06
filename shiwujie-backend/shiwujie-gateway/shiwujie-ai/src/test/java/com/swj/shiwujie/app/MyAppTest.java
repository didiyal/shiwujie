package com.swj.shiwujie.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MyAppTest {



    @Resource
    private MyApp myApp;

    @Test
    void doChatWithText() {

        myApp.doChatWithText("给我介绍一下这个app的功能",10000L);

    }

    @Test
    void doChatWithImage() {

        myApp.doChatWithImage("我要识别","https://img1.baidu.com/it/u=2114716004,220994554&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=666",10000L);
    }
}