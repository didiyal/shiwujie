package com.swj.shiwujie.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 家庭表
 * @TableName family
 * @author ldl
 */
@TableName(value ="family")
@Data
public class Family implements Serializable {
    /**
     * 主键 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 家庭名字
     */
    private String familyName;

    /**
     * 家庭账号
     */
    private String familyAccount;

    /**
     * 创建人Id
     */
    private Long userId;

    /**
     * 加入人的Id  json   
     */
    private String addId;

    /**
     * 待加入用户Id
     */
    private String postId;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 逻辑删除   0 - 存在  1 - 删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}