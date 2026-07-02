package com.swj.shiwujie.model.VO.user.blind;



import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 请求返回类
 */
@Data
public class BlindVO implements java.io.Serializable{


    private static final long serialVersionUID = -1496156123308537532L;
    /**
     * 视障人士ID
     */
    private Long blindId;

    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 是否主动加入社区
     */
    private Integer isActivelyJoined;

    /**
     * 家庭ID
     */
    private Long familyId;

    /**
     * 名字
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码,脱敏
     */
    private String password;

    /**
     * 性别 0-男 1-女
     */
    private Integer gender;

    /**
     * 微信账号
     */
    private String wechatId;

    /**
     * QQ账号
     */
    private String qqId;

    /**
     * 身份证号
     */
    private Boolean isIdCard;

    /**
     * 残疾人证件号
     */
    private Boolean isDisabilityCard;

    /**
     * 其它信息
     */
    private String otherInfo;

    /**
     * 求助次数
     */
    private Long helpRequestCount;

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

    /**
     * 位置更新时间
     */
    private Date locationUpdateTime;


}
