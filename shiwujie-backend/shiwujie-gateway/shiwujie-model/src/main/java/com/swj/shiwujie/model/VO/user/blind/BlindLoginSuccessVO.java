package com.swj.shiwujie.model.VO.user.blind;



import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 登录成功返回类
 */
public class BlindLoginSuccessVO extends BlindVO{



    /**
     * 登录token
     */
    private String token;


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
