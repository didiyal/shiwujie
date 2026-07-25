package com.swj.shiwujie.app;

import cn.hutool.core.util.StrUtil;
import org.springframework.ai.chat.model.ChatModel;
import com.swj.shiwujie.advisor.MyLoggerAdvisor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.swj.shiwujie.constants.AiConstants.CONVERSATION_ROUND;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;


/**
 * 文本APP
 */
@Component
@Slf4j
public class ToolChoiceApp {

    private final ChatClient client;
    @Value("classpath:/prompttemplate/toolChoice-template.txt")
    private Resource toolChoiceResource;
    private String systemPrompt;

    public ToolChoiceApp(@Qualifier("qwenText") ChatModel chatModel,
                         @Qualifier("toolChoiceAppChatMemory") ChatMemoryRepository chatMemoryRepository){
        log.info("文字模型" + chatModel.getDefaultOptions().getModel());
        // 创建消息存储器
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(CONVERSATION_ROUND)
                .build();
        client =  ChatClient.builder(chatModel)
                .defaultAdvisors(
                        // 日志
                        new MyLoggerAdvisor(),
                        // 添加消息存储器
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build()
                )
                .build();
    }



    /**
     * 与大模型文字交流(工具调用判断)
     *
     * @param text 输入的文本
     * @return 大模型回复
     */
    public String doChat(String text, Long blindId) {
        return client.prompt(systemPrompt)
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, blindId.toString()))
                .user(text)
                .call()
                .content();
    }




    // region 工具方法
    // 在资源注入完成后初始化系统提示词
    @PostConstruct
    private void initSystemPrompt() throws IOException {
        if(StrUtil.isEmptyIfStr(systemPrompt)){
            systemPrompt = StreamUtils.copyToString(toolChoiceResource.getInputStream(), StandardCharsets.UTF_8);
            log.debug("toolChoiceApp系统提示词初始化: " + systemPrompt);
        }
    }
// endregion

}