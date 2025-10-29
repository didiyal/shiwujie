package com.swj.shiwujie.constant;


import java.util.HashMap;
import java.util.Map;

/**
 * Ai常量
 */
public interface AiConstants {


    /**
     * 对话轮数
     */
    int CONVERSATION_ROUND = 3;



    /**
     * 图片对话轮数
     */
    int IMAGE_CONVERSATION_ROUND = 1;


    /**
     * 对话语气
     */
    Map<Long,String> clientTone = new HashMap<>();

}
