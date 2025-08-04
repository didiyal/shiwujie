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
    private Long help_id;


    /**
     * 频道id
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long channel_id;


    /**
     * 家庭ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long family_id;


    /**
     * 视障人士ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long blind_id;

    /**
     * 视障人士纬度坐标
     */
    private BigDecimal blind_latitude;

    /**
     * 视障人士经度坐标
     */
    private BigDecimal blind_longitude;

    /**
     * 视障人士位置地址（省市区+详细地址）
     */
    private String blind_location_address;

    /**
     * 响应家属(志愿者)ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long volunteer_id;

    /**
     * 响应家属纬度坐标
     */
    private BigDecimal volunteer_latitude;

    /**
     * 响应家属经度坐标
     */
    private BigDecimal volunteer_longitude;

    /**
     * 响应家属位置地址（省市区+详细地址）
     */
    private String volunteer_location_address;

    /**
     * 求助状态 0-待响应 1-处理中 2-已完成 3-已取消
     */
    private Integer help_status;

    /**
     * 求助开始时间
     */
    private Date start_time;

    /**
     * 求助响应时间
     */
    private Date response_time;

    /**
     * 求助结束时间
     */
    private Date end_time;

    /**
     * 求助耗时
     */
    private Long duration;

    /**
     * 视频储存地址
     */
    private String video_path;

    /**
     * 创建时间
     */
    private Date create_time;

    /**
     * 信息更新时间
     */
    private Date update_time;

    /**
     * 逻辑删除 0-存在 1-删除
     */
    @TableLogic
    private Integer is_delete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}