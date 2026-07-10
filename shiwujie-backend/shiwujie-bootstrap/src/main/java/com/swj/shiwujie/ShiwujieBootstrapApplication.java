package com.swj.shiwujie;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 视无界单体唯一启动入口（v3.0.0 单体化）。
 * <p>
 * 聚合 user / call / community / ai 四模块为单进程：
 * <ul>
 *   <li>{@code scanBasePackages = "com.swj.shiwujie"} 覆盖四模块全部 Bean（含 call 的 WebSocketConfig / ServerEndpointExporter）。</li>
 *   <li>{@code @MapperScan("com.swj.shiwujie.mapper")} —— 四模块 13 个 Mapper 接口同在此包（且均带 @Mapper）。</li>
 *   <li>{@code @EnableAsync} —— ai 的 TextAppChatMemoryRes 异步持久化需要。</li>
 * </ul>
 * 原 user/call/community/ai 四个 Application 类已删；本类是唯一 @SpringBootApplication。
 */
@SpringBootApplication(scanBasePackages = "com.swj.shiwujie")
@MapperScan("com.swj.shiwujie.mapper")
@EnableAsync
public class ShiwujieBootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiwujieBootstrapApplication.class, args);
    }

}
