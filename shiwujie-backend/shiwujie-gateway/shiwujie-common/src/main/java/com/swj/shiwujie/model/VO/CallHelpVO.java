package com.swj.shiwujie.model.VO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallHelpVO {
    /**
     * 频道号
     */
    private String channel;

    /**
     * 返回给本人的uid,盲人
     * 现指用户账号
     */
    private String uid;
}
