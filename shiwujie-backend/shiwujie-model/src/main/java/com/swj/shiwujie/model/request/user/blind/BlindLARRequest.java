package com.swj.shiwujie.model.request.user.blind;


import lombok.Data;

import java.io.Serializable;

/**
 * 手机号密码登录请求类
 * @author ldl
 */
@Data
public class BlindLARRequest implements Serializable {

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
