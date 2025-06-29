package com.swj.shiwujie.service;


import com.swj.shiwujie.utils.SnowFlakeUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.Random;

@SpringBootTest
@Slf4j
public class FamilyServiceTest {


    @Test
    void testRadomByTime(){
        Random random = new Random(System.currentTimeMillis());
        String familyAccount = String.valueOf(Math.abs(random.nextInt()));
        log.info(familyAccount);

    }

    @Test
    void testRadomBySnowFlake(){
        SnowFlakeUtils snowFlakeUtils = new SnowFlakeUtils();

        for (int i = 0; i < 10; i++) {
            log.info(String.valueOf(snowFlakeUtils.snowflakeId(0,i)).substring(10));
        }
    }

}
