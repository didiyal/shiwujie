package com.swj.shiwujie.model.VO.user.volunteer;



import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;


@Data
public class VolunteerLoginSuccessVO {


    /**
     * 志愿者ID
     */
    private Long volunteerId;

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
    private Boolean isIdCard;


    /**
     * 其它信息
     */
    private String otherInfo;

    /**
     * 在线状态（用于区分是否匹配） 0-离线 1-在线 2-忙碌
     */
    private Integer onlineStatus;

    /**
     * 帮助次数
     */
    private Long helpCount;

    /**
     * 志愿者评分
     */
    private BigDecimal rating;

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
     * 登录token
     */
    private String token;
}
