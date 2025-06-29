package com.swj.shiwujie.model.VO;


import lombok.Data;

@Data
public class VideoVO {
    /**
     * 频道号
     */
    private String channel;

    /**
     * 返回给本人的uid,既可以是盲人也可以是志愿者
     * 现指用户账号
     */
    private String uid;
}
