package com.swj.shiwujie.utils;


import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 随机数工具类
 * @author  ldl
 */
@Component
public class RadomUtils {


    /**
     * 生成根据时间随机字符串
     * @return
     */
    public synchronized static String generateRandomString() {
        Random random = new Random(System.currentTimeMillis());
        String string = String.valueOf(Math.abs(random.nextInt()));
        return string;
    }



}
