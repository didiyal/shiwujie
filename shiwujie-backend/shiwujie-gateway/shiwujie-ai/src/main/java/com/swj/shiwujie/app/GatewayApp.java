package com.swj.shiwujie.app;


import cn.hutool.core.util.URLUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.swj.shiwujie.advisor.MyLoggerAdvisor;
import com.swj.shiwujie.chatmemory.RedisChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;


/**
 * 决策APP
 */
@Slf4j
@Component
public class GatewayApp {


    /**
     * 初始化的AI client
     */
    private final ChatClient chatClient;


    /**
     * 系统提示词
     */
    private final String systemPrompt = "你是一个AI智能助手,擅长解决问题";



    private final ChatMemory chatMemory;


    /**
     * 构造
     * @param dashscopeChatModel 阿里云灵积大模型
     * @param redisChatMemory 自定义redis对话存储
     */
    public GatewayApp(ChatModel dashscopeChatModel, RedisChatMemory redisChatMemory){

        // 引入基于redis自定义存储的bean
        this.chatMemory = redisChatMemory;

        // 基于内存存储的chatMemory
//        this.chatMemory = new InMemoryChatMemory();

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        // 自定义日志校验
                        new MyLoggerAdvisor(),
                        // 自定义消息记录(基于redis)
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }


    /**
     * 与大模型文字交流
     * @param text
     * @return
     */
    public String doChatWithText(String text,Long blindId){
        ChatResponse chatResponse = chatClient.prompt()
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .user(text)
                .call()
                .chatResponse();
        String res = chatResponse.getResult().getOutput().getText();
        return res;
    }



    /**
     * 与大模型文字交流
     * @param image
     * @return
     */
    public String doChatWithImage(String text,String image,Long blindId){
        ChatResponse chatResponse = chatClient.prompt()
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .user(u -> u.text("这个图片展示了什么信息")
                        .media(MimeTypeUtils.IMAGE_PNG, URLUtil.url(image)))
                .user(text)
                .call()
                .chatResponse();
        String res = chatResponse.getResult().getOutput().getText();
        return res;
    }





}
