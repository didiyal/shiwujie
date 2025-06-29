package com.swj.shiwujie.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户表
 * @TableName user
 * @author ldl
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 用户头像
     */
    private String userUrl;

    /**
     * 用户手机
     */
    private String userPhone;

    /**
     * 用户邮箱
     */
    private String userEmail;

    /**
     * 性别：男 0，女 1
     */
    private Integer gender;

    /**
     * 状态: 盲人 0，志愿者 1，等待选择 2
     */
    private Integer status;

    /**
     * 是否在线：不在线 0，在线 1
     */
    private Integer isOnline;

    /**
     * 家庭ID
     */
    private Long familyId;

    /**
     * 用户权限：用户 0，管理员 1
     */
    private Integer userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
    /**
     * 信令状态 0 - 空闲，1 - 等待接通，2 - 正在通话
     */
    private Integer callStatus;

    /**
     * 当前通话的 Call ID
     */
    private String callChannel;

    /**
     * 残疾人证件
     */
    private String userCertificate;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}