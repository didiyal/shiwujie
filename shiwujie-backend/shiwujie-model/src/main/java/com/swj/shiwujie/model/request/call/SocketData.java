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


    /**
     * 随信令下发的提示文案（服务端 → 客户端 TTS 播报用）。
     *
     * <p>2026-09-14 新增：外层 SocketVO.message 在客户端解析 envelope 时会被丢弃，
     * 需要客户端播报的文字必须放进 SocketData 本体（客户端 SocketDataV0.message 同名自动映射）。</p>
     */
    private String message;


    private final static long serialVersionUID = 1L;
}
