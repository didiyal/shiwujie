package com.swj.shiwujie.model.domain.community;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 社区管理人员表
 * @TableName CommunityManager
 */
@TableName(value ="CommunityManager")
@Data
public class Communitymanager implements Serializable {
    /**
     * 社区管理人员ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long managerId;

    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 志愿者ID
     */
    private Long volunteerId;

    /**
     * 社区角色权限ID
     */
    private Long rolePermissionId;

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