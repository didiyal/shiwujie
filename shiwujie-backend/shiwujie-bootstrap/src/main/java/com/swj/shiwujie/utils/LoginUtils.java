package com.swj.shiwujie.utils;

import com.swj.shiwujie.model.domain.user.Blind;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 获取登录用户id
 *
 * <p>v3.0.0 单体化阶段2.5：合并原 ai 模块自带副本（{@code getLoginBlind()} 读取 ai 拦截器注入的
 * 完整 Blind 实体、无参 {@code getLoginUserPhone()} 走 RequestContextHolder）与 common-web 原有
 * 的 request 参版本，消除同 FQN 双副本在 fat-jar 中的非确定碰撞。</p>
 */
public class LoginUtils {


    /**
     * 获取当前登录盲人id
     *
     * @param request
     * @return
     */
    public static Long getLoginBlindId(HttpServletRequest request) {
        Long loginUserId = (Long)request.getAttribute("loginBlindId");
        return loginUserId;
    }


    /**
     * 获取当前登录志愿者id
     *
     * @param request
     * @return
     */
    public static Long getLoginVolunteerId(HttpServletRequest request) {
        Long loginUserId = (Long)request.getAttribute("loginVolunteerId");
        return loginUserId;
    }


    /**
     * 获取当前登录用户手机号
     *
     * @param request
     * @return
     */
    public static String getLoginUserPhone(HttpServletRequest request) {
        String phone = (String)request.getAttribute("phone");
        return phone;
    }


    /**
     * 获取当前登录用户社区职位id
     *
     * @param request
     * @return
     */
    public static Long getVolunteerRole(HttpServletRequest request) {
        Long phone = (Long)request.getAttribute("role");
        return phone;
    }


    /**
     * 获取当前登录盲人id（无参版：经 RequestContextHolder，供 AI 等 SSE 控制器直用）。
     * <p>2026-09-12：AI 链路鉴权并入业务 {@code LoginCheckInterceptor}（注入 {@code loginBlindId}）
     * 后，取代原读取 {@code loginBlind} 实体属性的 {@code getLoginBlind()}（该属性随 AI 专用
     * 拦截器删除不再存在）；志愿者 token 访问 AI 接口时返回 {@code null}，由调用方拒绝。</p>
     */
    public static Long getLoginBlindId() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return (Long) request.getAttribute("loginBlindId");
    }


    /**
     * 获取当前登录用户手机号（无参版，ai 链路专用：走 RequestContextHolder，用于非 Controller 调用栈如 AI Tools）
     */
    public static String getLoginUserPhone() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return (String) request.getAttribute("phone");
    }


}
