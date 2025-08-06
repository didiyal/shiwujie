package com.swj.shiwujie.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


/**
 * 获取登录用户id
 */
public class LoginUtils {


    /**
     * 获取当前登录盲人id
     *
     * @return
     */
    public static Long getLoginBlindId() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Long loginUserId = (Long)request.getAttribute("loginBlindId");
        return loginUserId;
    }


    /**
     * 获取当前登录志愿者id
     *
     * @return
     */
    public static Long getLoginVolunteerId() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Long loginUserId = (Long)request.getAttribute("loginVolunteerId");
        return loginUserId;
    }


    /**
     * 获取当前登录用户手机号
     *
     * @return
     */
    public static String getLoginUserPhone() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
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

}
