package com.swj.shiwujie.data.model;

/**
 * API响应的基础包装类
 * @param <T> 响应数据的类型
 */
public class BaseResponse<T> {
    /**
     * 响应码
     * - 0: 成功
     * - 40000: 需要重新选择身份
     * - 40010: token失效，需要重新登录
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
} 