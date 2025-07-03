package com.swj.shiwujie.model.request.user.volunteer;


import lombok.Data;

@Data
public class VolunteerUpdatePasswordRequest {


    /**
     * 密码
     */
    private String originPassword;



    /**
     * 密码
     */
    private String newPassword;


}
