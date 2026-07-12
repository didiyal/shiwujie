package com.swj.shiwujie.utils;

import cn.hutool.crypto.SecureUtil;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link PasswordUtils} 单元测试——BCrypt 口令哈希 / 校验 / 历史 MD5 兼容。
 * 纯静态方法，不起 Spring 上下文。
 */
@DisplayName("PasswordUtils 密码工具")
class PasswordUtilsTest {

    // ---------- hash ----------

    @Nested
    @DisplayName("hash(plain): BCrypt 哈希")
    class Hash {

        @Test
        @DisplayName("正常明文：返回 60 字符且以 $2a$10$ 开头（cost=10）")
        void hash_returnsBcryptFormat() {
            String hashed = PasswordUtils.hash("abc123");

            assertThat(hashed).hasSize(60);
            assertThat(hashed).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("同明文两次 hash 因随机盐而不同")
        void hash_differentSaltEachCall() {
            String a = PasswordUtils.hash("samePass1");
            String b = PasswordUtils.hash("samePass1");

            assertThat(a).isNotEqualTo(b);
            assertThat(a).startsWith("$2a$10$");
            assertThat(b).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("hash(null) 抛 BusinessException(PARAMS_ERROR)")
        void hash_nullThrows() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> PasswordUtils.hash(null));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("hash(\"\") 抛 BusinessException(PARAMS_ERROR)")
        void hash_emptyThrows() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> PasswordUtils.hash(""));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }
    }

    // ---------- matches ----------

    @Nested
    @DisplayName("matches(plain, stored): 校验")
    class Matches {

        @Test
        @DisplayName("BCrypt 往返：matches(p, hash(p)) == true")
        void matches_bcryptRoundTrip() {
            String plain = "abc123";
            String stored = PasswordUtils.hash(plain);

            assertThat(PasswordUtils.matches(plain, stored)).isTrue();
        }

        @Test
        @DisplayName("BCrypt 错明文 → false")
        void matches_bcryptWrongPlain() {
            String stored = PasswordUtils.hash("abc123");

            assertThat(PasswordUtils.matches("wrongPwd", stored)).isFalse();
        }

        @Test
        @DisplayName("stored=null → false（快注册未设密码场景）")
        void matches_storedNull() {
            assertThat(PasswordUtils.matches("abc123", null)).isFalse();
        }

        @Test
        @DisplayName("plain=null → false")
        void matches_plainNull() {
            assertThat(PasswordUtils.matches(null, "$2a$10$someStoredValueHereAaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")).isFalse();
        }

        @Test
        @DisplayName("兼容 legacy MD5：matches(\"abc123\", md5(\"abc123\")) == true")
        void matches_legacyMd5Correct() {
            String md5 = SecureUtil.md5("abc123"); // 32 位小写 hex

            assertThat(PasswordUtils.matches("abc123", md5)).isTrue();
        }

        @Test
        @DisplayName("legacy MD5 错明文 → false")
        void matches_legacyMd5Wrong() {
            String md5 = SecureUtil.md5("abc123");

            assertThat(PasswordUtils.matches("wrongPwd", md5)).isFalse();
        }

        @Test
        @DisplayName("非 hex、非 BCrypt 的怪串 → false（不抛异常）")
        void matches_garbageStored_returnsFalseNotThrows() {
            // 既不是 32 位 hex，也不是合法 BCrypt → BCrypt.checkpw 内部异常被吞，返回 false
            assertThat(PasswordUtils.matches("abc123", "not-a-valid-hash-zzz")).isFalse();
        }

        @Test
        @DisplayName("60 字符但非合法 BCrypt 串 → false（不抛异常）")
        void matches_sixtyCharsButNotBcrypt_returnsFalse() {
            // 长度 60 但前缀不对，BCrypt.checkpw 应判定非法
            String fake = "012345678901234567890123456789012345678901234567890123456789";
            assertThat(PasswordUtils.matches("abc123", fake)).isFalse();
        }
    }

    // ---------- isLegacyMd5 ----------

    @Nested
    @DisplayName("isLegacyMd5(stored): 历史 MD5 判定")
    class IsLegacyMd5 {

        @Test
        @DisplayName("32 位小写 hex → true")
        void isLegacyMd5_lowercaseHex() {
            assertThat(PasswordUtils.isLegacyMd5(SecureUtil.md5("abc123"))).isTrue();
        }

        @Test
        @DisplayName("32 位大写 hex → true（实现允许 a-fA-F）")
        void isLegacyMd5_uppercaseHex() {
            String upper = SecureUtil.md5("abc123").toUpperCase();
            assertThat(PasswordUtils.isLegacyMd5(upper)).isTrue();
        }

        @Test
        @DisplayName("null → false")
        void isLegacyMd5_null() {
            assertThat(PasswordUtils.isLegacyMd5(null)).isFalse();
        }

        @Test
        @DisplayName("33 位 hex → false（长度不符）")
        void isLegacyMd5_wrongLength33() {
            String md5 = SecureUtil.md5("abc123") + "a"; // 33 字符
            assertThat(PasswordUtils.isLegacyMd5(md5)).isFalse();
        }

        @Test
        @DisplayName("BCrypt 60 串 → false")
        void isLegacyMd5_bcryptString() {
            String bcrypt = PasswordUtils.hash("abc123");
            assertThat(PasswordUtils.isLegacyMd5(bcrypt)).isFalse();
        }

        @Test
        @DisplayName("含非 hex 字符（g）→ false")
        void isLegacyMd5_containsNonHex_g() {
            String bad = "g".repeat(32);
            assertThat(PasswordUtils.isLegacyMd5(bad)).isFalse();
        }

        @Test
        @DisplayName("含非 hex 字符（-）→ false")
        void isLegacyMd5_containsNonHex_dash() {
            String bad = "-".repeat(32);
            assertThat(PasswordUtils.isLegacyMd5(bad)).isFalse();
        }

        @Test
        @DisplayName("31 位 hex → false")
        void isLegacyMd5_wrongLength31() {
            String md5 = SecureUtil.md5("abc123").substring(1); // 31 字符
            assertThat(PasswordUtils.isLegacyMd5(md5)).isFalse();
        }
    }
}
