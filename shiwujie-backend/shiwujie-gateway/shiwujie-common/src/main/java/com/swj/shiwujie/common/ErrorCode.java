package com.swj.shiwujie.common;



/**
 * 错误码
 * @author ldl
 */

public enum ErrorCode {
    PARAMS_ERROR(40000, "请求参数错误", ""),
    NOT_LOGIN(40010, "未登录", ""),
    NO_AUTH(40030, "无权限", ""),
    SYSTEM_ERROR(50000, "系统内部异常", ""),
    OPERATION_ERROR(50000, "操作失败", "");

    /**
     * 状态码
     */
    private  int code;

    /**
     * 错误信息
     */
    private  String message;

    /**
     * 错误描述
     */
    private  String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
