package com.swj.shiwujie.utils;

import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.Base64;

@Slf4j
public class JWTUtils {




    // 使用 Keys.secretKeyFor 方法来生成安全的密钥
    // 替换为固定密钥（Base64编码的512-bit密钥）
    private static final String BASE64_SECRET = "7A2B5E9C4F8D1A3B6E0C7D2A5F8B3D6E4A1B9C0D3E6F2A5B8D1C4E9F0A3B6C5D8" +
            "7A2B5E9C4F8D1A3B6E0C7D2A5F8B3D6E4A1B9C0D3E6F2A5B8D1C4E9F0A3B6C5D8";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            Base64.getDecoder().decode(BASE64_SECRET)
    );
    private static final long EXPIRATION_TIME = 43200000L; // 12 小时的过期时间

    /**
     * 生成JWT令牌
     *
     * @param userId JWT第二部分负载(payload)中存储的内容
     * @return 生成的JWT字符串
     */
    public static String generateJwt(String userId) {
        // 构建JWT令牌
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    /**
     * 解析JWT令牌
     *
     * @param token JWT令牌
     * @return JWT第二部分负载(payload)中存储的内容
     */
    public static boolean parseJWT(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true; // 令牌合法
        } catch (JwtException | IllegalArgumentException e) {
            return false; // 令牌无效
        }
    }

    /**
     * 生成一个256位的随机密钥
     */
    // 提取用户信息（UserId）从令牌
    public static String extractUserId(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

}
