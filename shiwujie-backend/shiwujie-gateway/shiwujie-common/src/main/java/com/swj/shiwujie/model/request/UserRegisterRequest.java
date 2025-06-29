package com.swj.shiwujie.model.request;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 用户注册请求体
 * @author ddc
 */
@Data
public class UserRegisterRequest implements Serializable {


    /**
     * 用户手机号
     */
    private String userPhone;
    /**
     * 用户密码
     */
    private String userPassword;

}

