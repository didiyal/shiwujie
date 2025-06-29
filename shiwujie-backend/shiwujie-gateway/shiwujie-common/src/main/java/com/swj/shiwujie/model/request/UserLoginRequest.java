package com.swj.shiwujie.model.request;

import lombok.Data;

@Data
public class UserLoginRequest {
    /**
     * 用户账号
     */
    private String userAccount;
    /**
     * 用户手机号
     */
    private String userPhone;
    /**
     * 用户邮箱
     */
    private String userEmial;
    /**
     * 用户密码
     */
    private String userPassword;
}
