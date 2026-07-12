package com.swj.shiwujie.utils;

import com.swj.shiwujie.model.domain.user.Blind;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.context.request.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link LoginUtils} 单元测试。
 *
 * <p>request 参版本：直接 mock {@link HttpServletRequest}。
 * 无参版本（{@code getLoginBlind} / {@code getLoginUserPhone()}）走 {@link
 * org.springframework.web.context.request.RequestContextHolder} 静态方法，用
 * {@link MockedStatic} 桩住，避免依赖 Spring 上下文。
 */
@DisplayName("LoginUtils 登录用户上下文取值")
class LoginUtilsTest {

    // ---------- request 参版本 ----------

    @Test
    @DisplayName("getLoginBlindId(request): 取回注入的 loginBlindId")
    void getLoginBlindId_returnsInjected() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("loginBlindId")).thenReturn(2001L);

        assertThat(LoginUtils.getLoginBlindId(req)).isEqualTo(2001L);
    }

    @Test
    @DisplayName("getLoginBlindId(request): 未注入 → null")
    void getLoginBlindId_nullWhenAbsent() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("loginBlindId")).thenReturn(null);

        assertThat(LoginUtils.getLoginBlindId(req)).isNull();
    }

    @Test
    @DisplayName("getLoginVolunteerId(request): 取回注入的 loginVolunteerId")
    void getLoginVolunteerId_returnsInjected() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("loginVolunteerId")).thenReturn(3001L);

        assertThat(LoginUtils.getLoginVolunteerId(req)).isEqualTo(3001L);
    }

    @Test
    @DisplayName("getLoginVolunteerId(request): 未注入 → null")
    void getLoginVolunteerId_nullWhenAbsent() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("loginVolunteerId")).thenReturn(null);

        assertThat(LoginUtils.getLoginVolunteerId(req)).isNull();
    }

    @Test
    @DisplayName("getLoginUserPhone(request): 取回注入的 phone")
    void getLoginUserPhone_req_returnsInjected() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("phone")).thenReturn("13800138000");

        assertThat(LoginUtils.getLoginUserPhone(req)).isEqualTo("13800138000");
    }

    @Test
    @DisplayName("getLoginUserPhone(request): 未注入 → null")
    void getLoginUserPhone_req_nullWhenAbsent() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("phone")).thenReturn(null);

        assertThat(LoginUtils.getLoginUserPhone(req)).isNull();
    }

    @Test
    @DisplayName("getVolunteerRole(request): 取回注入的 role")
    void getVolunteerRole_returnsInjected() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("role")).thenReturn(2L);

        assertThat(LoginUtils.getVolunteerRole(req)).isEqualTo(2L);
    }

    @Test
    @DisplayName("getVolunteerRole(request): 未注入 → null")
    void getVolunteerRole_nullWhenAbsent() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("role")).thenReturn(null);

        assertThat(LoginUtils.getVolunteerRole(req)).isNull();
    }

    // ---------- 无参版本（RequestContextHolder 链路） ----------

    @Test
    @DisplayName("getLoginUserPhone() 无参版：桩住 RequestContextHolder 取回 phone")
    void getLoginUserPhone_noArg_returnsFromHolder() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("phone")).thenReturn("13900139000");

        try (MockedStatic<org.springframework.web.context.request.RequestContextHolder> holderMock =
                     org.mockito.Mockito.mockStatic(
                             org.springframework.web.context.request.RequestContextHolder.class)) {

            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    mock(org.springframework.web.context.request.ServletRequestAttributes.class);
            when(attrs.getRequest()).thenReturn(req);
            holderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);

            assertThat(LoginUtils.getLoginUserPhone()).isEqualTo("13900139000");
        }
    }

    @Test
    @DisplayName("getLoginBlind()：桩住 RequestContextHolder + Convert 转换注入 Blind")
    void getLoginBlind_returnsFromHolder() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        Blind injected = new Blind();
        injected.setBlindId(9001L);
        injected.setName("测试盲人");
        when(req.getAttribute("loginBlind")).thenReturn(injected);

        try (
                MockedStatic<org.springframework.web.context.request.RequestContextHolder> holderMock =
                        org.mockito.Mockito.mockStatic(
                                org.springframework.web.context.request.RequestContextHolder.class);
                MockedStatic<cn.hutool.core.convert.Convert> convertMock =
                        org.mockito.Mockito.mockStatic(cn.hutool.core.convert.Convert.class)
        ) {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    mock(org.springframework.web.context.request.ServletRequestAttributes.class);
            when(attrs.getRequest()).thenReturn(req);
            holderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);

            // Convert.convert(TypeReference, Object) 直接回返注入实体
            convertMock.when(() -> cn.hutool.core.convert.Convert.convert(
                    org.mockito.ArgumentMatchers.<cn.hutool.core.lang.TypeReference<Blind>>any(),
                    org.mockito.ArgumentMatchers.eq(injected)))
                    .thenReturn(injected);

            Blind result = LoginUtils.getLoginBlind();
            assertThat(result).isNotNull();
            assertThat(result.getBlindId()).isEqualTo(9001L);
            assertThat(result.getName()).isEqualTo("测试盲人");
        }
    }
}
