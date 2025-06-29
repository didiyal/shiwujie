package com.swj.shiwujie.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 视频通话频道表
 * @TableName callHelp
 */
@TableName(value ="callHelp")
@Data
public class CallHelp implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 家庭id
     */
    private Long familyId;

    /**
     * 求助盲人id
     */
    private Long blindId;

    /**
     * 求助盲人视频通话uid
     */
    private String blindUid;

    /**
     * 通话频道
     */
    private String channel;

    /**
     * 帮助家属id(后续可以多人)
     */
    private String helpOthersId;

    /**
     * 帮助家属视频通话uid(后续可以多人)
     */
    private String helpOthersUid;

    /**
     * 状态 0 - 求助人等待帮助中   1 - 求助人与帮助家属正在通话	2 - 求助人主动取消求助	3 - 求助正常结束
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 视频通话开始时间
     */
    private Date beginTime;

    /**
     * 视频通话截止时间
     */
    private Date endTime;

    /**
     * 视频通话时长(小时:分钟)
     */
    private Long callTime;

    /**
     * 逻辑删除,0 - 存在  1 - 删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}