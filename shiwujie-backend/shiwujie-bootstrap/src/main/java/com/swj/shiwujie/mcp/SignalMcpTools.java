package com.swj.shiwujie.mcp;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.request.call.Destination;
import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.call.InnerSocket;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Phase 5 chunk-2b / 2b-4a：信令 MCP 工具（缝 C Java 侧，推 WS 信令给 App）。
 *
 * <p>3 个信令工具真身经 blind_id（{@link BlindMcpContext}）解析当前盲人 phone，组 {@link SocketData}
 * 推 WS 信令（{@link InnerSocket}→{@code CoordinationSocketHandler}→App）：
 * <ul>
 *   <li>{@code request_video_help} → noticeVideoHelp（WS 5002）</li>
 *   <li>{@code open_app} → noticeJumpSoftware（WS 5004），⚠️ 白名单硬卡（design ⑬）</li>
 *   <li>{@code launch_navigation} → noticeNavigation（WS 5006），结构化 destination{name,lat,lng,address}
 *       （chunk-2e-1 三端对齐：替代旧 volunteerPhone hack，Android 5006 读 destination.name 起高德导航）</li>
 * </ul>
 * {@code request_emergency_help}（5003）拆 {@code request_emergency_help_prepare}（签 turn-bound token，
 * 不推 WS）+ {@code request_emergency_help_confirm}（校验 token 跨轮通过才推 WS 5003，design ⑬ 红队 Q18
 * 三道闸之 Java 侧闸 ②；gate ① Python parallel_tool_calls=False / gate ③ App 显式确认面留 2e）。
 *
 * <p><b>工具名 / 参数名 snake_case</b>（@Tool name + 方法参数名）：对齐 Python 侧
 * {@code tools/java_mcp.py} spike schema + design + FC 测试基线（与 {@link BusinessMcpTools} 同）。
 * Java 方法名保持 camelCase（注册时取 @Tool name）。
 *
 * <p>blind_id 取法：{@link #resolveBlindId} 包 {@link BlindMcpContext#blindId} 静态调用——
 * <b>protected 便单测 subclass override</b>（不引 mockito-inline 测 service 推送路径）。
 * phone→session 查找由 {@code CoordinationSocketHandler} 按 blindPhone 做（WS 会话键=phone）。
 *
 * <p><b>盲人在线性</b>：信令工具执行时盲人 WS 必然在线——AI turn 本身走 WS（缝 A，
 * {@code AiWsRelayService}），WS 断则 turn 不触发、信令工具不会被调。{@code CoordinationSocketHandler.noticeXxx}
 * 的「盲人不在线」分支是 turn 中途断连的防御性 log（design ⑫ encode-不抛——noticeXxx 返 void
 * 不报送达状态，调用方据「执行即在线」假设 ok）。
 *
 * <p><b>encode-不抛</b>（design ⑫）：无身份 / 盲人不存在 / 白名单外 / 异常都返 {@code status:error} JSON，绝不杀 graph。
 *
 * <p><b>open_app 白名单</b>（design ⑬ 硬修正 + Phase 1）：仅通讯 / 生活类（电话 / 短信 /
 * 微信 / 高德地图 / 通讯录），非白名单拒绝——盲人安全护栏，Java 侧硬卡不靠 prompt。
 */
@Component
@Slf4j
public class SignalMcpTools {

    @Resource
    private BlindService blindService;

    @Resource
    private InnerSocket innerSocket;

    @Resource
    private EmergencyTokenStore emergencyTokenStore;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** open_app 白名单关键词（normalize 后 contains 匹配，中英大小写不敏感）。 */
    private static final Set<String> APP_WHITELIST_KEYWORDS = Set.of(
            "电话", "拨号", "phone", "dial",
            "短信", "信息", "message", "sms",
            "微信", "wechat",
            "高德", "地图", "amap", "map",
            "通讯录", "联系人", "contact");

    // ─────────────── 信令工具 ───────────────

    @Tool(name = "request_video_help", description = "请求志愿者视频协助（推送 WS 5002 信令），用于非紧急的视觉帮助需求。")
    public String requestVideoHelp(ToolContext toolContext) {
        return pushSignal(toolContext, "视频求助", phone -> {
            SocketData d = newSocketData(phone, 5002);
            innerSocket.noticeVideoHelp(d);
        }, "正在为您匹配志愿者", "请求视频协助失败");
    }

    @Tool(name = "open_app", description = "打开手机应用（推送 WS 5004 信令）。"
            + "⚠️ 仅限白名单应用（通讯 / 生活类：电话 / 短信 / 微信 / 高德地图 / 通讯录）；非白名单应用拒绝打开。")
    public String openApp(ToolContext toolContext,
            @ToolParam(description = "应用名称（须在白名单内）") String app_name) {
        if (StrUtil.isBlank(app_name)) {
            return err("请提供要打开的应用名称");
        }
        if (!isWhitelistedApp(app_name)) {
            return err("「" + app_name + "」不在白名单内（仅支持电话 / 短信 / 微信 / 高德地图 / 通讯录），已拒绝打开");
        }
        return pushSignal(toolContext, "跳转应用", phone -> {
            SocketData d = newSocketData(phone, 5004);
            d.setVolunteerPhone(app_name); // 旧 hack：appName 塞 volunteerPhone（Android 5004 读它当 appName）
            innerSocket.noticeJumpSoftware(d);
        }, "已打开应用：" + app_name, "打开应用失败");
    }

    @Tool(name = "launch_navigation", description = "在手机端启动高德导航到指定目的地（推送 WS 5006 信令）。"
            + "destination_name 为目的地名称，mode 为交通方式（walking 步行 / transit 公交 / driving 驾车）。")
    public String launchNavigation(ToolContext toolContext,
            @ToolParam(description = "目的地名称") String destination_name,
            @ToolParam(description = "交通方式：walking 步行 / transit 公交 / driving 驾车", required = false) String mode) {
        if (StrUtil.isBlank(destination_name)) {
            return err("请提供导航目的地");
        }
        return pushSignal(toolContext, "导航", phone -> {
            SocketData d = newSocketData(phone, 5006);
            // chunk-2e-1：结构化 destination（替代旧 volunteerPhone hack，与 Android 三端对齐）
            // v1 仅 name（高德 URI 按 name 地理编码即起导航）；lat/lng/address 预留高德 SDK 精确定位
            Destination dest = new Destination();
            dest.setName(destination_name);
            d.setDestination(dest);
            innerSocket.noticeNavigation(d);
        }, "已发起导航至 " + destination_name, "发起导航失败");
    }

    // ─────────────── 紧急求助（design ⑬ 红队 Q18 拆 prepare/confirm + turn-bound token）───────

    @Tool(name = "request_emergency_help_prepare", description = "**仅紧急情况**（受伤、迷路遇险、突发疾病、人身安全受威胁）才用。"
            + "本工具是紧急求助的**第一步：准备**——它生成一个确认码(token)并请你向用户确认，**此时还不会真的通知家属**。"
            + "可选 reason 简述紧急原因。**非紧急**情况（只是需要帮助、看不清东西）请改用 request_video_help，不要用本工具。")
    public String requestEmergencyHelpPrepare(ToolContext toolContext,
            @ToolParam(description = "紧急情况简述，如'我摔倒了'、'胸闷喘不上气'", required = false) String reason) {
        Long blindId = resolveBlindId(toolContext);
        if (blindId == null) {
            return noIdentity();
        }
        int turn = resolveIssuingTurn(toolContext);
        try {
            String token = emergencyTokenStore.issue(blindId, turn);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", "ok");
            m.put("token", token);
            m.put("message", "确认码已生成。请向用户确认后，用本 token 调用 request_emergency_help_confirm。");
            log.info("紧急求助 prepare blindId={} turn={} reason={}", blindId, turn, reason);
            return json(m);
        } catch (Exception e) {
            log.warn("紧急求助 prepare 失败 blindId={} turn={}", blindId, turn, e);
            return err("生成确认码失败：" + safeMsg(e));
        }
    }

    @Tool(name = "request_emergency_help_confirm", description = "紧急求助**第二步：确认**。必须用上一步 request_emergency_help_prepare 返回的 token 才会真正"
            + "通知所有家属（信令5003 群发）。**绝不**在没有先调用 prepare 的情况下凭空捏造 token 调用；也"
            + "**不要在同一轮里**既调用 prepare 又调用 confirm——必须先 prepare 并向用户确认后，等用户下一轮"
            + "明确确认，再用 confirm。token 校验失败会返回 rejected（可向用户解释后重新 prepare）。")
    public String requestEmergencyHelpConfirm(ToolContext toolContext,
            @ToolParam(description = "紧急求助确认码——必须由上一步 request_emergency_help_prepare 返回") String token) {
        if (StrUtil.isBlank(token)) {
            return err("请提供确认码 token（由 request_emergency_help_prepare 返回）");
        }
        Long blindId = resolveBlindId(toolContext);
        if (blindId == null) {
            return noIdentity();
        }
        int turn = resolveIssuingTurn(toolContext);
        EmergencyTokenStore.VerifyResult vr = emergencyTokenStore.verify(token, blindId, turn);
        if (!vr.ok()) {
            log.warn("紧急求助 confirm 拒绝 blindId={} turn={} reason={}", blindId, turn, vr.message());
            // 软拒绝（status=rejected），encode-不抛——agent 读拒绝 message 后向用户解释 / 重新确认。
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", "rejected");
            m.put("message", vr.message());
            return json(m);
        }
        // token 跨轮校验通过 → 推 WS 5003（复用 pushSignal 骨架：取 phone → noticeUrgentHelp）。
        return pushSignal(toolContext, "紧急求助", phone -> {
            SocketData d = newSocketData(phone, 5003);
            innerSocket.noticeUrgentHelp(d);
        }, "紧急求助已发起，已通知所有家属", "发起紧急求助失败");
    }

    // ─────────────── 推送骨架（encode-不抛）────────────────

    /**
     * 信令推送骨架：取身份 → 取 phone → 执行推送动作 → 返 ok/err。
     * 任何失败（无身份 / 盲人不存在 / 无 phone / 异常）都 encode 成 status:error JSON，不抛。
     *
     * @param pushAction 拿到 phone 后的推送动作（组 SocketData + 调 noticeXxx）
     */
    private String pushSignal(ToolContext toolContext, String label,
            Consumer<String> pushAction,
            String successMsg, String failLabel) {
        Long blindId = resolveBlindId(toolContext);
        if (blindId == null) {
            return noIdentity();
        }
        try {
            Blind blind = blindService.getById(blindId);
            if (blind == null) {
                return err("未找到当前用户");
            }
            String phone = blind.getPhone();
            if (StrUtil.isBlank(phone)) {
                return err("当前用户无有效手机号");
            }
            pushAction.accept(phone);
            log.info("信令推送 - {} blindId={}", label, blindId);
            return ok(successMsg);
        } catch (Exception e) {
            log.warn("{}失败 blindId={}", failLabel, blindId, e);
            return err(failLabel + "：" + safeMsg(e));
        }
    }

    private SocketData newSocketData(String blindPhone, int requestType) {
        SocketData d = new SocketData();
        d.setBlindPhone(blindPhone);
        d.setRequestType(requestType);
        return d;
    }

    /** open_app 白名单匹配：normalize（小写 + 去空白）后 contains 任一关键词。 */
    private boolean isWhitelistedApp(String appName) {
        String n = appName.toLowerCase().replaceAll("\\s+", "");
        return APP_WHITELIST_KEYWORDS.stream().anyMatch(kw -> n.contains(kw.toLowerCase()));
    }

    /**
     * blind_id 取法（protected 便单测 subclass override，不引 mockito-inline 测 service 推送路径）。
     */
    protected Long resolveBlindId(ToolContext toolContext) {
        return BlindMcpContext.blindId(toolContext).orElse(null);
    }

    /**
     * issuing_turn 取法（design ⑬ 红队 Q18 紧急求助 turn-bound token 判据）。缺失降级 0（fail-closed：
     * confirm 因同轮被拒，绝不误发 5003）。protected 便单测 subclass override。
     */
    protected int resolveIssuingTurn(ToolContext toolContext) {
        return BlindMcpContext.issuingTurn(toolContext).orElse(0);
    }

    // ─────────────── helpers（与 BusinessMcpTools 同构）───────

    private String ok(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "ok");
        m.put("message", message);
        return json(m);
    }

    private String err(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "error");
        m.put("message", message);
        return json(m);
    }

    private String noIdentity() {
        return err("未识别到当前用户身份（blind_id 缺失）");
    }

    private String json(Map<String, Object> data) {
        try {
            return MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("JSON 序列化失败", e);
            return "{\"status\":\"error\",\"message\":\"内部错误\"}";
        }
    }

    /** 异常 message 转友好文本（防内部堆栈泄给 LLM）。 */
    private String safeMsg(Exception e) {
        String msg = e.getMessage();
        return StrUtil.isBlank(msg) ? e.getClass().getSimpleName() : msg;
    }
}
