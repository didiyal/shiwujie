package com.swj.shiwujie.utils;


import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 通用返回类
 * @author ldl
 */
@Component
public class ResultUtils {


    /**
     * 成功
     * @param data 返回的数据
     * @return BaseResponse统一返回类
     * @param <T> 泛型
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(1, "success", data, "");
    }

    /**
     * 错误,自定义错误码与信息
     * @param code 错误码
     * @param message 错误信息
     * @param description 错误描述
     * @return BaseResponse统一返回类
     * @param <T> 泛型
     */

    public static <T> BaseResponse<T> error(int code, String message,String description){
        return new BaseResponse<>(code, message, null, description);
    }


    /**
     * 错误，使用已存在的错误码
     * @param errorCode 错误码
     * @return BaseResponse统一返回类
     * @param <T> 泛型
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode.getCode(), errorCode.getMessage(), null, errorCode.getDescription());
    }


    /**
     * 错误，使用已存在的错误码，自定义错误描述
     * @param errorCode 错误码
     * @param description 错误描述
     * @return BaseResponse统一返回类
     * @param <T> 泛型
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String description){
        return new BaseResponse<>(errorCode.getCode(), errorCode.getMessage(), null, description);
    }
}
