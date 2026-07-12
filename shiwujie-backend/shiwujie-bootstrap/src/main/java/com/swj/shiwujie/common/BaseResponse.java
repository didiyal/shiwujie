package com.swj.shiwujie.common;


import lombok.Data;

/**
 * 通用返回类
 * @author ldl
 */


@Data
public class BaseResponse<T> {
    /**
     * 状态码 0 表示成功，非 0 表示失败
     */
    private int code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 描述
     */
    private String description;


    public BaseResponse(int code, String message, T data, String description) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.description = description;
    }

}
