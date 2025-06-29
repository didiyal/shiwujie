package com.swj.shiwujie.exception;


import com.swj.shiwujie.common.ErrorCode;

/**
 * 自定义异常
 * @author ldl
 */
public class BusinessException extends RuntimeException{

    /**
     * 错误码
     */
    private int code;

    /**
     * 错误信息
     */
    private String description;


    /**
     * 构造方法,手动传入错误码和错误信息
     * @param code 错误码
     * @param message 错误信息
     * @param description 错误描述
     */
    public BusinessException(int code,String message, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }


    /**
     * 构造方法,使用已存在的错误码
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    /**
     * 构造方法,使用已存在的错误码,自定义错误描述
     * @param errorCode 错误码
     * @param description 错误描述
     */
    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public int getCode(){
        return code;
    }
    public String getDescription() {
        return description;
    }
}
