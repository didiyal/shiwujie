package com.swj.shiwujie.app;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * 任务处理app(简单任务)
 */
@Slf4j
@Component
public class ImageApp {

    /**
     * 初始化的AI client
     */
    private final ChatClient chatClient;



    public ImageApp(DashScopeChatModel chatModel) {

        // 模型名
        String model = chatModel.getDefaultOptions().getModel();
        log.info("图片处理模型" + model);

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        // 自定义日志校验
//                        new MyLoggerAdvisor(),
                        // 自定义消息记录(基于redis)
//                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }

    /**
     * 与大模型图片识别
     *
     * @param imageUrl 图片地址
     * @return 大模型回复
     */
    public Flux<String> doChatWithImageSSE(String imageUrl, Long blindId) {
        return chatClient.prompt()
                .user(u -> u.text("请描述这张图片，语言简洁明了，适合视障人士语音收听，100字以内")
                        .media(MimeTypeUtils.IMAGE_PNG, new FileSystemResource(imageUrl)))
                .stream()
                .content();
    }


    /**
     * 与大模型图片识别
     *
     * @param imageUrl 图片地址
     * @return 大模型回复
     */
    public String doChatWithImage(String imageUrl, Long blindId) {
        return chatClient.prompt()
                .user(u -> u.text("请描述这张图片，语言简洁明了，适合视障人士语音收听，100字以内")
                        .media(MimeTypeUtils.IMAGE_JPEG, new FileSystemResource(imageUrl)))
                .call()
                .content();
    }



}
