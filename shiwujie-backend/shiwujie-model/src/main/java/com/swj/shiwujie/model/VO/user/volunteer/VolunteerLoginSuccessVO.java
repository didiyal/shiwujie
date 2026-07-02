package com.swj.shiwujie.model.VO.user.volunteer;



import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;


public class VolunteerLoginSuccessVO extends VolunteerVO{


    private static final long serialVersionUID = 5115728641061849684L;
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
