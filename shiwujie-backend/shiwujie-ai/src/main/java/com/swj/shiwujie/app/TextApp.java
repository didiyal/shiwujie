package com.swj.shiwujie.app;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.swj.shiwujie.advisor.MyLoggerAdvisor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static com.swj.shiwujie.constants.AiConstants.CONVERSATION_ROUND;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;


/**
 * 文本APP
 */
@Component
@Slf4j
public class TextApp {

    private final ChatClient client;
    @Value("classpath:/prompttemplate/text-template.txt")
    private Resource textResource;
    private String systemPrompt;


    public TextApp(@Qualifier("qwenText") DashScopeChatModel chatModel,
                   @Qualifier("textAppChatMemory") ChatMemoryRepository chatMemoryRepository,
                   @Qualifier("myRetrievalAugmentationAdvisor") RetrievalAugmentationAdvisor myRagAdvisor){
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
                        // 添加rag
//                        myRagAdvisor
                )
                .build();
    }

    /**
     * 与大模型文字交流(工具调用判断)
     *
     * @param text 输入的文本
     * @return 大模型回复
     */
    public Flux<String> doChat(String text, Long blindId) {
        return client.prompt(systemPrompt)
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, blindId.toString()))
                .user(text)
                .stream()
                .content()
                .doOnNext(System.out::print)
                .doOnComplete(System.out::println);
    }

    // region 工具方法


    // 在资源注入完成后初始化系统提示词
    @PostConstruct
    private void initSystemPrompt() {
        if(StrUtil.isEmptyIfStr(systemPrompt)){
            PromptTemplate promptTemplate = new PromptTemplate(textResource);
            systemPrompt = promptTemplate.create().getContents();
            log.debug("textApp系统提示词初始化: " + systemPrompt);
        }
    }

    // endregion

}
