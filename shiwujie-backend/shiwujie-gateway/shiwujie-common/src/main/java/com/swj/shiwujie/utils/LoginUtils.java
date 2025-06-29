package com.swj.shiwujie.utils;

import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;


public class LoginUtils {



    /**
     * 获取当前登录用户id
     *
     * @param request
     * @return
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        Long loginUserId = (Long)request.getAttribute("loginUserId");
        if(loginUserId==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
        return loginUserId;
    }

}
