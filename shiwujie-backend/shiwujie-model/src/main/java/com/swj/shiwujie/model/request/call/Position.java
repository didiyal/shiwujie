package com.swj.shiwujie.model.request.call;


import lombok.Data;

import java.io.Serializable;

/**
 * 盲人当前定位（design ② 每轮附位置）。
 *
 * <p>AI-turn 入站随 {@link SocketData} 携带（仅 requestType=100），Java 转发给 Python /ai/turn 的
 * {@code position} 字段（{lat,lng,address}）。其余 socket 信令不携带，字段保持 null。</p>
 */
@Data
public class Position implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 纬度
     */
    private Double lat;

    /**
     * 经度
     */
    private Double lng;

    /**
     * 反查地址（朗读友好）
     */
    private String address;


}
