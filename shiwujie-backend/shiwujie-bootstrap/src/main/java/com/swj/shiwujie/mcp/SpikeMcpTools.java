package com.swj.shiwujie.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Phase 5 chunk-2b：信令 MCP 工具桩（缝 C Java 侧，4 个信令 stub）。
 *
 * <p>原 8 工具桩已拆分：业务 4（join_family / leave_family / family_info / update_profile）
 * 已落地真身 → {@link BusinessMcpTools}（2b-3b）；spike 验证用的 whoami（2b-3a header 透传机制）
 * 已端到端验通并移除——生产 LLM 不应见此调试工具。
 *
 * <p>本类剩 4 个**信令桩**——方法体仍返固定 JSON，不推真实 WS 信令。真身（→进程内
 * {@code InnerSocket.noticeXxx} 推 WS 5002/5003/5004/5006）见 chunk-2b / 2b-4：届时加
 * {@code ToolContext} 参数 + {@link BlindMcpContext} 取 blind_id 解析 phone→session，
 * emergency 拆 {@code request_emergency_help_prepare} / {@code _confirm} 双工具（design ⑬ 硬修正 1）。
 *
 * <p><b>工具名用 snake_case</b>（@Tool name）：对齐 Python 侧 {@code tools/java_mcp.py} spike schema +
 * design + FC 测试基线，让 chunk-2c 接真时 Python {@code get_tools()} 拿到的工具名与 spike 零漂移。
 * 信令参数（destination / mode / appName）是临时桩，2b-4 信令真身时按 design 定稿
 * （destination_name / lat / lng / mode / app）。
 *
 * <p>description 内嵌护栏（open_app 白名单 / emergency 须确认），便于 Python 侧 get_tools() 看到真实形态。
 * 用 @Tool + ToolCallbackProvider（见 {@link SpikeMcpConfig}），避 @McpTool 触 #4392。
 */
@Component
public class SpikeMcpTools {

    @Tool(name = "launch_navigation", description = "在手机端启动高德导航到指定目的地（推送 WS 5006 信令）。mode 为交通方式。")
    public String launchNavigation(
            @ToolParam(description = "目的地名称") String destination,
            @ToolParam(description = "交通方式：walking 步行 / transit 公交 / driving 驾车") String mode) {
        return "{\"status\":\"ok\",\"message\":\"导航已开始\"}";
    }

    @Tool(name = "request_video_help", description = "请求志愿者视频协助（推送 WS 5002 信令），用于非紧急的视觉帮助需求。")
    public String requestVideoHelp() {
        return "{\"status\":\"ok\",\"message\":\"正在为您匹配志愿者\"}";
    }

    @Tool(name = "request_emergency_help", description = "请求紧急求助（推送 WS 5003 信令，通知所有家属）。"
            + "⚠️ 仅限真实紧急情况；调用前必须先向用户确认「确认发起紧急求助？」；"
            + "非紧急需求改用 request_video_help。")
    public String requestEmergencyHelp() {
        return "{\"status\":\"ok\",\"message\":\"已通知所有家属\"}";
    }

    @Tool(name = "open_app", description = "打开手机应用（推送 WS 5004 信令）。"
            + "⚠️ 仅限白名单应用（通讯/生活类，如电话/短信/微信/高德地图）；非白名单应用拒绝打开。")
    public String openApp(
            @ToolParam(description = "应用名称（须在白名单内）") String appName) {
        return "{\"status\":\"ok\",\"message\":\"已打开应用：" + appName + "\"}";
    }
}
