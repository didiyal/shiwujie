package com.swj.shiwujie.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;


/**
 * Redis工具类
 */
@Component
public class RedisUtils {


    @Resource
    private RedisTemplate redisTemplate;


    public RedisUtils(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

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
     * 删除存储在 Redis 中的数据
     * @param key
     * @return
     */
    public Object removeToRedis(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 存储数据到 Redis，设置过期时间,天
     * @param key
     * @param value
     * @param expirationTime
     */
    public void setToRedis(String key, String value, long expirationTime) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, expirationTime, TimeUnit.DAYS);
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
