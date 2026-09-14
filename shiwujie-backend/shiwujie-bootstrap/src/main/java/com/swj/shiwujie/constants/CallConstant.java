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
     * 滞留 30 天，僵尸志愿者在队首被反复 poll。现显式按秒设置（滑动窗口，每次入/出队/匹配均重置）。</p>
     *
     * <p>2026-09-14：30s → 1h。志愿者点「接听电话」后只要不主动取消，1 小时内一直可被匹配，
     * 无需反复重新入队；志愿者离线由 joinVideohelp 原子匹配跳过兜底（matchSuccess 失败即标记
     * 该候选已取消并尝试下一位），长挂队列不会被离线志愿者卡死。</p>
     */
    long VOLUNTEER_QUEUE_TTL_SECONDS = 3600L;
}
