package com.swj.shiwujie.common;



/**
 * 错误码
 * @author ldl
 */

public enum ErrorCode {
    PARAMS_ERROR(40000, "请求参数错误", ""),
    NOT_LOGIN(40010, "未登录", ""),
    CHECK_FAILURE(40020,"参数校验失败(包括密码、用户名、账号校验失败)",""),
    NO_AUTH(40030, "无权限", ""),
    NOT_FOUND(40040, "请求未找到", ""),
    UNAUTHORIZED(40049,"无权限",""),
    USER_ERROR(40050,"获取用户信息失败",""),
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
