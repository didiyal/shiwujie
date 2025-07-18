package com.swj.shiwujie.model.request.community;

import com.baomidou.mybatisplus.annotation.*;
import com.swj.shiwujie.model.request.user.volunteer.CommunityVolunteerRegisterRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 社区信息表
 * @TableName Community
 */
@TableName(value ="Community")
@Data
public class CommunityRegisterRequest implements Serializable {


    /**
     * 社区名字
     */
    private String communityName;

    /**
     * 社区介绍
     */
    private String communityDescription;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String district;

    /**
     * 具体地址
     */
    private String address;

    /**
     * 社区注册信息
     */
    private String registrationInfo;


    /**
     * 注册人信息
     */
    private CommunityVolunteerRegisterRequest volunteer;




}