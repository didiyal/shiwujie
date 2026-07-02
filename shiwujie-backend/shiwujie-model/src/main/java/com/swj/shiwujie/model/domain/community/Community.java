package com.swj.shiwujie.model.domain.community;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 社区信息表
 * @TableName Community
 */
@TableName(value ="Community")
@Data
public class Community implements Serializable {
    /**
     * 社区ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long communityId;

    /**
     * 社区类型ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long communityTypeId;

    /**
     * 社区级别ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long communityLevelId;

    /**
     * 是否是默认社区
     */
    private Integer isDefaultCommunity;

    /**
     * 上级社区ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
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
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long registerVolunteerId;

    /**
     * 社区状态  0-未审核, 1-已审核, 2-已停用
     */
    private Integer communityStatus;

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