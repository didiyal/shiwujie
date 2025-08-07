package com.swj.shiwujie.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EasyProblemAppTest {



    @Resource
    private EasyProblemApp easyProblemApp;

    @Test
    void doChatWithText() {

        easyProblemApp.doChatWithText("我想要加入家庭",10000L);
//        easyProblemApp.doChatWithText("我要知道这个网页有什么内容https://blog.csdn.net/m0_37145844/article/details/146019608",10000L);
        easyProblemApp.doChatWithText("家主手机号是17745540159",10000L);
//        myApp.doChatWithText("我想知道今天宜昌的天气如何",10000L);

    }

    @Test
    void doChatWithImage() {

        easyProblemApp.doChatWithImage("https://img1.baidu.com/it/u=2114716004,220994554&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=666",10000L);
    }
}