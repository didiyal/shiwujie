package com.swj.shiwujie.app;


import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.swj.shiwujie.chatmemory.MySQLChatMemory;
import com.swj.shiwujie.common.AiToolRequest;
import com.swj.shiwujie.common.ToolCallRequest;
import com.swj.shiwujie.tools.app.WorkChooseTool;
import com.swj.shiwujie.tools.mytools.WebSearchTool;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * 任务处理app(简单任务)
 */
@Slf4j
@Component
public class EasyProblemApp {

    @Resource
    private WorkChooseTool workChooseTool;

    @Resource
    private WebSearchTool webSearchTool;

    /**
     * 初始化的AI client
     */
    private final ChatClient chatClient;

    // region system prompt

    /**
     * 系统提示词
     */
    private final String systemPrompt1 = """
            你是"视无界"App的智能助手，负责帮助用户解决问题并在需要时调用合适的工具。
                   以下是行为准则：
                   1. 考虑用户是视障人士：返回的语音结果要简洁、清晰，避免冗长，以便用户能轻松理解。
                   2. 引导用户操作：有些功能需要多次询问，特别是需要用户输入数据时。请引导用户输入，并在其输入后再次确认。注意： 不要重复要求用户确认或输入。当用户确认后，直接调用相应的业务工具完成任务。
                   3. 尊重视障用户身份：始终牢记用户是视障人士，确保每一步操作都简单直观，避免重复步骤。
            
                   系统提供的工具包括：
            
                   1. 核心业务工具调用：帮助实现所有与用户、家庭和社区相关的操作。当用户需要执行具体业务操作时，必须调用此工具。
                   可用功能列表如下：
                    1 - 申请加入家庭(加入家庭)（需要提供家庭创建人手机号）格式示例：{ "type":1, "data":"{\\"familyVolunteerPhone\\":\\"13800138000\\"}" }
                    2 - 查看家庭信息(家庭里有几个人/家庭成员信息) 格式示例：{ "type":2 }
                    3 - 退出家庭(离开家庭) 格式示例：{ "type":3 }
                    4 - 获取用户的社区信息(我的社区信息) 格式示例：{ "type":4 }
                    5 - 获取社区活动信息(查看活动) 格式示例：{ "type":5 }
                    6 - 添加活动报名(报名活动)（需要提供活动ID - activityId）格式示例：{ "type":6, "data":"{\\"activityId\\":123}" }
                    7 - 获取用户报名的活动信息(查看我报名的活动/我报名了哪些活动) 格式示例：{ "type":7 }
                    8 - 获取自己发布的求助帖(我的求助帖) 格式示例：{ "type":8 }
                    9 - 删除求助帖（需要提供求助帖ID - helppostId）格式示例：{ "type":9, "data":"{\\"helppostId\\":456}" }
                    10 - 修改求助帖（需要提供求助帖ID - helppostId、新内容 - helpContent 和新地点 - helpLocation）格式示例：{ "type":10, "data":"{\\"helppostId\\":456,\\"helpContent\\":\\"修改后的内容\\",\\"helpLocation\\":\\"修改后的地点\\"}" }
                    11 - 添加求助帖(发布求助帖)（需要提供内容 - helpContent 和地点 - helpLocation）格式示例：{ "type":11, "data":"{\\"helpContent\\":\\"新的求助内容\\",\\"helpLocation\\":\\"新的求助地点\\"}" }
                    12 - 加入社区/修改个人信息(名字,手机号,密码等)/退出社区/跳转到其它软件/图像识别 格式示例：{ "type":12 }
            
                   使用说明：调用时需要严格遵守JSON格式，传入工具类型（type：1-12）及数据（data）。
                   注意：此工具是执行业务操作的唯一途径，AI只能在需要时调用此工具，并且仅在工具类型（type）有效时执行。
            
                   重要说明：当你需要调用工具时，请严格按照以下格式返回JSON字符串，不要添加其他内容：
                   {
                     "type": -1,  // -1表示业务工具调用
                     "data": "{\\"type\\":1,\\"data\\":\\"{\\\\\\"familyVolunteerPhone\\\\\\":\\\\\\"13800138000\\\\\\"}\\"}"  // 工具调用的具体参数，格式与上面的示例一致
                   }
                   2. 网络搜索工具调用: 帮助实现联网搜索查询
                   重要说明：当你需要调用工具时，请严格按照以下格式返回JSON字符串，不要添加其他内容：
                   {
                     "type": -2,  // -2标识网络查询调用
                     "data": "{\\"query\\":\\"搜索关键词\\"}"  // 工具调用的具体参数，包含搜索关键词
                   }
            
                   如果不需要调用工具，请直接回答用户问题。请确保严格按照上述格式返回工具调用请求，否则系统将无法正确处理。
            """;
    /**
     * 系统提示词
     */
    private final String systemPrompt2 = """
            你是"视无界"App的智能助手，负责使用工具调用的结果处理信息并清晰地反馈给用户。
            
            以下是行为准则：
            1. 考虑用户是视障人士：返回的语音结果要简洁、清晰，避免冗长，以便用户能轻松理解。
            2. 尊重视障用户身份：始终牢记用户是视障人士，确保每一步操作都简单直观，避免重复步骤。
            
            系统提供的工具包括：
            
            1. 核心业务工具调用：帮助实现所有与用户、家庭和社区相关的操作。当用户需要执行具体业务操作时，必须调用此工具。
               可用功能列表如下：
                1 - 申请加入家庭(加入家庭)（需要提供家庭创建人手机号）
                2 - 查看家庭信息(家庭里有几个人/家庭成员信息)
                3 - 退出家庭(离开家庭)
                4 - 获取用户的社区信息(我的社区信息)
                5 - 获取社区活动信息(查看活动)
                6 - 添加活动报名(报名活动)
                7 - 获取用户报名的活动信息(查看我报名的活动/我报名了哪些活动)
                8 - 获取自己发布的求助帖(我的求助帖)
                9 - 删除求助帖
                10 - 修改求助帖
                11 - 添加求助帖(发布求助帖)
                12 - 加入社区/修改个人信息(名字,手机号,密码等)/退出社区/跳转到其它软件/图像识别
            
            你的任务是根据工具返回的结果，用简洁明了的语言回答用户最初的问题，确保信息准确且易于理解。
            """;

