package com.swj.shiwujie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 启动真实嵌入式 Tomcat（RANDOM_PORT），以提供 WebSocket 所需的 javax.websocket ServerContainer；
// 默认 MOCK 环境不会启动容器，ServerEndpointExporter 会因 ServerContainer 不可用而初始化失败
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "dubbo.consumer.check=false")
class CallApplicationTests {

    @Test
    void contextLoads() {
    }

}
