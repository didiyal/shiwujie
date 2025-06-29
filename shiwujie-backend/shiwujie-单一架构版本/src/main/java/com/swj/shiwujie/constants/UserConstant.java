package com.swj.shiwujie.constants;

import com.swj.shiwujie.model.domain.User;

/**
 * 用户公共类
 *
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "userLoginState";

    //  ------- 权限 --------

    /**
     * 默认权限
     */
    int DEFAULT_ROLE = 0;


    /**
     * 管理员权限
     */
    int ADMIN_ROLE = 1;


    /**
     * 盐值，混淆密码
     *
     */
    String SALT = "shiwujie";


    /**
     * 登录用户id存储键(redis)
     */
    String LOGIN_USER_KEY = "LOGIN_USER_KEY";


    /**
     * 登录用户token存储键(redis)
     */
    String LOGIN_USER_TOKEN = "LOGIN_USER_TOKEN";

}
