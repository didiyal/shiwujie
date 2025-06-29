package com.swj.shiwujie.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {


    @Resource
    private RedisTemplate redisTemplate;


    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 获取存储在 Redis 中的数据
    public Object getFromRedis(String key) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    // 存储数据到 Redis，设置过期时间
    public void setToRedis(String key, String value, long expirationTime) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, expirationTime, TimeUnit.SECONDS);
    }

    // 检查 Redis 中的数据是否过期
    public boolean isKeyExpired(String key) {
        return redisTemplate.hasKey(key) == null;  // 返回 null 表示数据已经过期
    }

    // 续期 Redis 数据的有效期
    public void renewKey(String key, long expirationTime) {
        redisTemplate.expire(key, expirationTime, TimeUnit.SECONDS);
    }
}
