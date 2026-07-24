package com.swj.shiwujie.mcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BusinessMcpTools} 单元测试。
 *
 * <p>纯单测（不起 Spring、不连库、不依赖 mockito-inline mock 静态）：聚焦
 * <ul>
 *   <li>design ⑬ 硬修正 2：update_profile 参数白名单结构性排除敏感字段（password/idCard/disabilityCard），
 *       反射断言防约定腐烂；</li>
 *   <li>design ⑫ encode-不抛：无 blind_id / 参数错都返 {@code status:error} JSON，绝不抛（Pi 金标准契约）。</li>
 * </ul>
 *
 * <p>service 调用路径（有 blind_id）需 mock Spring AI 静态 {@code McpToolUtils.getMcpExchange}——
 * 本测不引 mockito-inline，该路径留 chunk-2c 真启动端到端验证（2b-3a whoami 已端到端验过 blind_id 透传，
 * 2b-3b service 接线手测一次即可）。测试用例均在「身份校验 / 参数校验」处 return，不触达 null 的 service 字段。
 *
 * <p>依赖编译器 {@code -parameters}（Spring Boot 3.x 默认开启）以反射真实参数名。
 */
@DisplayName("BusinessMcpTools 业务 MCP 工具")
class BusinessMcpToolsTest {

    private final BusinessMcpTools tools = new BusinessMcpTools();

    // ───── design ⑬ 硬修正 2：参数白名单（防约定腐烂）─────

    @Test
    @DisplayName("updateProfile 参数白名单：ToolContext 之外恰好 3 个，结构性排除敏感字段")
    void updateProfile_paramWhitelist_excludesSensitiveFields() throws NoSuchMethodException {
        Method m = BusinessMcpTools.class.getMethod(
                "updateProfile",
                ToolContext.class, String.class, String.class, String.class);

        Parameter[] params = m.getParameters();
        // ToolContext 是 Spring AI 注入、不暴露给 LLM；其余才是 LLM 可见业务字段
        Set<String> visible = new HashSet<>();
        for (Parameter p : params) {
            if (p.getType() != ToolContext.class) {
                visible.add(p.getName());
            }
        }

        // 结构性硬卡：ToolContext 之外恰好 3 个业务字段（多于 3 = 可能塞了敏感字段）
        long bizParamCount = Arrays.stream(params)
                .filter(p -> p.getType() != ToolContext.class)
                .count();
        assertThat(bizParamCount).isEqualTo(3);

        // 恰好 nickname / phone / gender（依赖 -parameters）
        assertThat(visible).containsExactlyInAnyOrder("nickname", "phone", "gender");

        // 绝不出现任何敏感字段（任意大小写/下划线变体）
        assertThat(visible).noneMatch(BusinessMcpToolsTest::looksSensitive);
    }

    /** 敏感字段探测：password / idCard / disabilityCard / idNumber 任意大小写下划线变体。 */
    private static boolean looksSensitive(String name) {
        String n = name.toLowerCase().replace("_", "").replace("-", "");
        return n.contains("password")
                || n.contains("idcard")
                || n.contains("disabilitycard")
                || n.contains("idnumber");
    }

    // ───── design ⑫ encode-不抛：无 blind_id ─────

    @Test
    @DisplayName("各工具 null ToolContext → 返回 error JSON 含 blind_id 缺失，不抛")
    void noIdentity_returnsErrorJson_notThrow() {
        assertThat(tools.joinFamily(null, "13800138000"))
                .contains("\"status\":\"error\"").contains("blind_id");
        assertThat(tools.leaveFamily(null))
                .contains("\"status\":\"error\"").contains("blind_id");
        assertThat(tools.familyInfo(null))
                .contains("\"status\":\"error\"").contains("blind_id");
        // 合法参数但无身份也走到 noIdentity
        assertThat(tools.updateProfile(null, "小明", "13800138000", "男"))
                .contains("\"status\":\"error\"").contains("blind_id");
    }

    // ───── updateProfile 参数校验（身份校验之前，快速失败）─────

    @Nested
    @DisplayName("updateProfile 参数校验（不依赖身份，参数错先返回）")
    class UpdateProfileValidation {

        @Test
        @DisplayName("全空 → 未提供任何要更新的字段")
        void allBlank() {
            assertThat(tools.updateProfile(null, null, null, null))
                    .contains("未提供任何要更新的字段");
            assertThat(tools.updateProfile(null, "  ", "", null))
                    .contains("未提供任何要更新的字段");
        }

        @Test
        @DisplayName("非法手机号 → 手机号格式错误")
        void invalidPhone() {
            assertThat(tools.updateProfile(null, null, "12345", null))
                    .contains("手机号格式错误");
            assertThat(tools.updateProfile(null, null, "abc", null))
                    .contains("手机号格式错误");
        }

        @Test
        @DisplayName("非法性别 → 性别只能是「男」或「女」")
        void invalidGender() {
            assertThat(tools.updateProfile(null, null, null, "xyz"))
                    .contains("性别只能是");
        }

        @Test
        @DisplayName("合法性别多形态被接受（中文/英文/数字）— 不报性别错（走 noIdentity）")
        void validGenderAccepted() {
            for (String g : new String[]{"男", "女", "male", "female", "0", "1"}) {
                assertThat(tools.updateProfile(null, null, null, g))
                        .as("gender=%s 应被接受", g)
                        .doesNotContain("性别只能是");
            }
        }

        @Test
        @DisplayName("合法手机号被接受 — 不报格式错（走 noIdentity）")
        void validPhoneAccepted() {
            assertThat(tools.updateProfile(null, null, "13800138000", null))
                    .doesNotContain("手机号格式错误");
        }
    }

    // ───── joinFamily 参数校验 ─────

    @Test
    @DisplayName("joinFamily 空 familyPhone → 请提供家庭创建人的手机号")
    void joinFamily_blankPhone() {
        assertThat(tools.joinFamily(null, "")).contains("请提供家庭创建人的手机号");
        assertThat(tools.joinFamily(null, null)).contains("请提供家庭创建人的手机号");
        assertThat(tools.joinFamily(null, "  ")).contains("请提供家庭创建人的手机号");
    }
}
