package com.swj.shiwujie.mcp;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * MCP 工具的「当前盲人」上下文（design 缝 C · blind_id 经 transportContext 传递，2b-3）。
 *
 * <p>每个 @Tool 方法接收 {@link ToolContext}（Spring AI 在 MCP bridge 转换时注入 exchange）→
 * {@link McpToolUtils#getMcpExchange(ToolContext)} 取 {@code McpSyncServerExchange} →
 * {@code transportContext().get(KEY_BLIND_ID)} 读由 {@link McpTransportConfig} 的 extractor
 * 从 {@code X-Blind-Id} / {@code X-Issuing-Turn} header 填入的 blind_id / 当前轮次。</p>
 *
 * <p>取不到（exchange 未注入 / header 缺 / 非数字）→ empty。调用方（业务/信令工具）应判空并 encode
 * 友好错误回灌 agent（design ⑫ encode-不抛的 Java 等价：不抛、返回 isError 文案让 agent 换路/告用户）。</p>
 *
 * <p><b>issuing_turn 与 fail-closed</b>（design ⑬ 硬修正 1，红队 Q18 紧急求助 turn-bound token）：
 * {@link #issuingTurn} 取不到时调用方应降级为 turn=0——prepare 存 turn=0、confirm 校验 turn=0，
 * 同轮必拒，故 **turn 未跨进程传时 confirm 永拒、绝不误发 WS 5003**（盲人安全优先）。turn 概念来自
 * Python graph（service 层自推 HumanMessage 数+1，见 {@code shiwujie-ai/service/app.py}），经 MCP header
 * 透传；chunk-2a Python 走 stub（不发 header），chunk-2c 真 MCP client 接 per-turn header。</p>
 *
 * <p>并发安全：extractor 每请求新建 immutable {@link McpTransportContext}，reactor context per-subscription，
 * 多 blind_id 并发不串读（优于 ThreadLocal）。</p>
 */
public final class BlindMcpContext {

    private BlindMcpContext() {
    }

    /**
     * 取当前 blind_id；ToolContext 为空 / exchange 未注入 / header 缺 / 非数字 → empty。
     */
    public static Optional<Long> blindId(ToolContext toolContext) {
        if (toolContext == null) {
            return Optional.empty();
        }
        return McpToolUtils.getMcpExchange(toolContext)
                .map(ex -> ex.transportContext().get(McpTransportConfig.KEY_BLIND_ID))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .flatMap(BlindMcpContext::tryParseLong);
    }

    /**
     * 取当前 issuing_turn（design ⑬ 红队 Q18 紧急求助 turn-bound token 判据）；缺失 → empty。
     *
     * <p>调用方缺失时降级 turn=0（fail-closed：confirm 因同轮被拒）。详见类 javadoc。
     */
    public static Optional<Integer> issuingTurn(ToolContext toolContext) {
        if (toolContext == null) {
            return Optional.empty();
        }
        return McpToolUtils.getMcpExchange(toolContext)
                .map(ex -> ex.transportContext().get(McpTransportConfig.KEY_ISSUING_TURN))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .flatMap(BlindMcpContext::tryParseInt);
    }

    private static Optional<Long> tryParseLong(String s) {
        try {
            return Optional.of(Long.parseLong(s.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> tryParseInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
