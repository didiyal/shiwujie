package com.swj.shiwujie.model.request.user;


import lombok.Data;

/**
 * 手机号密码登录请求类
 * @author ldl
 */
@Data
public class VolunteerLARRequest {

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码
     */
    private String password;


}
