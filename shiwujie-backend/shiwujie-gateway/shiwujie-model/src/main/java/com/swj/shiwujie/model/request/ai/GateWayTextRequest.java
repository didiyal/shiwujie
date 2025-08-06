package com.swj.shiwujie.model.request.ai;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文本请求类
 */
@Data
@AllArgsConstructor
public class GateWayTextRequest {

    /**
     * 用户输入文本
     */
    private String text;


    /**
     * 视障人士id
     */
    private Long blindId;

}
