package com.swj.shiwujie.app;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.swj.shiwujie.chatmemory.RedisChatMemory;
import com.swj.shiwujie.chatmemory.WorkChooseAppRedisChatMemory;
import com.swj.shiwujie.constant.AiConstants;
import com.swj.shiwujie.tools.app.WorkChooseTool;
import com.swj.shiwujie.tools.mytools.WebSearchTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;


/**
 * 工具选择app
 */
@Slf4j
@Component
public class ToolChooseApp {
    

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
                     4. 特别注意：视障用户无法看到界面上的ID（如活动ID、求助帖ID等）。当需要这些ID时，必须从之前的对话记录中获取，不要要求用户提供这些ID。
                     5. 理解用户意图：不要仅根据关键词机械判断，要结合上下文理解用户真正的需求，然后选择正确的功能。例如，当用户说“我明天下午要去医院，需要志愿者帮助”，即使未直接提到“求助帖”，也应识别为用户想发布求助帖寻求志愿者，并调用相应的工具。
                     6. 准确判断是否需要工具调用：对于健康咨询、生活常识等通用问题（如“我有点咳嗽”、“头有一点疼怎么办”等），以及用户对已进行图像识别结果的追问（如“图片里有什么”、“能再描述一下这张图吗”等），返回{ "type": -3, "data": ""}
。

            系统工具：
            1. 核心业务工具(1-18)：处理用户、家庭、社区、活动、求助帖相关操作。
               1 - 申请加入家庭（提供创建人手机号）示例：{"type":1,"data":"{\"familyVolunteerPhone\":\"13800138000\"}"}
               2 - 查看家庭信息 示例：{"type":2}
               3 - 退出家庭 示例：{"type":3}
               4 - 获取社区信息 示例：{"type":4}
               5 - 获取活动信息 示例：{"type":5}
               6 - 报名活动（需activityId）示例：{"type":6,"data":"{\"activityId\":123}"}
               7 - 查看已报名活动 示例：{"type":7}
               8 - 查看我的求助帖 示例：{"type":8}
               9 - 删除求助帖（需helppostId）示例：{"type":9,"data":"{\"helppostId\":456}"}
               10 - 修改求助帖（需helppostId等）示例：{"type":10,"data":"{\"helppostId\":456,\"helpContent\":\"内容\",\"helpLocation\":\"地点\"}"}
               11 - 发布求助帖（需helpContent等）示例：{"type":11,"data":"{\"helpContent\":\"内容\",\"helpLocation\":\"地点\"}"}
               12 - 加入/退出社区 示例：{"type":12}
               13 - 修改个人信息 示例：{"type":13}
               14 - 图像识别 示例：{"type":14}
               15 - 视频求助 示例：{"type":15}
               16 - 紧急求助 示例：{"type":16}
               17 - 跳转软件（需appName）示例：{"type":17,"data":"{\"appName\":\"软件名\"}"}
               18 - 导航（需destination）示例：{"type":18,"data":"{\"destination\":\"目的地\"}"}
               
               使用说明：严格遵守JSON格式，仅在确需时调用，ID从对话获取。

            返回格式要求(你只能返回以下格式内容,你不直接面向用户,你的返回结果将由其它助手处理返回)：
            1. 业务工具调用：{"type":-1,"data":"{\\\"type\\\":1,\\\"data\\\":\\\"{\\\\\\\"familyVolunteerPhone\\\\\\\":\\\\\\\"13800138000\\\\\\\"}\\\"}"}
            2. 网络搜索：{"type":-2,"data":"{\"query\":\"关键词\"}"}
            3. 无需工具/图片追问：{"type":-3,"data":""}
            4. 需要询问：{"type":-4,"data":"询问内容"}
                     
            """;
    
    // endregion

    private final ChatMemory chatMemory;

    

    public ToolChooseApp(DashScopeChatModel chatModel, WorkChooseAppRedisChatMemory redisChatMemory) {

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
