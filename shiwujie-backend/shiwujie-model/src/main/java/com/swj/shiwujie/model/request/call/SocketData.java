package com.swj.shiwujie.model.request.call;


import lombok.Data;

import java.io.Serializable;

/**
 * socket请求类
 */
@Data
public class SocketData implements Serializable {


    /**
     * 请求类型 0 - 建立连接  1 - 志愿者匹配成功  2 - 志愿者视频初始化成功
     */
    private Integer requestType;


    /**
     * 视障人士手机号
     */
    private String blindPhone;


    /**
     * 志愿者手机号
     */
    private String volunteerPhone;


    /**
     * 频道id
     */
    private Long channelId;



    private final static long serialVersionUID = 1L;
}
