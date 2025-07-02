package com.swj.shiwujie.model.VO.user.blind;


import lombok.Data;

@Data
public class BlindUpdatePassword {


    /**
     * 密码
     */
    private String originPassword;



    /**
     * 密码
     */
    private String newPassword;


}
