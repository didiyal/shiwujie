package com.swj.shiwujie.data.model;

public class BaseResponse<T> {
    private int code;
    private String message;
    private String description;
    private T data;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    public T getData() {
        return data;
    }
} 