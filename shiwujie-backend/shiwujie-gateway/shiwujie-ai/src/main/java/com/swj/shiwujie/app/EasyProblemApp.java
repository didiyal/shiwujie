package com.swj.shiwujie.app;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.swj.shiwujie.advisor.MyLoggerAdvisor;
import com.swj.shiwujie.chatmemory.MySQLChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.io.File;
import java.net.URL;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;


/**
 * 任务处理app(简单任务)
 */
@Slf4j
@Component
public class EasyProblemApp {


    /**
     * 初始化的AI client
     */
    private final ChatClient chatClient;

    // region system prompt

    /**
     * 系统提示词
     */
    private final String systemPrompt = """
            你是“视无界”App的智能助手，负责帮助用户解决问题并在需要时调用合适的工具。
            以下是行为准则：
            1. 考虑用户是视障人士：返回的语音结果要简洁、清晰，避免冗长，以便用户能轻松理解。
            2. 引导用户操作：有些功能需要多次询问，特别是需要用户输入数据时。请引导用户输入，并在其输入后再次确认。注意： 不要重复要求用户确认或输入。当用户确认后，直接调用相应的业务工具完成任务。
            3. 尊重视障用户身份：始终牢记用户是视障人士，确保每一步操作都简单直观，避免重复步骤。

            系统提供的工具包括：

            1. 业务调用工具：帮助实现所有与家庭、社区、求助帖等相关的操作。
            业务调用工具的type介绍: 1 - 申请加入家庭(加入家庭) 2 - 查看家庭信息(家庭里有几个人/家庭成员信息) 3 - 退出家庭(离开家庭) 4 - 获取用户的社区信息(我的社区信息) 5 - 获取社区活动信息(查看活动) 6 - 添加活动报名(报名活动) 7 - 获取用户报名的活动信息(查看我报名的活动/我报名了哪些活动) 8 - 获取自己发布的求助帖(我的求助帖) 9 - 删除求助帖 10 - 修改求助帖 11 - 添加求助帖(发布求助帖) 12 - 加入社区/修改个人信息(名字,手机号,密码等)/退出社区/跳转到其它软件/图像识别
            2. 网络搜索工具：用于联网查询信息。
            3. 高德地图相关工具：用于提供地图导航和相关服务。
            """;

    // endregion

    private final ChatMemory chatMemory;


    // 自定义基于云知识库的向量存储
    @Resource
    private Advisor myRagCloudAdvisor;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    @Resource
    private ToolCallback[] allTools;

    public EasyProblemApp(DashScopeChatModel chatModel, MySQLChatMemory mySQLChatMemory) {

        //基于内存存储的ChatMemory
        this.chatMemory = mySQLChatMemory;

        // 模型名
        String model = chatModel.getDefaultOptions().getModel();
        log.info(model);

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        // 自定义日志校验
//                        new MyLoggerAdvisor(),
                        // 自定义消息记录(基于redis)
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }


    // region 非流式调用

    /**
     * 与大模型文字交流
     *
     * @param text 输入的文本
     * @return 大模型回复
     */
    public String doChatWithText(String text, Long blindId) {
        ChatResponse chatResponse = chatClient.prompt()
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
                .advisors(myRagCloudAdvisor)
                .user(text)
                .tools(toolCallbackProvider)
                .tools(allTools)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }


    /**
     * 与大模型图片识别
     *
     * @param imageUrl 图片地址
     * @return 大模型回复
     */
    public String doChatWithImage(String imageUrl, Long blindId) {
        ChatResponse chatResponse = chatClient.prompt()
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
                .user(u -> u.text("这个图片展示了什么信息")
                        .media(MimeTypeUtils.IMAGE_PNG, new FileSystemResource(imageUrl)))
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();

    }


    // endregion


    // region 流式调用

    /**
     * 与大模型文字交流
     *
     * @param text 输入的文本
     * @return 大模型回复
     */
    public Flux<String> doChatWithTextSSE(String text, Long blindId) {
        return chatClient.prompt()
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
//                .advisors(myRagCloudAdvisor)
                .user(text)
                .tools(toolCallbackProvider)
                .tools(allTools)
                .stream()
                .content()
                .onErrorResume(throwable -> {
                    // 处理流式工具调用中的空参数错误
                    if (throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("toolInput cannot be null or empty")) {
                        log.warn("Caught toolInput null error in stream, returning fallback response");
                        return Flux.just("抱歉，系统正在处理您的请求，请稍后再试。");
                    }
                    log.error("Stream processing error: ", throwable);
                    return Flux.just("系统发生错误，请稍后再试。");
                });
    }


    /**
     * 与大模型图片识别
     *
     * @param imageUrl 图片地址
     * @return 大模型回复
     */
    public Flux<String> doChatWithImageSSE(String imageUrl, Long blindId) {
        return chatClient.prompt()
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
                .user(u -> u.text("这个图片展示了什么信息")
                        .media(MimeTypeUtils.IMAGE_PNG, new FileSystemResource(imageUrl)))
                .stream()
                .content();
    }


    // endregion

}
