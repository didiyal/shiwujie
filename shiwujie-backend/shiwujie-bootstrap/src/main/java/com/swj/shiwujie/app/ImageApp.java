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
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

import static com.swj.shiwujie.constants.AiConstants.IMAGE_CONVERSATION_ROUND;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 图像APP
 */
@Component
@Slf4j
public class ImageApp {

    private final ChatClient client;
    @Value("classpath:/prompttemplate/image-template.txt")
    private Resource imageResource;
    private String systemPrompt;

    public ImageApp(@Qualifier("qwenImage") ChatModel chatModel,
                    @Qualifier("imageAppChatMemory") ChatMemoryRepository chatMemoryRepository) {
        log.info("图像模型" + chatModel.getDefaultOptions().getModel());
        // 创建消息存储器
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(IMAGE_CONVERSATION_ROUND)
                .build();
        client =  ChatClient.builder(chatModel)
                .defaultAdvisors(
                        // 日志
                        new MyLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build()
                )
                .build();
    }

    // 在资源注入完成后初始化系统提示词
    @PostConstruct
    private void initSystemPrompt() {
        if(StrUtil.isEmptyIfStr(systemPrompt)){
            PromptTemplate promptTemplate = new PromptTemplate(imageResource);
            systemPrompt = promptTemplate.create(Map.of(
                    "identity", "针对视障人士的图片识别助手")).getContents();
            log.debug("系统提示词初始化: " + systemPrompt);
        }
    }


    /**
     * 处理图片
     * @param imageUrl 图片地址
     * @return 大模型回复
     */
    public Flux<String> doImage(String imageUrl, Long blindId) {
        return client.prompt(systemPrompt)
                .user(u -> u.text("请描述这张图片，语言简洁明了，适合视障人士语音收听，100字以内")
                        .media(MimeTypeUtils.IMAGE_PNG, new FileSystemResource(imageUrl)))
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, blindId.toString()))
                .stream()
                .content()
                .doOnNext(System.out::print)
                .onErrorResume(throwable -> {
                    log.error("处理图片识别流时发生错误", throwable);
                    return Flux.just("处理图片时发生错误，请稍后重试");
                });
    }


    /**
     * 图片追问
     */
    public String doChatCall(String message,Long blindId) {
        return client.prompt(systemPrompt)
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, blindId.toString()))
                .call()
                .content();
    }




}