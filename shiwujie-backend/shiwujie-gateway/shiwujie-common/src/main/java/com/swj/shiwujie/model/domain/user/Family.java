package com.swj.shiwujie.model.domain.user;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 家庭信息表
 * @TableName Family
 */
@TableName(value ="Family")
@Data
public class Family implements Serializable {
    /**
     * 家庭ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long familyId;

    /**
     * 家庭名字
     */
    private String familyName;

    /**
     * 家庭详细介绍
     */
    private String familyDescription;

    /**
     * 家庭创建人ID（关联志愿者表）
     */
    private Long creatorVolunteerId;

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