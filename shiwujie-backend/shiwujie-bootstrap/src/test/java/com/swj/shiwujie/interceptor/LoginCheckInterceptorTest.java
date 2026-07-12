package com.swj.shiwujie.interceptor;

import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.constants.UserConstants;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.utils.JwtUtils;
import com.swj.shiwujie.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoginCheckInterceptor 鉴权链单元测试。
 * 纯 Mockito：不起 Spring 上下文、不连 Redis；用真实 JwtUtils 造合法 token，
 * RedisUtils 用 @Mock 隔离。
 */
@ExtendWith(MockitoExtension.class)
class LoginCheckInterceptorTest {

    @Mock
    private RedisUtils redisUtils;

    @InjectMocks
    private LoginCheckInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    /** 造合法盲人 token（isBlind=true 分支） */
    private String makeBlindToken(Long blindId, String phone) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("isBlind", true);
        payload.put("blindId", blindId);
        payload.put("phone", phone);
        return JwtUtils.generateToken(payload, UserConstants.TOKEN_SECRETKEY, Duration.ofDays(90));
    }

    /** 造合法志愿者 token（isBlind=false 分支） */
    private String makeVolunteerToken(Long volunteerId, String phone, Long role) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("isBlind", false);
        payload.put("volunteerId", volunteerId);
        payload.put("phone", phone);
        if (role != null) {
            payload.put("role", role);
        }
        return JwtUtils.generateToken(payload, UserConstants.TOKEN_SECRETKEY, Duration.ofDays(90));
    }

    @BeforeEach
    void setUp() {
        // 默认回填一个非 OPTIONS、非白名单的方法/URL，各用例可按需覆盖
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
    }

    // ===================== 放行分支 =====================

    @Nested
    @DisplayName("放行分支（OPTIONS / 白名单 URL）")
    class PassThrough {

        @Test
        @DisplayName("OPTIONS 预检请求 → 直接放行 true，不读 Authorization")
        void preHandle_options_returnsTrue() throws Exception {
            when(request.getMethod()).thenReturn(HttpMethod.OPTIONS.toString());

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            verify(request, never()).getHeader(anyString());
            verify(redisUtils, never()).renewKey(anyString(), anyLong());
        }

        @Test
        @DisplayName("URL 含 loginAndRegister → 放行 true")
        void preHandle_urlContainsLoginAndRegister_returnsTrue() throws Exception {
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/user/loginAndRegister"));

            assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
            verify(request, never()).getHeader(anyString());
        }

        @Test
        @DisplayName("URL 含 Login → 放行 true")
        void preHandle_urlContainsLogin_returnsTrue() throws Exception {
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/community/Login"));

            assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        }

        @Test
        @DisplayName("URL 含 Register → 放行 true")
        void preHandle_urlContainsRegister_returnsTrue() throws Exception {
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/community/Register"));

            assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        }
    }

    // ===================== 拒绝分支（抛 NOT_LOGIN） =====================

    @Nested
    @DisplayName("未登录拒绝分支（NOT_LOGIN code=40010）")
    class RejectNotLogin {

        private void assertNotLogin(Runnable action) {
            assertThatThrownBy(action::run)
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NOT_LOGIN.getCode());
        }

        @Test
        @DisplayName("无 Authorization header → NOT_LOGIN")
        void preHandle_noAuthorizationHeader_throwsNotLogin() {
            when(request.getHeader("Authorization")).thenReturn(null);

            assertNotLogin(() -> safePreHandle());
        }

        @Test
        @DisplayName("Authorization header 不以 'Bearer ' 开头 → NOT_LOGIN")
        void preHandle_headerNotBearer_throwsNotLogin() {
            when(request.getHeader("Authorization")).thenReturn("Token abc.def.ghi");

            assertNotLogin(() -> safePreHandle());
        }

        @Test
        @DisplayName("Bearer 后 token 为空串 → NOT_LOGIN")
        void preHandle_emptyToken_throwsNotLogin() {
            when(request.getHeader("Authorization")).thenReturn("Bearer ");

            assertNotLogin(() -> safePreHandle());
        }

        @Test
        @DisplayName("token 非法（乱串，JWT 解析失败） → NOT_LOGIN")
        void preHandle_invalidJwt_throwsNotLogin() {
            when(request.getHeader("Authorization")).thenReturn("Bearer not.a.valid.jwt");

            assertNotLogin(() -> safePreHandle());
        }

        @Test
        @DisplayName("合法 token 但 Redis 中无记录（getFromRedis 返 null） → NOT_LOGIN")
        void preHandle_redisNull_throwsNotLogin() {
            String token = makeBlindToken(1001L, "13800000001");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            String expectedKey = UserConstants.REDIS_SECRETKEY + "-blind-1001";
            when(redisUtils.getFromRedis(expectedKey)).thenReturn(null);

            assertNotLogin(() -> safePreHandle());
        }

        @Test
        @DisplayName("合法 token 但 Redis 中值与 token 不一致 → NOT_LOGIN")
        void preHandle_redisValueMismatch_throwsNotLogin() {
            String token = makeBlindToken(1001L, "13800000001");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            String expectedKey = UserConstants.REDIS_SECRETKEY + "-blind-1001";
            when(redisUtils.getFromRedis(expectedKey)).thenReturn("a-completely-different-token");

            assertNotLogin(() -> safePreHandle());
            // 不一致时不应续期
            verify(redisUtils, never()).renewKey(anyString(), anyLong());
        }
    }

    // ===================== 成功分支 =====================

    @Nested
    @DisplayName("成功鉴权分支（续期 + 注入 attribute）")
    class Success {

        @Test
        @DisplayName("盲人合法 token：续期 + 注入 loginBlindId/phone/role → true")
        void preHandle_blindValidToken_renewsAndSetsAttr() throws Exception {
            Long blindId = 1001L;
            String phone = "13800000001";
            Long role = 2L;
            String token = makeBlindTokenWithRole(blindId, phone, role);
            String expectedKey = UserConstants.REDIS_SECRETKEY + "-blind-" + blindId;

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(redisUtils.getFromRedis(expectedKey)).thenReturn(token);

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            // 续期 90 天
            verify(redisUtils).renewKey(expectedKey, 90L);
            // 注入 attribute（盲人分支）
            verify(request).setAttribute("loginBlindId", blindId);
            verify(request, never()).setAttribute(eq("loginVolunteerId"), any());
            verify(request).setAttribute("phone", phone);
            verify(request).setAttribute("role", role);
        }

        @Test
        @DisplayName("盲人 token 无 role：role attribute 注入 null")
        void preHandle_blindTokenNoRole_setsRoleNull() throws Exception {
            Long blindId = 1002L;
            String phone = "13800000002";
            String token = makeBlindToken(blindId, phone);
            String expectedKey = UserConstants.REDIS_SECRETKEY + "-blind-" + blindId;

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(redisUtils.getFromRedis(expectedKey)).thenReturn(token);

            assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

            verify(request).setAttribute("loginBlindId", blindId);
            verify(request).setAttribute("phone", phone);
            verify(request).setAttribute("role", null);
        }

        @Test
        @DisplayName("志愿者合法 token：续期 + 注入 loginVolunteerId/phone/role → true")
        void preHandle_volunteerValidToken_renewsAndSetsAttr() throws Exception {
            Long volunteerId = 2001L;
            String phone = "13900000001";
            Long role = 3L;
            String token = makeVolunteerToken(volunteerId, phone, role);
            String expectedKey = UserConstants.REDIS_SECRETKEY + "-volunteer-" + volunteerId;

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(redisUtils.getFromRedis(expectedKey)).thenReturn(token);

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            verify(redisUtils).renewKey(expectedKey, 90L);
            verify(request).setAttribute("loginVolunteerId", volunteerId);
            verify(request, never()).setAttribute(eq("loginBlindId"), any());
            verify(request).setAttribute("phone", phone);
            verify(request).setAttribute("role", role);
        }

        @Test
        @DisplayName("盲人分支：ArgumentCaptor 精确校验注入的 attribute key 集合")
        void preHandle_blindBranch_attributeKeysExact() throws Exception {
            Long blindId = 1003L;
            String phone = "13800000003";
            String token = makeBlindToken(blindId, phone);
            String expectedKey = UserConstants.REDIS_SECRETKEY + "-blind-" + blindId;

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(redisUtils.getFromRedis(expectedKey)).thenReturn(token);

            interceptor.preHandle(request, response, new Object());

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(request, org.mockito.Mockito.times(3))
                    .setAttribute(keyCaptor.capture(), any());

            assertThat(keyCaptor.getAllValues())
                    .containsExactlyInAnyOrder("loginBlindId", "phone", "role");
        }
    }

    /** 造带 role 的盲人 token */
    private String makeBlindTokenWithRole(Long blindId, String phone, Long role) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("isBlind", true);
        payload.put("blindId", blindId);
        payload.put("phone", phone);
        payload.put("role", role);
        return JwtUtils.generateToken(payload, UserConstants.TOKEN_SECRETKEY, Duration.ofDays(90));
    }

    /** 包装 checked exception 为 runtime，便于 assertThrow lambda 调用 */
    private void safePreHandle() {
        try {
            interceptor.preHandle(request, response, new Object());
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }
}