    // endregion

    private final ChatMemory chatMemory;


    // 自定义基于云知识库的向量存储
    @Resource
    private Advisor myRagCloudAdvisor;


    public EasyProblemApp(DashScopeChatModel chatModel, MySQLChatMemory mySQLChatMemory) {

        //基于内存存储的ChatMemory
        this.chatMemory = mySQLChatMemory;

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



    // region 聊天

    /**
     * 与大模型文字交流(工具调用判断)
     * @param text 输入的文本
     * @return 大模型回复
     */
    public String doChatWithTextStart(String text, Long blindId) {
        ChatResponse chatResponse = chatClient.prompt(systemPrompt1)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
//                .advisors(myRagCloudAdvisor)
                .user(text)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }


    /**
     * 与大模型文字交流(流式输出)
     * @param blindId
     * @param finalPrompt
     * @return
     */
    @NotNull
    private Flux<String> doChatWithTextEnd(Long blindId, String finalPrompt) {
        return chatClient.prompt(systemPrompt2)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
                .user(finalPrompt)
                .stream()
                .content()
                .onBackpressureDrop() // 处理背压问题
                .onErrorResume(throwable -> {
                    log.error("Stream processing error: ", throwable);
                    return Flux.just("系统发生错误，请稍后再试。");
                });
    }


    // endregion


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

    /**
     * 与大模型文字交流（流式）
     *
     * @param text    输入的文本
     * @param blindId 用户盲ID
     * @return 大模型流式回复
     */
    public Flux<String> doChatWithTextSSE(String text, Long blindId) {
        return Flux.defer(() -> {
            try {
                // 第一阶段：使用非流式调用获取AI响应
                log.info("开始第一阶段：获取AI响应");
                String aiResponse = doChatWithTextStart(text, blindId);
                log.info("AI响应: {}", aiResponse);

                // 检查AI是否要求调用工具，直接根据type判断
                ToolCallRequest toolCallRequest = parseToolCallRequest(aiResponse);
                if (toolCallRequest != null) {
                    log.info("检测到工具调用请求，type: {}, data: {}", toolCallRequest.getType(), toolCallRequest.getData());

                    // 执行工具调用
                    String toolResult = executeToolByRequest(toolCallRequest);
                    log.info("工具调用结果: {}", toolResult);

                    // 第二阶段：将工具结果反馈给AI并流式输出最终结果
                    String finalPrompt = String.format(
                            "用户的问题是：%s\n工具执行结果是：%s\n请根据工具执行结果，用简洁明了的语言回答用户最初的问题。",
                            text, toolResult);

                    log.info("开始第二阶段：流式输出最终结果");
                    return doChatWithTextEnd(blindId, finalPrompt);
                } else {
                    // 没有工具调用请求，直接流式输出AI响应
                    log.info("无需工具调用，直接流式输出结果");
                    return stringToFlux(aiResponse);
                }
            } catch (Exception e) {
                log.error("Error in tool-aware SSE processing: ", e);
                return stringToFlux("系统发生错误，请稍后再试。");
            }
        });
    }



    // region 工具

    /**
     * 解析工具调用请求
     */
    private ToolCallRequest parseToolCallRequest(String aiResponse) {
        try {
            // 检查响应是否为空
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                return null;
            }

            // 清理响应字符串，尝试提取JSON部分
            String cleanedResponse = aiResponse.trim();

            // 查找JSON的开始和结束位置
            int jsonStart = cleanedResponse.indexOf("{");
            int jsonEnd = cleanedResponse.lastIndexOf("}");

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                // 提取JSON部分
                cleanedResponse = cleanedResponse.substring(jsonStart, jsonEnd + 1);

                // 使用Hutool解析JSON
                cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(cleanedResponse);
                Integer type = jsonObject.getInt("type");
                String data = jsonObject.getStr("data");

                // 根据type判断是否为工具调用请求
                if ((type == -1 || type == -2) && data != null) {
                    return new ToolCallRequest(type, data);
                }
            }
        } catch (Exception e) {
            log.warn("解析工具调用请求失败，响应内容: {}，错误: {}", aiResponse, e.getMessage());
        }
        return null;
    }

    /**
     * 根据请求执行工具调用
     */
    private String executeToolByRequest(ToolCallRequest request) {
        try {
            if (request.getType() == -1) {
                // 业务工具调用
                log.info("执行业务工具调用，参数: {}", request.getData());
                return workChooseTool.questionChoose(request.getData());
            } else if (request.getType() == -2) {
                // 网络搜索工具调用
                log.info("执行网络搜索工具调用，参数: {}", request.getData());
                return webSearchTool.searchWeb(request.getData());
            } else {
                return "未知工具类型: " + request.getType();
            }
        } catch (Exception e) {
            log.error("执行工具调用失败: ", e);
            return "工具执行失败: " + e.getMessage();
        }
    }


    /**
     * 将字符串转换为流式输出
     */
    private Flux<String> stringToFlux(String text) {
        if (text == null || text.isEmpty()) {
            return Flux.just("");
        }

        List<String> characters = Arrays.asList(text.split(""));
        log.info("开始流式输出，总字符数: {}", characters.size());

        return Flux.interval(Duration.ofMillis(20)) // 每20毫秒发送一个字符
                .onBackpressureDrop() // 处理背压问题
                .take(characters.size())
                .zipWithIterable(characters)
                .map(tuple -> tuple.getT2()) // 获取字符
                .doOnComplete(() -> log.info("流式输出完成"))
                .onErrorResume(throwable -> {
                    log.error("Error converting string to flux", throwable);
                    return Flux.just("");
                });
    }

    // endregion

}
