package com.swj.shiwujie.model.request.user.volunteer;


import lombok.Data;

import java.io.Serializable;

/**
 * 修改手机号请求类
 */
@Data
public class VolunteerUpdatePhoneRequest implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 志愿者id
     */
    private Long volunteerId;


    /**
     * 要修改的手机号
     */
    private String phone;


}
