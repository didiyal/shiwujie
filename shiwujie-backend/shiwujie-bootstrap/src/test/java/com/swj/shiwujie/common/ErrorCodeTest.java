package com.swj.shiwujie.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ErrorCode} 轻测——码值契约锁死，防误改。
 * 纯枚举，不起 Spring 上下文。
 */
@DisplayName("ErrorCode 码值锁")
class ErrorCodeTest {

    @Test
    @DisplayName("PARAMS_ERROR: code=40000, message=请求参数错误")
    void paramsError() {
        assertThat(ErrorCode.PARAMS_ERROR.getCode()).isEqualTo(40000);
        assertThat(ErrorCode.PARAMS_ERROR.getMessage()).isEqualTo("请求参数错误");
        assertThat(ErrorCode.PARAMS_ERROR.getDescription()).isEqualTo("");
    }

    @Test
    @DisplayName("NOT_LOGIN: code=40010, message=未登录")
    void notLogin() {
        assertThat(ErrorCode.NOT_LOGIN.getCode()).isEqualTo(40010);
        assertThat(ErrorCode.NOT_LOGIN.getMessage()).isEqualTo("未登录");
    }

    @Test
    @DisplayName("NO_AUTH: code=40030, message=无权限")
    void noAuth() {
        assertThat(ErrorCode.NO_AUTH.getCode()).isEqualTo(40030);
        assertThat(ErrorCode.NO_AUTH.getMessage()).isEqualTo("无权限");
    }

    @Test
    @DisplayName("SYSTEM_ERROR: code=50000, message=系统内部异常")
    void systemError() {
        assertThat(ErrorCode.SYSTEM_ERROR.getCode()).isEqualTo(50000);
        assertThat(ErrorCode.SYSTEM_ERROR.getMessage()).isEqualTo("系统内部异常");
    }

    @Test
    @DisplayName("OPERATION_ERROR: 与 SYSTEM_ERROR 共享 code=50000（已知码值，message 区分）")
    void operationError() {
        assertThat(ErrorCode.OPERATION_ERROR.getCode()).isEqualTo(50000);
        assertThat(ErrorCode.OPERATION_ERROR.getMessage()).isEqualTo("操作失败");
    }

    @Test
    @DisplayName("枚举完整：恰好 5 项")
    void enumCount() {
        assertThat(ErrorCode.values()).hasSize(5);
    }

    @Test
    @DisplayName("枚举顺序契约：按声明序 PARAMS_ERROR=0 ... OPERATION_ERROR=4")
    void enumOrder() {
        assertThat(ErrorCode.values())
                .containsExactly(
                        ErrorCode.PARAMS_ERROR,
                        ErrorCode.NOT_LOGIN,
                        ErrorCode.NO_AUTH,
                        ErrorCode.SYSTEM_ERROR,
                        ErrorCode.OPERATION_ERROR);
    }

    @Test
    @DisplayName("所有 description 默认空串")
    void allDescriptionsEmptyByDefault() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(ec.getDescription()).isEqualTo("");
        }
    }
}
