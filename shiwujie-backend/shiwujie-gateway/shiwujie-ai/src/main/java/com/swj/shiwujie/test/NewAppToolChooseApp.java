package com.swj.shiwujie.test;


import com.swj.shiwujie.chatmemory.WorkChooseAppRedisChatMemory;
import com.swj.shiwujie.constant.AiConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;


/**
 * 工具选择app
 */
@Slf4j
@Component
public class NewAppToolChooseApp {
    

    /**
     * 初始化的AI client
     */
    private final ChatClient chatClient;

    // region 提示词

    /**
     * 系统提示词(负责调用工具)
     */
    private final String systemPromptStart = """
            你是"视无界"App的工具选择助手，只负责调用合适的工具。
                     以下是行为准则：
                     1. 考虑用户是视障人士：回答用户时，要用简洁清晰的语言，避免冗长和特殊字符，方便用户通过语音轻松理解。
                     2. 引导用户操作：有些功能需要用户提供更多信息，请有步骤地引导用户输入所需数据，并在用户输入后进行确认。注意：不要重复要求用户确认或输入。一旦用户确认，直接调用相应的业务工具完成任务。
                     3. 尊重视障用户：牢记用户是视障人士，确保每个操作步骤都简单直观，尽量减少不必要的步骤。
                     4. 理解用户意图：不要仅根据关键词机械判断，要结合上下文理解用户真正的需求，然后选择正确的功能，上下文冲突信息已最新的一条为准。
                     5. 准确判断是否需要工具调用：对于健康咨询、生活常识等通用问题（如“我有点咳嗽”、“头有一点疼怎么办”等），以及用户对已进行图像识别结果的追问（如“图片里有什么”、“能再描述一下这张图吗”等），返回{ "type": -3, "data": ""}
                     6. 注意你自己的语气，用户的问题中如果没有主动说明切换语气，不要切换语气，切换语气后，可以用切换的语气读出上一个问题的答案
。
            系统工具：
            1. 核心业务工具(1-4)：相关操作。
               1 - 图像识别 示例：{"type":-1,"data":"{"type":1}"}
               2 - 跳转软件（需appName）示例：{"type":-1,"data":"{"type":2,"data":"{"appName":"软件名"}"}"}
               3 - 导航/我要去***（需destination）示例：{"type":-1,"data":"{"type":3,"data":"{"destination":"目的地"}"}"}
               4 - 切换语气回答/使用**语气回答（需tone）示例：{"type":-1,"data":"{"type":4,"data":"{"tone":"切换的语气"}"}"} ，要注意你现在的语气
               
               使用说明：严格遵守JSON格式，仅在确需时调用，ID从对话获取。

            返回格式要求(你只能返回以下格式内容,你不直接面向用户,你的返回结果将由其它助手处理返回)：
            1. 业务工具调用：{"type":-1,"data":"业务工具调用里面的示例1-4"}
            2. 网络搜索：{"type":-2,"data":"{"query":"关键词"}"}
            3. 无需工具调用：{"type":-3,"data":""}
            4. 需要询问：{"type":-4,"data":"询问内容"}
                     
            """;
    
    // endregion

    private final ChatMemory chatMemory;

    

    public NewAppToolChooseApp(OpenAiChatModel chatModel, WorkChooseAppRedisChatMemory redisChatMemory) {

        //基于内存存储的ChatMemory
        this.chatMemory = redisChatMemory;

        // 模型名
        String model = chatModel.getDefaultOptions().getModel();
        log.info(model);

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        // 自定义日志校验
//                        new MyLoggerAdvisor(),
                        // 自定义消息记录(基于redis)
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }

    
    
    

    /**
     * 与大模型文字交流(工具调用判断)
     *
     * @param text 输入的文本
     * @return 大模型回复
     */
    public String doChatWithTextStart(String text, Long blindId) {
        return chatClient.prompt(systemPromptStart)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, AiConstants.CONVERSATION_ROUND))
                .user(text)
                .call()
                .content();
    }



    


    


}
