package com.swj.shiwujie.mcp;

import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 5 chunk-1 Spike-2：把 SpikeMcpTools 的 8 个 @Tool 方法注册成 MCP server 工具。
 *
 * Spring AI 1.1.0 重构后**无 ToolCallbacks 工具类**——改用 MethodToolCallbackProvider
 * （实现 ToolCallbackProvider）：builder().toolObjects(带 @Tool 方法的 bean).build() 扫注解生成 callback。
 * MCP server boot starter 自动探测 ToolCallbackProvider bean 注册为 MCP 工具
 * （见 Spring AI 文档 mcp-server-boot-starter）。SYNC server 把同步 @Tool 方法转 sync 规格。
 */
@Configuration
public class SpikeMcpConfig {

    @Bean
    public MethodToolCallbackProvider spikeMcpToolCallbacks(SpikeMcpTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
