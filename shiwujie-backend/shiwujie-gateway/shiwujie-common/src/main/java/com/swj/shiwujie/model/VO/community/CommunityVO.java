package com.swj.shiwujie.model.VO.community;



import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 社区信息脱敏返回类
 */

@Data
public class CommunityVO {


    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 社区类型ID
     */
    private String communityType;

    /**
     * 社区级别ID
     */
    private String communityLevel;


    /**
     * 上级社区ID
     */
    private Long parentCommunityId;

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
     * 社区注册人ID（关联志愿者表）
     */
    private Long registerVolunteerId;

    /**
     * 社区状态  0-未审核, 1-已审核, 2-已停用
     */
    private Integer communityStatus;



}
