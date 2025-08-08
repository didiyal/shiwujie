package com.swj.shiwujie.app;


import cn.hutool.core.util.URLUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.swj.shiwujie.advisor.MyLoggerAdvisor;
import com.swj.shiwujie.chatmemory.RedisChatMemory;
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
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

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

    /**
     * 系统提示词
     */
    private final String systemPrompt =
            "你是视无界无障碍辅助 APP 的 AI 问题处理助手。\n" +
            "当用户需要加入家庭时，严格按照以下步骤处理：\n" +
            "第一步：用户说\"我想要加入家庭\"时，回复\"好的，请提供家庭创建人的手机号，以便我们完成加入家庭的申请。\"\n" +
            "第二步：用户提供了手机号后，回复\"好的，请确认您是否同意加入这个家庭。如果您同意，请回复\"我确认加入家庭\"。\"\n" +
            "第三步：当用户说\"我确认加入家庭\"时，立即调用\"申请加入家庭\"工具，不要回复其他任何内容\n" +
            "重要规则：\n" +
            "1. 用户一旦提供了手机号，就不能再要求用户重复提供\n" +
            "2. 当用户说\"我确认加入家庭\"时，必须调用\"申请加入家庭\"工具\n" +
            "3. 只有调用工具才算完成任务，单纯的文本回复不算完成任务";



    private final ChatMemory chatMemory;


    // 自定义基于云知识库的向量存储
    @Resource
    private Advisor myRagCloudAdvisor;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    @Resource
    private ToolCallback[] allTools;


    /**
     * 构造
     *
     * @param chatModel 阿里云灵积大模型
     * @param redisChatMemory 自定义redis对话存储
     */
    public EasyProblemApp(DashScopeChatModel chatModel, RedisChatMemory redisChatMemory) {

        //基于内存存储的ChatMemory
        this.chatMemory = redisChatMemory;

        // 模型名
        String model = chatModel.getDefaultOptions().getModel();
        log.info(model);

        this.chatClient = ChatClient.builder(chatModel)
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
                .advisors(myRagCloudAdvisor)
                .user(u -> u.text("这个图片展示了什么信息")
                        .media(MimeTypeUtils.IMAGE_PNG, URLUtil.url(imageUrl)))
                .tools(toolCallbackProvider)
                .tools(allTools)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }


}
