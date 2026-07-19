package com.swj.shiwujie.model.request.call;


import lombok.Data;

import java.io.Serializable;

/**
 * 导航目的地（design chunk-2e-1，5006 信令结构化载荷）。
 *
 * <p>WS 5006（launch_navigation）下行随 {@link SocketData} 携带——Python 经高德 poi_search 解析目的地后
 * 调 launch_navigation，Java 组 SocketData(5006) 填 destination 推 App；App 读 name 起高德导航。
 * 替代旧「destination 塞 volunteerPhone」hack（SignalMcpTools / AiFragment 三端对齐）。</p>
 *
 * <p>v1 仅 name 必填（高德 URI 按 name 地理编码即起导航）；lat/lng/address 字段预留——等高德 SDK 集成
 * （能力补全批次）+ Python 导航技能传坐标时，扩 launch_navigation 签名填值，做精确导航。</p>
 */
@Data
public class Destination implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 目的地名称（必填，如「市第一医院」）
     */
    private String name;

    /**
     * 纬度（预留，高德 SDK 精确导航用；v1 不填）
     */
    private Double lat;

    /**
     * 经度（预留，高德 SDK 精确导航用；v1 不填）
     */
    private Double lng;

    /**
     * 反查地址 / 朗读友好文本（预留）
     */
    private String address;


}
