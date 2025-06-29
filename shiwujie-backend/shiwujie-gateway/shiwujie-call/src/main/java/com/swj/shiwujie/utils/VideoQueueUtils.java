package com.swj.shiwujie.utils;


import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

@Component
public class VideoQueueUtils {



    /**
     * 将object转化成Queue
     * @param obj
     * @return
     */
    public static   Queue<String> convertToQueue(Object obj) {
        if (obj instanceof LinkedList<?>) {
            LinkedList<?> linkedList = (LinkedList<?>) obj;

            // 检查元素类型是否为 String
            if (linkedList.stream().allMatch(e -> e instanceof String)) {
                @SuppressWarnings("unchecked")
                Queue<String> queue = (Queue<String>) linkedList;
                return queue;
            } else {
                throw new ClassCastException("LinkedList 中的元素不是 String 类型");
            }
        } else {
            throw new ClassCastException("对象不是 LinkedList 的实例");
        }
    }
}
