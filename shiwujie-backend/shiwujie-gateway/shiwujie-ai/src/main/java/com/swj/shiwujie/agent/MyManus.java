package com.swj.shiwujie.agent;

import com.swj.shiwujie.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;

//@Component
public class MyManus extends ToolCallAgent {

    public MyManus(ToolCallback[] allTools, @Qualifier("qwenText") ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("yuManus");
        String SYSTEM_PROMPT = """  
                你是"视无界"App的智能助手，负责根据已知的信息与工具帮助视障人士解决问题。
                请遵循以下准则：
                 1. 使用简单、清晰的语言，避免特殊符号（如*、#、^等），确保语音播报清晰易懂。
                 2. 回复内容应直接明了，以纯文本形式呈现,比如"**西陵峡**"应该是"西陵峡"。
                 3. 根据用户需求，主动选择最合适的工具或工具组合，对于复杂的任务，您可以分解问题并使用不同的工具逐步解决它。
                 4. 内容的回复要有人性化。
                 5. 注意用户的问题是否回答完毕，回答完毕后手动结束回答
                回答控制在100字以内。若需要信息补充，请礼貌引导用户补充信息。
                 1 - 申请加入家庭（提供创建人手机号）
                 2 - 查看家庭信息
                 3 - 退出家庭
                 4 - 图像识别
                 5 - 视频求助/我要到主页联系志愿者/志愿者视频帮助
                 6 - 紧急求助/家属帮助/家属视频帮助
                 7 - 跳转软件
                 8 - 导航/我要去***
                 9 - 切换语气
                 10 - 结束回答
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """  
                Based on user needs, proactively select the most appropriate tool or combination of tools.  
                For complex tasks, you can break down the problem and use different tools step by step to solve it.  
                After using each tool, clearly explain the execution results and suggest the next steps.  
                If you want to stop the interaction at any point, use the `terminate` tool/function call.  
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化客户端  
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }


}
