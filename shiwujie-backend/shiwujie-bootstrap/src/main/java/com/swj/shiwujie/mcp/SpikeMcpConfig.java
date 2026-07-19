package com.swj.shiwujie.mcp;

import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 5 chunk-2b：注册 MCP server 工具（缝 C Java 侧）。
 *
 * <p>两个 bean 的 @Tool 方法合并注册：
 * <ul>
 *   <li>{@link SpikeMcpTools}：4 信令桩（5002/5003/5004/5006，真身见 2b-4）</li>
 *   <li>{@link BusinessMcpTools}：4 业务真身（join_family/leave_family/family_info/update_profile，2b-3b 落地）</li>
 * </ul>
 * 共 8 工具暴露给 Python 侧 {@code get_tools()}。
 *
 * <p>Spring AI 1.1.0 重构后**无 ToolCallbacks 工具类**——改用 MethodToolCallbackProvider
 * （实现 ToolCallbackProvider）：{@code builder().toolObjects(带 @Tool 方法的 bean...).build()} 扫注解生成 callback。
 * MCP server boot starter 自动探测 ToolCallbackProvider bean 注册为 MCP 工具。SYNC server 把同步 @Tool 方法转 sync 规格。
 */
@Configuration
public class SpikeMcpConfig {

    @Bean
    public MethodToolCallbackProvider spikeMcpToolCallbacks(SpikeMcpTools signalTools, BusinessMcpTools businessTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(signalTools, businessTools)
                .build();
    }
}
