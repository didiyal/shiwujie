package com.swj.shiwujie.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Phase 5 chunk-1 Spike-2：Java MCP server 暴露的 8 工具桩（缝 C）。
 *
 * 仅验证 polyglot MCP 缝（spring-ai-starter-mcp-server-webmvc streamable-HTTP +
 * Python langchain-mcp-adapters MultiServerMCPClient.get_tools() round-trip）——
 * 方法体返固定 JSON，不调真实业务/WS。生产实现（→ InnerFamilyService / BlindService /
 * InnerSocket.noticeXxx）见 shiwujie-ai/docs/design.md §3.7。
 *
 * 工具名/description/参数对齐生产意图并内嵌护栏（update_profile 仅基本字段、open_app 白名单、
 * emergency 须确认），便于 Python 侧 get_tools() 看到真实形态（spike 不验护栏语义，只验管道）。
 *
 * 注：用 @Tool + ToolCallbackProvider（见 SpikeMcpConfig），避 @McpTool 触 #4392。
 */
@Component
public class SpikeMcpTools {

    @Tool(description = "申请加入家庭（盲人加入志愿者所在家庭）。需提供家庭邀请码或家属手机号。")
    public String joinFamily(
            @ToolParam(description = "家庭邀请码或家属手机号") String invite) {
        return "{\"status\":\"ok\",\"message\":\"已申请加入家庭\"}";
    }

    @Tool(description = "退出当前家庭。")
    public String leaveFamily() {
        return "{\"status\":\"ok\",\"message\":\"已退出家庭\"}";
    }

    @Tool(description = "查询当前盲人所在家庭信息（家庭成员、关系）。")
    public String familyInfo() {
        return "{\"family\":\"示例家庭\",\"members\":[{\"name\":\"张三\",\"role\":\"家属\"}]}";
    }

    @Tool(description = "更新盲人个人基本资料。"
            + "⚠️ 仅可更新 nickname/phone/gender 三个基本字段；"
            + "password/身份证号/残疾证号等敏感字段严禁通过本工具，须引导用户走专门入口。")
    public String updateProfile(
            @ToolParam(description = "昵称（可空=不更新）", required = false) String nickname,
            @ToolParam(description = "手机号（可空=不更新）", required = false) String phone,
            @ToolParam(description = "性别（可空=不更新）", required = false) String gender) {
        return "{\"status\":\"ok\",\"message\":\"资料已更新\"}";
    }

    @Tool(description = "在手机端启动高德导航到指定目的地（推送 WS 5006 信令）。mode 为交通方式。")
    public String launchNavigation(
            @ToolParam(description = "目的地名称") String destination,
            @ToolParam(description = "交通方式：walking 步行 / transit 公交 / driving 驾车") String mode) {
        return "{\"status\":\"ok\",\"message\":\"导航已开始\"}";
    }

    @Tool(description = "请求志愿者视频协助（推送 WS 5002 信令），用于非紧急的视觉帮助需求。")
    public String requestVideoHelp() {
        return "{\"status\":\"ok\",\"message\":\"正在为您匹配志愿者\"}";
    }

    @Tool(description = "请求紧急求助（推送 WS 5003 信令，通知所有家属）。"
            + "⚠️ 仅限真实紧急情况；调用前必须先向用户确认「确认发起紧急求助？」；"
            + "非紧急需求改用 requestVideoHelp。")
    public String requestEmergencyHelp() {
        return "{\"status\":\"ok\",\"message\":\"已通知所有家属\"}";
    }

    @Tool(description = "打开手机应用（推送 WS 5004 信令）。"
            + "⚠️ 仅限白名单应用（通讯/生活类，如电话/短信/微信/高德地图）；非白名单应用拒绝打开。")
    public String openApp(
            @ToolParam(description = "应用名称（须在白名单内）") String appName) {
        return "{\"status\":\"ok\",\"message\":\"已打开应用：" + appName + "\"}";
    }
}
