package com.swj.shiwujie.constants;


/**
 * JWT常量
 */
public interface UserConstants {


    /**
     * token生成秘钥
     */
    String TOKEN_SECRETKEY = "TOKEN_SECRETKEY";


    /**
     * redis储存秘钥
     */
    String REDIS_SECRETKEY = "REDIS_SECRETKEY";


    /**
     * 用户密码匹配正则表达式
     */
    String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{1,}$";



    /**
     * 残疾人证匹配正则表达式
     * 格式：18位身份证号 + 1位残疾类别(0-6) + 1位残疾等级(1-4)
     */
    String BLIND_REGEX = "^\\d{17}(\\d|X)[0-6][1-4]$";
}
