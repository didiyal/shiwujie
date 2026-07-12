package com.swj.shiwujie.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;


/**
 * Redis工具类
 *
 * <p>2026-07-12 修复：移除与 {@code @Resource} 字段注入冲突的 {@code StringRedisTemplate} 构造器
 * （Spring 走唯一构造器注入，实际注入的是 Spring Boot 自动配置的 {@code StringRedisTemplate}，
 * 其 StringRedisSerializer 无法序列化 {@code LinkedList<Long>} 等非 String 值，导致视频求助匹配队列
 * 在首次 {@code createVideohelp} 即 ClassCastException）。现回归字段注入 {@code RedisTemplateConfig}
 * 配置的 {@code RedisTemplate<String,Object>} bean（值走默认 JDK 序列化，String token 与 LinkedList&lt;Long&gt;
 * 队列均可正确往返）。</p>
 */
@Component
public class RedisUtils{


    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取存储在 Redis 中的数据
     * @param key
     * @return
     */
    public Object getFromRedis(String key) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }


    /**
     * 检查redis是否存在
     * @param key
     * @return
     */
    public Object hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除存储在 Redis 中的数据
     * @param key
     * @return
     */
    public Object removeToRedis(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 存储数据到 Redis，设置过期时间（单位：天）。登录 token 等长生命周期值用此方法。
     * @param key
     * @param value
     * @param expirationTime 过期天数
     */
    public void setToRedis(String key, Object value, long expirationTime) {
        setToRedis(key, value, expirationTime, TimeUnit.DAYS);
    }

    /**
     * 存储数据到 Redis，指定过期单位。视频求助匹配队列等短生命周期值用此方法传 {@link TimeUnit#SECONDS}。
     * @param key
     * @param value
     * @param timeout 过期数值
     * @param unit 过期单位
     */
    public void setToRedis(String key, Object value, long timeout, TimeUnit unit) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, timeout, unit);
    }


    /**
     * 检查 Redis 中的数据是否过期,返回 null 表示数据已经过期
     * @param key
     * @return
     */
    public boolean isKeyExpired(String key) {
        return redisTemplate.hasKey(key) == null;
    }

    /**
     * 续期 Redis 数据的有效期
     * @param key
     * @param expirationTime
     */
    public void renewKey(String key, long expirationTime) {
        redisTemplate.expire(key, expirationTime, TimeUnit.DAYS);
    }
}
