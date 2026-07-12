package com.swj.shiwujie.constants;

/**
 * 视频公共常量
 */
public interface CallConstant {


    /**
     * 志愿者队列储redis存键
     */
    String VOLUNTEER_QUEUE_REDIS = "VOLUNTEER_QUEUE_REDIS_KEY";

    /**
     * 志愿者匹配队列 Redis 过期秒数。
     *
     * <p>2026-07-12 修复：原 {@code RedisUtils.setToRedis(..., 30L)} 走 {@code TimeUnit.DAYS}，队列实际
     * 滞留 30 天，僵尸志愿者在队首被反复 poll。现显式按秒设置（滑动窗口，每次入/出队/匹配均重置）。
     * 匹配窗口是否取 30s 待产品确认，可在此常量调整。</p>
     */
    long VOLUNTEER_QUEUE_TTL_SECONDS = 30L;
}
