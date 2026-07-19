package com.swj.shiwujie.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP server transport 配置（design 缝 C · blind_id 经 HTTP header 传递，2b-3）。
 *
 * <p>Spring AI 1.1.0 streamable-HTTP MCP server 的 @Tool 方法**不在 DispatcherServlet 线程**执行
 * （走 SSE 异步回调），故 {@code RequestContextHolder} 不可用。官方指定桥接位点 = MCP Java SDK
 * （spring-ai 1.1.0 锁定 0.16.0）的 {@link McpTransportContextExtractor}：transport 层抓 HTTP header
 * → {@link McpTransportContext} → 经 reactor context 透传 → Spring AI 桥进 ToolContext
 * （{@code McpToolUtils.getMcpExchange}），@Tool 经 {@link BlindMcpContext#blindId} 取出。</p>
 *
 * <p>Spring AI autoconfigure 默认注册一个 contextExtractor 返回 {@code EMPTY} 的 transport bean
 * （{@code @ConditionalOnMissingBean}）。本配置以同类型 bean 顶掉它，注入从 {@code X-Blind-Id} /
 * {@code X-Internal-Secret} 提取上下文的 extractor。Python 侧 langchain-mcp-adapters 0.3.0 每次
 * JSON-RPC POST 都带这两个 header（httpx client-level 默认头）。</p>
 *
 * <p>2b-3a 端到端验通：whoami spike 工具读 {@code transportContext.get("blind_id")} 返回 Python 传入值
 * （见 {@code shiwujie-ai} 端 spike 脚本）；whoami 随业务工具真身落地已移除（生产 LLM 不应见调试工具）。
 * 生产业务（{@link BusinessMcpTools}）/ 信令（{@link SignalMcpTools}）工具经同一 {@link BlindMcpContext} 取 blind_id。</p>
 */
@Configuration
public class McpTransportConfig {

    /** transportContext key：当前盲人 id（来自 HTTP header {@code X-Blind-Id}）。 */
    public static final String KEY_BLIND_ID = "blind_id";

    /** transportContext key：Java↔Python 内部互信密钥（来自 HTTP header {@code X-Internal-Secret}）。 */
    public static final String KEY_INTERNAL_SECRET = "internal_secret";

    /**
     * 顶掉 Spring AI autoconfigure 的默认 transport bean（{@code @ConditionalOnMissingBean} 按类型），
     * 注入带 header 提取器的 streamable-HTTP transport。endpoint 沿用 {@code /mcp}（与 yml / Spike-2 一致）。
     */
    @Bean
    public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcp")
                .contextExtractor(extractBlindHeaders())
                .build();
    }

    private McpTransportContextExtractor<ServerRequest> extractBlindHeaders() {
        return serverRequest -> {
            Map<String, Object> ctx = new HashMap<>(4);
            ctx.put(KEY_BLIND_ID, serverRequest.headers().firstHeader("X-Blind-Id"));
            ctx.put(KEY_INTERNAL_SECRET, serverRequest.headers().firstHeader("X-Internal-Secret"));
            return McpTransportContext.create(ctx);
        };
    }
}
