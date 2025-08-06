package com.swj.shiwujie.model.request.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图片请求类
 */
@Data
@AllArgsConstructor
public class GateWayImageRequest {

    /**
     * 用户输入文本
     */
    private String imageUrl;


    /**
     * 视障人士id
     */
    private Long blindId;

}
