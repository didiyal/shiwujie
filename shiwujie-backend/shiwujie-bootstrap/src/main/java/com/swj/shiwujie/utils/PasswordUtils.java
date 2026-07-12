package com.swj.shiwujie.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;

/**
 * 密码工具：BCrypt 存储与校验，兼容历史无盐 MD5（登录通过即懒升级）。
 *
 * <p>历史 {@link SecureUtil#md5(String)} 产物为 32 位小写 hex；
 * BCrypt 产物为 {@code $2a$10$...}（60 字符），长度天然可区分。
 * 身份证 / 残疾证等 PII 单向哈希仍走 {@link SecureUtil#md5(String)}，与口令无关。
 *
 * @author swj
 */
public final class PasswordUtils {

    private PasswordUtils() {
    }

    /**
     * 口令哈希（BCrypt，随机盐，cost=10）。
     */
    public static String hash(String plainPassword) {
        ThrowUtils.throwIf(plainPassword == null || plainPassword.isEmpty(),
                ErrorCode.PARAMS_ERROR, "密码不能为空");
        return BCrypt.hashpw(plainPassword);
    }

    /**
     * 校验明文口令是否匹配库存值（null-safe；兼容历史 MD5 与 BCrypt）。
     * <ul>
     *   <li>库存为 null（快注册未设密码）→ 返 false。</li>
     *   <li>库存为历史 MD5（32 位 hex）→ 用 MD5 比对。</li>
     *   <li>否则（BCrypt）→ 用 BCrypt 校验。</li>
     * </ul>
     */
    public static boolean matches(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) {
            return false;
        }
        if (isLegacyMd5(storedPassword)) {
            return SecureUtil.md5(plainPassword).equals(storedPassword);
        }
        return BCrypt.checkpw(plainPassword, storedPassword);
    }

    /**
     * 判断库存值是否为历史无盐 MD5（32 位 hex）。
     */
    public static boolean isLegacyMd5(String storedPassword) {
        return storedPassword != null
                && storedPassword.length() == 32
                && storedPassword.matches("[0-9a-fA-F]{32}");
    }
}
