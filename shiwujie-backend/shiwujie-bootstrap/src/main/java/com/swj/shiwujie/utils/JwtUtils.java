package com.swj.shiwujie.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.signers.JWTSignerUtil;

import java.util.Map;
import java.time.Duration;

/**
 * JWT 工具类（基于 Hutool 实现）
 */
public class JwtUtils {

    /**
     * 生成 JWT Token
     *
     * @param payload     载荷数据（自定义声明）
     * @param secretKey   签名密钥
     * @param expireTime  过期时间（可为 null，null 表示不过期）
     * @return 生成的 JWT 字符串
     */
    public static String generateToken(Map<String, Object> payload,
                                       String secretKey,
                                       Duration expireTime) {
        // 创建 HS256 签名器（基于 HMAC-SHA256 算法）
        JWTSigner signer = JWTSignerUtil.hs256(secretKey.getBytes());

        // 初始化 JWT 构建器
        JWT jwt = JWT.create()
                .setSigner(signer)  // 设置签名器
                .addPayloads(payload);  // 添加自定义载荷

        // 设置过期时间（若不为 null）
        if (expireTime != null) {
            long expireMillis = DateUtil.current() + expireTime.toMillis();
            // 转换为Date类型
            jwt.setExpiresAt(DateUtil.date(expireMillis));
        }

        // 生成并返回 Token
        return jwt.sign();
    }

    /**
     * 校验并解析 JWT Token
     *
     * @param token       待校验的 JWT 字符串
     * @param secretKey   签名密钥
     * @param ignoreExp   是否忽略过期时间校验
     * @return 解析后的载荷数据
     * @throws RuntimeException 校验失败时抛出异常（如签名错误、过期等）
     */
    public static Map<String, Object> validateToken(String token,
                                                    String secretKey,
                                                    boolean ignoreExp) {
        // 解析 Token
        JWT jwt = JWTUtil.parseToken(token);

        // 验证签名（使用 HS256 算法）
        try {
            jwt.verify(JWTSignerUtil.hs256(secretKey.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Token 签名验证失败", e);
        }

        // 验证算法一致性（确保 header 中的算法与使用的算法匹配）
        JWTValidator.of(token)
                .validateAlgorithm(JWTSignerUtil.hs256(secretKey.getBytes()));

        // 验证时间（若不忽略过期时间）
        if (!ignoreExp) {
            JWTValidator.of(token)
                    .validateDate(DateUtil.date());  // 校验签发时间、生效时间、过期时间
        }

        // 返回解析后的载荷（包含标准声明和自定义声明）
        return jwt.getPayloads();
    }
}