package com.swj.shiwujie.app;


import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.swj.shiwujie.advisor.MyLoggerAdvisor;
import com.swj.shiwujie.chatmemory.RedisChatMemory;
import com.swj.shiwujie.model.request.ai.GateWayImageRequest;
import com.swj.shiwujie.model.request.ai.GateWayTextRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;


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
    private  ChatClient chatClient;



    /**
     * 系统提示词
     */
    private final String systemPrompt =
            "你是视无界APP的问题评估助手（视无界是服务视障人士的安卓软件），核心任务是根据用户输入选择对应工具处理问题。" +
                    "可用工具：简单问题处理工具(easyWorkChoose)、复杂问题处理工具(complexWorkChoose)。" +
                    "处理规则：" +
                    "1. 用户输入仅存在以下两种组合之一：" +
                    "   - 组合一：text（文字问题） + blindId（用户唯一标识）" +
                    "   - 组合二：imageUrl（图片地址） + blindId（用户唯一标识）" +
                    "2. 若为组合二（含imageUrl），直接返回数字1；" +
                    "3. 若为组合一（含text），需先评估问题复杂程度：" +
                    "   - 简单问题（如基础操作咨询、功能查询等）返回数字1；" +
                    "   - 复杂问题（如要多次操作的）返回数字2；" ;





    public GatewayApp(OpenAiChatModel chatModel) {


        String model = chatModel.getDefaultOptions().getModel();
        log.info(model);

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        // 自定义日志校验
//                        new MyLoggerAdvisor()
                )
                .build();
    }


    /**
     * 问题分析(文字)
     *
     * @param gateWayTextRequest
     * @return
     */
    public String analysisText(GateWayTextRequest gateWayTextRequest) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(JSONUtil.toJsonStr(gateWayTextRequest))
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }



    /**
     * 问题分析(图片)
     *
     * @param gateWayImageRequest
     * @return
     */
    public String analysisImage(GateWayImageRequest gateWayImageRequest) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(JSONUtil.toJsonStr(gateWayImageRequest))
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }


}
