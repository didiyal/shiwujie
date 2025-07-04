package com.swj.shiwujie.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 视障人士信息表
 * @TableName Blind
 */
@TableName(value ="Blind")
@Data
public class Blind implements Serializable {
    /**
     * 视障人士ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long blindId;

    /**
     * 社区ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long communityId;

    /**
     * 是否主动加入社区
     */
    private Integer isActivelyJoined;

    /**
     * 家庭ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
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
     * 密码
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 信息更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除 0-存在 1-删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}