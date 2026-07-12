package com.swj.shiwujie.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
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
     * 获取当前登录盲人实体（ai 链路专用：ai 拦截器向 request 注入完整 Blind）
     */
    public static Blind getLoginBlind() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Blind loginBlind = Convert.convert(new TypeReference<Blind>() {
        }, request.getAttribute("loginBlind"));

        return loginBlind;
    }


    /**
     * 获取当前登录用户手机号（无参版，ai 链路专用：走 RequestContextHolder，用于非 Controller 调用栈如 AI Tools）
     */
    public static String getLoginUserPhone() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return (String) request.getAttribute("phone");
    }


}
