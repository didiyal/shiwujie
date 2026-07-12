package com.swj.shiwujie.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ConverterUtils} 单元测试。
 * 逻辑薄：null 短路 + 委托 Hutool Convert。覆盖主要分支。
 */
@DisplayName("ConverterUtils 类型转换")
class ConverterUtilsTest {

    @Test
    @DisplayName("ObjToQueueLong(null) → null（短路分支）")
    void objToQueueLong_nullReturnsNull() {
        assertThat(ConverterUtils.ObjToQueueLong(null)).isNull();
    }

    @Test
    @DisplayName("ObjToQueueLong(List<Long>) → Queue<Long>（Hutool Convert 转换）")
    void objToQueueLong_listToQueue() {
        Queue<Long> result = ConverterUtils.ObjToQueueLong(Arrays.asList(1L, 2L, 3L));

        assertThat(result).isNotNull();
        // Hutool Convert 转 Queue 默认产 LinkedList
        assertThat(result).isInstanceOf(LinkedList.class);
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("ObjToQueueLong(空集合) → 空 Queue（非 null）")
    void objToQueueLong_emptyCollection() {
        Queue<Long> result = ConverterUtils.ObjToQueueLong(Arrays.asList());

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ObjToQueueLong(单元素) → 单元素 Queue")
    void objToQueueLong_singleElement() {
        Queue<Long> result = ConverterUtils.ObjToQueueLong(Arrays.asList(42L));

        assertThat(result).containsExactly(42L);
    }

    @Test
    @DisplayName("ObjToQueueLong(Integer 集合) → 自动转 Long Queue（Hutool 数值转换）")
    void objToQueueLong_integersUpcastToLong() {
        Queue<Long> result = ConverterUtils.ObjToQueueLong(Arrays.asList(7, 8));

        assertThat(result).containsExactly(7L, 8L);
    }
}
