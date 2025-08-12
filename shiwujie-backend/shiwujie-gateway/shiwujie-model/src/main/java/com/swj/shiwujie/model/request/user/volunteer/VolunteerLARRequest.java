package com.swj.shiwujie.model.request.user.volunteer;


import lombok.Data;

import java.io.Serializable;

/**
 * 手机号密码登录请求类
 * @author ldl
 */
@Data
public class VolunteerLARRequest implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码
     */
    private String password;


}
