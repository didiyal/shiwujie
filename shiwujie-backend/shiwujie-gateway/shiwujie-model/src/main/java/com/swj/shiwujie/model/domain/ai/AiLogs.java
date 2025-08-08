package com.swj.shiwujie.model.domain.ai;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * AI操作日志表
 * @TableName AiLogs
 */
@TableName(value ="AiLogs")
@Data
public class AiLogs implements Serializable {
    /**
     * AI操作日志ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long logId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 发送内容
     */
    private String content;

    /**
     * 类型
     */
    private String logType;

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