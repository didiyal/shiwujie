package com.swj.shiwujie.utils;

import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 获取登录用户id
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
        if(loginUserId==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
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
        if(loginUserId==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
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
        if(phone==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
        return phone;
    }

}
