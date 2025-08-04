package com.swj.shiwujie.model.request.user.blind;


import lombok.Data;

/**
 * 修改手机号请求类
 */
@Data
public class BlindUpdatePhoneRequest {

    /**
     * 盲人id
     */
    private Long blindId;


    /**
     * 要修改的手机号
     */
    private String phone;


}
