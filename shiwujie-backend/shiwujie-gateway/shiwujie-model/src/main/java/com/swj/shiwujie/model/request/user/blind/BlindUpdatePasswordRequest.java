package com.swj.shiwujie.model.request.user.blind;


import lombok.Data;

/**
 * 修改密码请求类
 */
@Data
public class BlindUpdatePasswordRequest {

    /**
     * 盲人id
     */
    private Long blindId;

    /**
     * 密码
     */
    private String originPassword;



    /**
     * 密码
     */
    private String newPassword;


}
