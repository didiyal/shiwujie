package com.swj.shiwujie.common;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI工具调用请求类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiToolRequest {
    /**
     * 工具类型 1-11
     */
    private Integer type;
    
    /**
     * 工具数据，可以为空
     */
    private String data;


    /**
     * 将对象转换为JSON字符串
     * @return JSON字符串
     */
    public String toJson() {
        return JSONUtil.toJsonStr(this);
    }

    /**
     * 从JSON字符串解析对象
     * @param json JSON字符串
     * @return AiToolRequest对象
     */
    public static AiToolRequest fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new AiToolRequest();
        }
        return JSONUtil.toBean(json, AiToolRequest.class);
    }
}
