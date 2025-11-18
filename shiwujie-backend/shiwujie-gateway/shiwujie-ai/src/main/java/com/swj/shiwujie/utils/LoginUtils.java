package com.swj.shiwujie.utils;

import com.swj.shiwujie.model.domain.user.Blind;


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
//        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
//        Blind loginBlind = Convert.convert(new TypeReference<Blind>() {
//        }, request.getAttribute("loginBlind"));
//
//        return loginBlind;
        Blind blind = new Blind();
        blind.setBlindId(1L);
        blind.setPhone("19872250169");
        return blind;
    }




    /**
     * 获取当前登录用户手机号
     *
     * @return
     */
    public static String getLoginUserPhone() {
//        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
//        return (String)request.getAttribute("phone");
        return "19872250169";
    }



}
