package com.swj.shiwujie.model.domain.community;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 求助帖表
 * @TableName HelpPost
 */
@TableName(value ="HelpPost")
@Data
public class Helppost implements Serializable {
    /**
     * 求助帖ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long helppostId;

    /**
     * 社区ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long communityId;

    /**
     * 视障人士ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long blindId;

    /**
     * 响应志愿者ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long volunteerId;

    /**
     * 求助级别
     */
    private Integer helpLevel;

    /**
     * 求助内容
     */
    private String helpContent;

    /**
     * 求助地点
     */
    private String helpLocation;

    /**
     * 求助帖状态 0-待响应 1-处理中 2-已完成 3-已取消
     */
    private Integer postStatus;

    /**
     * 求助评价
     */
    private String evaluation;

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