package com.swj.shiwujie.model.request.user.volunteer;


import lombok.Data;

import java.io.Serializable;

@Data
public class VolunteerUpdatePasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 志愿者id
     */
    private Long volunteerId;

    /**
     * 密码
     */
    private String originPassword;



    /**
     * 密码
     */
    private String newPassword;


}
