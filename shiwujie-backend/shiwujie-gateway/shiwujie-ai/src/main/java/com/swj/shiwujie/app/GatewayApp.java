package com.swj.shiwujie.app;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;



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
                    "1. 用户输入：" +
                    "   - text（文字问题）" +
                    "2. 需先评估问题复杂程度：" +
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
     * @param text
     * @return
     */
    public String analysisText(String text ) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(text)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }





}
