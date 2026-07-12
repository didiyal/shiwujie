package com.swj.shiwujie.utils;

import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ResultUtils} 单元测试——工厂方法返回的 BaseResponse 字段断言。
 * 纯静态方法，不起 Spring 上下文。
 */
@DisplayName("ResultUtils 统一返回工厂")
class ResultUtilsTest {

    @Test
    @DisplayName("success(data): code=1, message=success, data=传入, description=\"\"")
    void success_dataPopulated() {
        BaseResponse<String> resp = ResultUtils.success("hello");

        assertThat(resp.getCode()).isEqualTo(1);
        assertThat(resp.getMessage()).isEqualTo("success");
        assertThat(resp.getData()).isEqualTo("hello");
        assertThat(resp.getDescription()).isEqualTo("");
    }

    @Test
    @DisplayName("success(null data): data=null, 其余字段正常")
    void success_nullData() {
        BaseResponse<Object> resp = ResultUtils.success(null);

        assertThat(resp.getCode()).isEqualTo(1);
        assertThat(resp.getMessage()).isEqualTo("success");
        assertThat(resp.getData()).isNull();
        assertThat(resp.getDescription()).isEqualTo("");
    }

    @Test
    @DisplayName("success 泛型：传入 Integer")
    void success_genericInteger() {
        BaseResponse<Integer> resp = ResultUtils.success(42);

        assertThat(resp.getData()).isEqualTo(42);
        assertThat(resp.getCode()).isEqualTo(1);
    }

    @Test
    @DisplayName("error(code, message, description): 三参数全部回填，data=null")
    void error_codeMessageDescription() {
        BaseResponse<Object> resp = ResultUtils.error(40000, "请求参数错误", "字段缺失");

        assertThat(resp.getCode()).isEqualTo(40000);
        assertThat(resp.getMessage()).isEqualTo("请求参数错误");
        assertThat(resp.getDescription()).isEqualTo("字段缺失");
        assertThat(resp.getData()).isNull();
    }

    @Test
    @DisplayName("error(ErrorCode): 用枚举 code/message/description")
    void error_errorCodeOnly() {
        BaseResponse<Object> resp = ResultUtils.error(ErrorCode.NOT_LOGIN);

        assertThat(resp.getCode()).isEqualTo(ErrorCode.NOT_LOGIN.getCode());
        assertThat(resp.getMessage()).isEqualTo(ErrorCode.NOT_LOGIN.getMessage());
        assertThat(resp.getDescription()).isEqualTo(ErrorCode.NOT_LOGIN.getDescription());
        assertThat(resp.getData()).isNull();
    }

    @Test
    @DisplayName("error(ErrorCode, description): message 取枚举，description 自定义")
    void error_errorCodeWithCustomDescription() {
        BaseResponse<Object> resp = ResultUtils.error(ErrorCode.NO_AUTH, "无权删除该社区");

        assertThat(resp.getCode()).isEqualTo(ErrorCode.NO_AUTH.getCode());
        assertThat(resp.getMessage()).isEqualTo(ErrorCode.NO_AUTH.getMessage());
        assertThat(resp.getDescription()).isEqualTo("无权删除该社区");
        assertThat(resp.getData()).isNull();
    }

    @Test
    @DisplayName("error(SYSTEM_ERROR): code=50000")
    void error_systemError() {
        BaseResponse<Object> resp = ResultUtils.error(ErrorCode.SYSTEM_ERROR);

        assertThat(resp.getCode()).isEqualTo(50000);
    }

    @Test
    @DisplayName("error(OPERATION_ERROR): 与 SYSTEM_ERROR 共享 code=50000（已知码值）")
    void error_operationErrorSameCodeAsSystem() {
        BaseResponse<Object> op = ResultUtils.error(ErrorCode.OPERATION_ERROR);

        assertThat(op.getCode()).isEqualTo(50000);
        assertThat(op.getMessage()).isEqualTo("操作失败");
    }
}
