package com.swj.shiwujie.exception;

import com.swj.shiwujie.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ThrowUtils} 单元测试——三个 throwIf 重载。
 * 纯静态方法，不起 Spring 上下文。
 */
@DisplayName("ThrowUtils 条件抛异常")
class ThrowUtilsTest {

    // ---------- throwIf(cond, RuntimeException) ----------

    @Test
    @DisplayName("throwIf(true, RuntimeException): 抛传入的异常实例")
    void throwIf_runtimeException_true() {
        RuntimeException custom = new RuntimeException("自定义业务异常");

        assertThatThrownBy(() -> ThrowUtils.throwIf(true, custom))
                .isSameAs(custom)
                .hasMessage("自定义业务异常");
    }

    @Test
    @DisplayName("throwIf(false, RuntimeException): 静默不抛")
    void throwIf_runtimeException_false() {
        assertThatCode(() -> ThrowUtils.throwIf(false, new RuntimeException("不应抛")))
                .doesNotThrowAnyException();
    }

    // ---------- throwIf(cond, ErrorCode) ----------

    @Test
    @DisplayName("throwIf(true, ErrorCode): 抛 BusinessException，code/message 取枚举")
    void throwIf_errorCode_true() {
        assertThatThrownBy(() -> ThrowUtils.throwIf(true, ErrorCode.NOT_LOGIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ErrorCode.NOT_LOGIN.getCode());
                    assertThat(be.getMessage()).isEqualTo("未登录");
                });
    }

    @Test
    @DisplayName("throwIf(false, ErrorCode): 静默不抛")
    void throwIf_errorCode_false() {
        assertThatCode(() -> ThrowUtils.throwIf(false, ErrorCode.NO_AUTH))
                .doesNotThrowAnyException();
    }

    // ---------- throwIf(cond, ErrorCode, message) ----------

    @Test
    @DisplayName("throwIf(true, ErrorCode, msg): 抛 BusinessException，code 取枚举、message 覆盖")
    void throwIf_errorCodeMsg_true() {
        assertThatThrownBy(() -> ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "密码不能为空"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
                    assertThat(be.getMessage()).isEqualTo("密码不能为空");
                });
    }

    @Test
    @DisplayName("throwIf(false, ErrorCode, msg): 静默不抛")
    void throwIf_errorCodeMsg_false() {
        assertThatCode(() -> ThrowUtils.throwIf(false, ErrorCode.SYSTEM_ERROR, "不应抛"))
                .doesNotThrowAnyException();
    }

    // ---------- 边界：PasswordUtils 集成用法（验证约定契约成立） ----------

    @Test
    @DisplayName("契约验证：BusinessException(ErrorCode, message) 的 getCode() 取枚举 code，不含自定义 message 影响")
    void businessException_codeFromErrorCode() {
        // PasswordUtils.hash 内部正是 throwIf(null|empty, PARAMS_ERROR, "密码不能为空")
        BusinessException ex = new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");

        assertThat(ex.getCode()).isEqualTo(40000);
        assertThat(ex.getMessage()).isEqualTo("密码不能为空");
    }
}
