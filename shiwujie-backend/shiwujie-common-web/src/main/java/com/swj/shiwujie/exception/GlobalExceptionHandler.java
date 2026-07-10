package com.swj.shiwujie.exception;



import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器,处理所有异常
 * @author ldl
 *
 * <p>v3.0.0 单体化阶段2.5：补 {@code @Hidden}（合并自 ai 副本），对 ai 与业务链路统一生效。</p>
 */
@Slf4j
@RestControllerAdvice
@Hidden // 解决高版本springboot swagger-ui.html无法访问问题
public class GlobalExceptionHandler {

    /**
     * 处理自定义异常
     * @param e 自定义异常
     * @return BaseResponse统一返回类
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e){
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    /**
     * 处理其他异常
     * @param e 异常
     * @return BaseResponse统一返回类
     */

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e){
        log.error("runtimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage(), "");
    }


}
