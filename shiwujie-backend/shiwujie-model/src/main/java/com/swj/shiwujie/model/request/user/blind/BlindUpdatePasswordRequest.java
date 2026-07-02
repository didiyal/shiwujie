package com.swj.shiwujie.model.request.user.blind;


import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求类
 */
@Data
public class BlindUpdatePasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

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
