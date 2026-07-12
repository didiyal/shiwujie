package com.swj.shiwujie.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.hutool.core.convert.Convert;

/**
 * {@link JwtUtils} 单元测试——Hutool HS256 生成 / 校验 / 过期。
 * 纯静态方法，不起 Spring 上下文，用真实密钥做往返。
 */
@DisplayName("JwtUtils JWT 工具")
class JwtUtilsTest {

    private static final String SECRET = "test-secret-key";

    private Map<String, Object> samplePayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("blindId", 1001L);
        payload.put("isBlind", true);
        payload.put("phone", "13800138000");
        payload.put("role", 2L);
        return payload;
    }

    @Test
    @DisplayName("generateToken → validateToken(ignoreExp=true) 往返：payload 字段取回")
    void roundTrip_retrievesPayloadFields() {
        Map<String, Object> payload = samplePayload();
        String token = JwtUtils.generateToken(payload, SECRET, Duration.ofHours(1));

        Map<String, Object> parsed = JwtUtils.validateToken(token, SECRET, true);

        assertThat(parsed).isNotNull();
        // Hutool 解析 payload 时把 JSON 数字按取值范围窄化：1001/2 这类 int 范围值
        // 反序列化成 Integer 而非 Long，按值比较而非按类型比较（Convert 转 Long）。
        assertThat(Convert.toLong(parsed.get("blindId"))).isEqualTo(1001L);
        assertThat(parsed.get("isBlind")).isEqualTo(true);
        assertThat(parsed.get("phone")).isEqualTo("13800138000");
        assertThat(Convert.toLong(parsed.get("role"))).isEqualTo(2L);
    }

    @Test
    @DisplayName("错 secret validateToken 抛异常（签名验证失败）")
    void validateToken_wrongSecretThrows() {
        String token = JwtUtils.generateToken(samplePayload(), SECRET, Duration.ofHours(1));

        // Hutool 5.8.25 真实行为：JWT.verify(signer) 返回 boolean（签名不匹配不抛异常），
        // 故 JwtUtils 第 65 行的 verify + catch 包不到错密钥场景；真正抛点在
        // validateAlgorithm(signer)（JwtUtils.java:72）——它内部再调 verify，false 即抛
        // cn.hutool.core.exceptions.ValidateException("Signature verification failed!")，
        // 该异常未进 JwtUtils 的 catch，故以原样冒泡（ValidateException 是 RuntimeException 子类）。
        assertThatThrownBy(() -> JwtUtils.validateToken(token, "wrong-secret", true))
                .isInstanceOfAny(RuntimeException.class)
                .hasMessageContaining("Signature verification failed!");
    }

    @Test
    @DisplayName("ignoreExp=false + 过期 token → 抛异常")
    void validateToken_expiredNotIgnoredThrows() {
        // 构造一个过期 token（expireTime 为负）
        String token = JwtUtils.generateToken(samplePayload(), SECRET, Duration.ofMillis(-1000));

        assertThatThrownBy(() -> JwtUtils.validateToken(token, SECRET, false))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("ignoreExp=true + 过期 token → 不抛，能取 payload")
    void validateToken_expiredIgnoredOk() {
        String token = JwtUtils.generateToken(samplePayload(), SECRET, Duration.ofMillis(-1000));

        // ignoreExp=true 应放过过期校验，正常返回 payload
        Map<String, Object> parsed = JwtUtils.validateToken(token, SECRET, true);

        assertThat(parsed).isNotNull();
        assertThat(parsed.get("phone")).isEqualTo("13800138000");
    }

    @Test
    @DisplayName("expireTime=null → 不过期，validateToken(ignoreExp=false) 不抛")
    void generateToken_nullExpireNoExpiry() {
        String token = JwtUtils.generateToken(samplePayload(), SECRET, null);

        // 无过期声明，validateToken 即使校验时间也不应抛
        Map<String, Object> parsed = JwtUtils.validateToken(token, SECRET, false);

        assertThat(parsed).isNotNull();
        // int 范围数字经 Hutool JSON 反序列化窄化为 Integer，按值比较（见 roundTrip 注释）
        assertThat(Convert.toLong(parsed.get("blindId"))).isEqualTo(1001L);
    }

    @Test
    @DisplayName("正常未过期 token + ignoreExp=false → 不抛，取 payload")
    void validateToken_unexpiredNotIgnoredOk() {
        String token = JwtUtils.generateToken(samplePayload(), SECRET, Duration.ofHours(1));

        Map<String, Object> parsed = JwtUtils.validateToken(token, SECRET, false);

        assertThat(parsed).isNotNull();
        // int 范围数字经 Hutool JSON 反序列化窄化为 Integer，按值比较（见 roundTrip 注释）
        assertThat(Convert.toLong(parsed.get("blindId"))).isEqualTo(1001L);
    }
}
