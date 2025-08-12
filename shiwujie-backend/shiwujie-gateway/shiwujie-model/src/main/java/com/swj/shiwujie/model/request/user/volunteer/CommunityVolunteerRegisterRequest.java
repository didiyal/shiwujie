package com.swj.shiwujie.model.request.user.volunteer;


import lombok.Data;

import java.io.Serializable;

/**
 * 社区入驻志愿者注册信息
 * @author ldl
 */
@Data
public class CommunityVolunteerRegisterRequest implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 名字
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别 0-男 1-女
     */
    private Integer gender;

    /**
     * 微信账号
     */
    private String wechatId;


    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 其它信息
     */
    private String otherInfo;



}
