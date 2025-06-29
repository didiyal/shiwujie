package com.swj.shiwujie.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;
import lombok.Data;

/**
 * 视频通话频道表
 * @TableName channels
 */
@TableName(value ="video")
@Data
public class Video implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 频道号
     */
    private String channel;

    /**
     * 盲人uid
     */
    private String blindUid;

    /**
     * 志愿者uid
     */
    private String volunteerUid;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 视频通话开始时间
     */
    private String beginTime;

    /**
     * 视频通话截止时间
     */
    private String endTime;

    /**
     * 视频通话时长(小时:分钟)
     */
    private String callTime;

    /**
     * 频道状态  0 - 志愿者等待中  1 - 正在通话   2 - 通话结束   3  -  志愿者取消通话
     */
    private Integer status;

    /**
     * 逻辑删除,0 - 存在  1 - 删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}