package com.swj.shiwujie.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SnowFlakeUtils {
    /**
     * 工作机器ID(0~31)，2进制5位  32位减掉1位 31个
     */
    private long workerId = 0;
    /**
     * 数据中心ID(0~31)，2进制5位  32位减掉1位 31个
     */
    private long datacenterId = 1;

    /**
     * 雪花算法对象
     */
    private Snowflake snowFlake = IdUtil.createSnowflake(workerId, datacenterId);

    @PostConstruct
    public void init() {
        try {
            // 将网络ip转换成long
            workerId = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取雪花ID，默认使用网络IP作为工作机器ID
     *
     * @return ID
     */
    public synchronized long snowflakeId() {
        return this.snowFlake.nextId();
    }

    /**
     * 获取雪花ID
     *
     * @param workerId     工作机器ID
     * @param datacenterId 数据中心ID
     * @return ID
     */
    public synchronized  long snowflakeId(long workerId, long datacenterId) {
        Snowflake snowflake = IdUtil.createSnowflake(workerId, datacenterId);
        return snowflake.nextId();
    }

}
