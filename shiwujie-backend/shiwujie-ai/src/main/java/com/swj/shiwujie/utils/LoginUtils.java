package com.swj.shiwujie.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import com.swj.shiwujie.model.domain.user.Blind;
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
    public static Blind getLoginBlind() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Blind loginBlind = Convert.convert(new TypeReference<Blind>() {
        }, request.getAttribute("loginBlind"));

        return loginBlind;
    }




    /**
     * 获取当前登录用户手机号
     *
     * @return
     */
    public static String getLoginUserPhone() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return (String)request.getAttribute("phone");
    }



}
