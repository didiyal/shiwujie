package com.swj.shiwujie.mcp;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.enums.user.GenderEnum;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.user.InnerFamilyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 5 chunk-2b / 2b-3b：业务 MCP 工具真身（缝 C Java 侧）。
 *
 * <p>4 个业务工具经 blind_id（{@link BlindMcpContext} 从 {@code X-Blind-Id} header 取）调用真实业务 service：
 * <ul>
 *   <li>{@code join_family} / {@code leave_family} / {@code family_info} → {@link InnerFamilyService}（家庭三件套）</li>
 *   <li>{@code update_profile} → {@link BlindService}（仅 nickname/phone/gender 白名单字段）</li>
 * </ul>
 *
 * <p><b>工具名 / 参数名用 snake_case</b>（@Tool name + 方法参数名）：对齐 Python 侧
 * {@code tools/java_mcp.py} 的 spike schema + design + FC 测试基线（snake_case），让 chunk-2c 接真时
 * Python {@code get_tools()} 拿到的 schema 与 spike 零漂移。Java 方法名保持 camelCase（注册时取 @Tool name）。
 *
 * <p>blind_id 取法（与旧 {@code UserTools} 的 {@code LoginUtils.getLoginBlind()} 等价但线程模型无关）：
 * {@code BlindMcpContext.blindId(toolContext)} → {@code blindService.getById} → {@link Blind}。
 *
 * <p><b>encode-不抛</b>（design ⑫ Pi 金标准契约）：任何失败（无身份 / 参数错 / 业务异常）都**不抛**，
 * 返回 {@code status:error} 文案的 JSON，让 agent 换路 / 告用户——绝不杀 graph。
 *
 * <p><b>update_profile 字段门</b>（design ⑬ 硬修正 2）：**结构性只暴露 nickname/phone/gender**——
 * 不走 {@code BlindService.updateBlind}（它收 idCard/disabilityCard 会被 MD5 入库，违反字段门），
 * 改 {@code getById} + 仅 set 白名单 + {@code updateById}：idCard/disabilityCard/password 查出来是原值、
 * 不 set 即不变。单测 {@code BusinessMcpToolsTest} 反射断言参数白名单（防约定腐烂）。
 *
 * <p>gender 为 String（对齐 Python 侧 {@code tools/java_mcp.py} 的 {@code gender: Optional[str]}），
 * 内部 {@link #parseGender} 宽松解析「男 / 女 / male / female / 0 / 1」→ {@link GenderEnum} content。
 */
@Component
@Slf4j
public class BusinessMcpTools {

    @Resource
    private BlindService blindService;

    @Resource
    private InnerFamilyService innerFamilyService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Tool(name = "join_family", description = "申请加入家庭（盲人加入志愿者所在家庭）。需提供家庭创建人（志愿者）的手机号。"
            + "加入后即隶属该家庭，可发起 / 接收家庭紧急求助。")
    public String joinFamily(
            ToolContext toolContext,
            @ToolParam(description = "家庭创建人（志愿者）的手机号") String family_phone) {
        if (StrUtil.isBlank(family_phone)) {
            return err("请提供家庭创建人的手机号");
        }
        Long blindId = BlindMcpContext.blindId(toolContext).orElse(null);
        if (blindId == null) {
            return noIdentity();
        }
        try {
            Blind blind = blindService.getById(blindId);
            if (blind == null) {
                return err("未找到当前用户");
            }
            if (ObjUtil.isNotNull(blind.getFamilyId())) {
                return err("您已加入家庭，无需重复加入");
            }
            boolean ok = innerFamilyService.joinFamily(family_phone, blindId, null, blind.getPhone());
            return ok ? ok("已申请加入家庭，等待家庭创建人审核")
                    : err("申请加入家庭失败");
        } catch (Exception e) {
            log.warn("joinFamily 失败 blindId={}", blindId, e);
            return err("申请加入家庭失败：" + safeMsg(e));
        }
    }

    @Tool(name = "leave_family", description = "退出当前所在的家庭。")
    public String leaveFamily(ToolContext toolContext) {
        Long blindId = BlindMcpContext.blindId(toolContext).orElse(null);
        if (blindId == null) {
            return noIdentity();
        }
        try {
            Blind blind = blindService.getById(blindId);
            if (blind == null) {
                return err("未找到当前用户");
            }
            if (ObjUtil.isNull(blind.getFamilyId())) {
                return err("您当前未加入任何家庭");
            }
            boolean ok = innerFamilyService.userLeaveFromFamily(blindId, null, blind.getPhone());
            return ok ? ok("已退出家庭") : err("退出家庭失败");
        } catch (Exception e) {
            log.warn("leaveFamily 失败 blindId={}", blindId, e);
            return err("退出家庭失败：" + safeMsg(e));
        }
    }

    @Tool(name = "family_info", description = "查询当前盲人所在家庭信息（家庭名称、成员、关系）。")
    public String familyInfo(ToolContext toolContext) {
        Long blindId = BlindMcpContext.blindId(toolContext).orElse(null);
        if (blindId == null) {
            return noIdentity();
        }
        try {
            Blind blind = blindService.getById(blindId);
            if (blind == null) {
                return err("未找到当前用户");
            }
            if (ObjUtil.isNull(blind.getFamilyId())) {
                return err("您当前未加入任何家庭");
            }
            FamilyVO vo = innerFamilyService.getFamilyVOById(blind.getFamilyId(), blind.getPhone());
            if (vo == null) {
                return err("未查询到家庭信息");
            }

            Map<String, Object> family = new LinkedHashMap<>();
            family.put("familyName", vo.getFamilyName());
            family.put("familyDescription", vo.getFamilyDescription());
            VolunteerVO creator = vo.getCreatorVolunteer();
            family.put("creatorName", creator == null ? null : creator.getName());

            List<Map<String, Object>> members = new ArrayList<>();
            if (vo.getBlindVOList() != null) {
                for (BlindVO b : vo.getBlindVOList()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", b.getName());
                    m.put("role", "盲人");
                    m.put("isMe", blindId.equals(b.getBlindId()));
                    members.add(m);
                }
            }
            if (vo.getVolunteerVOList() != null) {
                for (VolunteerVO v : vo.getVolunteerVOList()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", v.getName());
                    m.put("role", "家属");
                    members.add(m);
                }
            }
            family.put("members", members);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "ok");
            data.put("family", family);
            return json(data);
        } catch (Exception e) {
            log.warn("familyInfo 失败 blindId={}", blindId, e);
            return err("查询家庭信息失败：" + safeMsg(e));
        }
    }

    @Tool(name = "update_profile", description = "更新盲人个人基本资料。"
            + "⚠️ 仅可更新 nickname 昵称 / phone 手机号 / gender 性别 三个基本字段；"
            + "password 密码 / 身份证号 / 残疾证号等敏感字段严禁通过本工具，须引导用户走专门入口。"
            + "gender 接受「男」「女」（或 male / female）。")
    public String updateProfile(
            ToolContext toolContext,
            @ToolParam(description = "昵称（可空=不更新）", required = false) String nickname,
            @ToolParam(description = "手机号（可空=不更新）", required = false) String phone,
            @ToolParam(description = "性别：男 / 女（可空=不更新）", required = false) String gender) {
        // 输入校验在前（参数格式错不依赖身份，可快速失败 + 便于单测）
        if (StrUtil.isBlank(nickname) && StrUtil.isBlank(phone) && StrUtil.isBlank(gender)) {
            return err("未提供任何要更新的字段（仅支持 nickname / phone / gender）");
        }
        if (StrUtil.isNotBlank(phone) && !PhoneUtil.isPhone(phone)) {
            return err("手机号格式错误");
        }
        Integer genderVal = null;
        if (StrUtil.isNotBlank(gender)) {
            genderVal = parseGender(gender);
            if (genderVal == null) {
                return err("性别只能是「男」或「女」");
            }
        }
        Long blindId = BlindMcpContext.blindId(toolContext).orElse(null);
        if (blindId == null) {
            return noIdentity();
        }
        try {
            Blind blind = blindService.getById(blindId);
            if (blind == null) {
                return err("未找到当前用户");
            }
            // ⚠️ 仅 set 白名单字段——绝不 set idCard / disabilityCard / password
            // （idCard/disabilityCard 走 BlindService.updateBlind 会 MD5 入库，本工具结构性排除）
            if (StrUtil.isNotBlank(nickname)) {
                blind.setName(nickname);
            }
            if (StrUtil.isNotBlank(phone)) {
                blind.setPhone(phone);
            }
            if (genderVal != null) {
                blind.setGender(genderVal);
            }
            boolean ok = blindService.updateById(blind);
            return ok ? ok("资料已更新") : err("资料更新失败");
        } catch (Exception e) {
            log.warn("updateProfile 失败 blindId={}", blindId, e);
            return err("资料更新失败：" + safeMsg(e));
        }
    }

    // ─────────────── helpers ───────────────

    /**
     * gender 字符串 → {@link GenderEnum} content；非法 → null。
     * 宽松解析「男 / 女 / male / female / m / f / 0 / 1」。
     */
    private Integer parseGender(String gender) {
        String g = gender.trim();
        String low = g.toLowerCase();
        if ("男".equals(g) || "male".equals(low) || "m".equals(low) || "0".equals(g)) {
            return GenderEnum.MAN.getContent();
        }
        if ("女".equals(g) || "female".equals(low) || "f".equals(low) || "1".equals(g)) {
            return GenderEnum.WOMEN.getContent();
        }
        return null;
    }

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
