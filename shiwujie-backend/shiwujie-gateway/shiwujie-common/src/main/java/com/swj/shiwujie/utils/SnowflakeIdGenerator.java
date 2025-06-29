package com.swj.shiwujie.utils;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ddc
 * 雪花算法，用于高并发下生成随机数账号
 */
public class SnowflakeIdGenerator {
    // 起始时间戳（2024-01-01 00:00:00）
    private static final long START_TIMESTAMP = 1704067200000L;

    // 数据中心ID和机器ID所占的位数
    private static final long DATA_CENTER_BITS = 5L;
    private static final long MACHINE_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    // 最大值
    private static final long MAX_DATA_CENTER_ID = (1L << DATA_CENTER_BITS) - 1;
    private static final long MAX_MACHINE_ID = (1L << MACHINE_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    // 左移位数
    private static final long MACHINE_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_SHIFT = SEQUENCE_BITS + MACHINE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_BITS + DATA_CENTER_BITS;

    // 数据中心ID和机器ID
    private final long dataCenterId;
    private final long machineId;

    // 序列号和上次时间戳
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    // 锁保证线程安全
    private final Lock lock = new ReentrantLock();

    public SnowflakeIdGenerator(long dataCenterId, long machineId) {
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("数据中心ID超出范围");
        }
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException("机器ID超出范围");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    public long nextId() {
        lock.lock();
        try {
            long currentTimestamp = System.currentTimeMillis();

            // 如果当前时间小于上次时间，说明系统时钟回退
            if (currentTimestamp < lastTimestamp) {
                throw new RuntimeException("时钟回退，无法生成ID");
            }

            if (currentTimestamp == lastTimestamp) {
                // 同一毫秒内生成ID，增加序列号
                sequence = (sequence + 1) & MAX_SEQUENCE;
                if (sequence == 0L) {
                    // 序列号用完，等待下一毫秒
                    currentTimestamp = nextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L; // 不同毫秒，序列号重置
            }

            lastTimestamp = currentTimestamp;

            // 组合生成ID
            return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                    | (dataCenterId << DATA_CENTER_SHIFT)
                    | (machineId << MACHINE_SHIFT)
                    | sequence;
        } finally {
            lock.unlock();
        }
    }

    private long nextMillis(long lastTimestamp) {
        long currentTimestamp = System.currentTimeMillis();
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = System.currentTimeMillis();
        }
        return currentTimestamp;
    }
}
