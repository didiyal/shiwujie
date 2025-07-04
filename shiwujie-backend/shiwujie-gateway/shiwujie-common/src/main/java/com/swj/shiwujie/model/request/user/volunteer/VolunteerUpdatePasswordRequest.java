package com.swj.shiwujie.model.request.user.volunteer;


import lombok.Data;

@Data
public class VolunteerUpdatePasswordRequest {


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
