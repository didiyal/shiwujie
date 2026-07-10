package com.swj.shiwujie.model.domain.call;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 紧急求助表
 * @TableName UrgentHelp
 */
@TableName(value ="UrgentHelp")
@Data
public class Urgenthelp implements Serializable {
    /**
     * 紧急求助ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long helpId;


    /**
     * 频道id
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long channelId;


    /**
     * 家庭ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long familyId;


    /**
     * 视障人士ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long blindId;

    /**
     * 视障人士纬度坐标
     */
    private BigDecimal blindLatitude;

    /**
     * 视障人士经度坐标
     */
    private BigDecimal blindLongitude;

    /**
     * 视障人士位置地址（省市区+详细地址）
     */
    private String blindLocationAddress;

    /**
     * 响应家属(志愿者)ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long volunteerId;

    /**
     * 响应家属纬度坐标
     */
    private BigDecimal volunteerLatitude;

    /**
     * 响应家属经度坐标
     */
    private BigDecimal volunteerLongitude;

    /**
     * 响应家属位置地址（省市区+详细地址）
     */
    private String volunteerLocationAddress;

    /**
     * 求助状态 0-待响应 1-处理中 2-已完成 3-已取消
     */
    private Integer helpStatus;

    /**
     * 求助开始时间
     */
    private Date startTime;

    /**
     * 求助响应时间
     */
    private Date responseTime;

    /**
     * 求助结束时间
     */
    private Date endTime;

    /**
     * 求助耗时
     */
    private Long duration;

    /**
     * 视频储存地址
     */
    private String videoPath;

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
