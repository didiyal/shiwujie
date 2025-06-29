package com.swj.shiwujie.model.VO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {
    /**
     * 用户ID
     */
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
     * 状态: 盲人 0，志愿者 1，盲人家人 2
     */
    private Integer status;

    /**
     * 是否在线：不在线 0，在线 1
     */
    private Integer isOnline;

    /**
     * 家庭账号
     */
    private String familyAccount;

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
     * 
     */
    private String userCertificate;

}
