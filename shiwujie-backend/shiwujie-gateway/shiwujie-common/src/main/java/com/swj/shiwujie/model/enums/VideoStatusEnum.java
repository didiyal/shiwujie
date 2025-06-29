package com.swj.shiwujie.model.enums;


import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum VideoStatusEnum {


    WAITING_CALL("志愿者等待通话中",0),
    CALLING("正在通话",1),
    CALL_END("通话正常结束",2),
    CALL_CANCEL("通话取消",3);


    private final String status;
    private final Integer value;

    VideoStatusEnum(String status, Integer value){
        this.status = status;
        this.value = value;
    }

    public static VideoStatusEnum  getEnumByValue(Integer value){
        if(value == null){
            return null;
        }
        Map<Integer,VideoStatusEnum> videoStatusEnumMap = Arrays.stream(VideoStatusEnum.values()).
                collect(Collectors.toMap(VideoStatusEnum::getValue,VideoStatusEnum ->VideoStatusEnum));
        return  videoStatusEnumMap.getOrDefault(value,null);
    }
}
