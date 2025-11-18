package com.swj.shiwujie.constants;


import java.util.HashMap;
import java.util.Map;

/**
 * Ai常量
 */
public interface AiConstants {


    /**
     * 对话个数
     */
    int CONVERSATION_ROUND = 10;



    /**
     * 图片对话个数
     */
    int IMAGE_CONVERSATION_ROUND = 6;



    // RAG知识库名
    String KNOWLEDGE_INDEX = "视无界";


    /**
     * 对话语气
     */
    Map<Long,String> clientTone = new HashMap<>();


    /**
     * Redis键前缀
     */
    String KEY_PREFIX = "chat:memory:";



    // 模型名称常量定义，一套系统多模型共存
    String TEXT_MODEL = "qwen3-max";
    String IMAGE_MODEL = "qwen3-vl-flash";
}
