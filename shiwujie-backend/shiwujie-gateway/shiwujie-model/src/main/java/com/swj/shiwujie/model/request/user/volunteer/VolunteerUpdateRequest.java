package com.swj.shiwujie.model.request.user.volunteer;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户信息修改
 */

@Data
public class VolunteerUpdateRequest {

    /**
     * 视障人士ID
     */
    private Long volunteerId;



    /**
     * 名字
     */
    private String name;


    /**
     * 性别 0-男 1-女
     */
    private Integer gender;


    /**
     * 身份证号
     */
    private String idCard;



    /**
     * 其它信息
     */
    private String otherInfo;


    /**
     * 纬度坐标
     */
    private BigDecimal latitude;

    /**
     * 经度坐标
     */
    private BigDecimal longitude;

    /**
     * 位置地址（省市区+详细地址）
     */
    private String locationAddress;


}
