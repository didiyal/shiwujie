package com.swj.shiwujie.model.request.user.blind;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户信息修改
 */

@Data
public class BlindUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 视障人士ID
     */
    private Long blindId;



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
     * 残疾人证件号
     */
    private String disabilityCard;



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
