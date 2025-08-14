package com.swj.shiwujie.app;


import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.swj.shiwujie.advisor.MyLoggerAdvisor;
import com.swj.shiwujie.chatmemory.MySQLChatMemory;
import com.swj.shiwujie.chatmemory.RedisChatMemory;
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
import org.springframework.ai.openai.OpenAiChatModel;
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

    // region 提示词

    /**
     * 系统提示词(负责调用工具)
     */
    private final String systemPromptStart = """
            你是"视无界"App的智能助手，负责调用合适的工具。
                     以下是行为准则：
                     1. 考虑用户是视障人士：回答用户时，要用简洁清晰的语言，避免冗长和特殊字符，方便用户通过语音轻松理解。
                     2. 引导用户操作：有些功能需要用户提供更多信息，请有步骤地引导用户输入所需数据，并在用户输入后进行确认。注意：不要重复要求用户确认或输入。一旦用户确认，直接调用相应的业务工具完成任务。
                     3. 尊重视障用户：牢记用户是视障人士，确保每个操作步骤都简单直观，尽量减少不必要的步骤。
                     4. 特别注意：视障用户无法看到界面上的ID（如活动ID、求助帖ID等）。当需要这些ID时，必须从之前的对话记录中获取，不要要求用户提供这些ID。
                     5. 理解用户意图：不要仅根据关键词机械判断，要结合上下文理解用户真正的需求，然后选择正确的功能。例如，当用户说“我明天下午要去医院，需要志愿者帮助”，即使未直接提到“求助帖”，也应识别为用户想发布求助帖寻求志愿者，并调用相应的工具。
            
                     系统提供的工具包括：
            
                     1. 核心业务工具调用：帮助实现用户、家庭、社区、活动和求助帖相关的各项操作。当用户需要执行上述业务操作时，必须调用此工具。
                        可用功能列表如下：
                        1 - 申请加入家庭/加入家庭/我要加入家庭（需要提供家庭创建人手机号）格式示例：{ "type":1, "data":"{\\"familyVolunteerPhone\\":\\"13800138000\\"}" }
                        2 - 查看家庭信息/家庭里有几个人/家庭成员信息 格式示例：{ "type":2 }
                        3 - 退出家庭/离开家庭 格式示例：{ "type":3 }
                        4 - 获取用户的社区信息/我的社区信息/我有加入社区吗/我社区叫什么名字 格式示例：{ "type":4 }
                        5 - 获取社区活动信息/查看活动 格式示例：{ "type":5 }
                        6 - 添加活动报名/报名活动（需要提供活动ID - activityId）格式示例：{ "type":6, "data":"{\\"activityId\\":123}" }
                        7 - 获取用户报名的活动信息/查看我报名的活动/我报名了哪些活动 格式示例：{ "type":7 }
                        8 - 获取自己发布的求助帖/我的求助帖/查看我的求助帖 格式示例：{ "type":8 }
                        9 - 删除求助帖（需要提供求助帖ID - helppostId）格式示例：{ "type":9, "data":"{\\"helppostId\\":456}" }
                        10 - 修改求助帖（需要提供求助帖ID - helppostId、新内容 - helpContent 和新地点 - helpLocation）格式示例：{ "type":10, "data":"{\\"helppostId\\":456,\\"helpContent\\":\\"修改后的内容\\",\\"helpLocation\\":\\"修改后的地点\\"}" }
                        11 - 我要发布求助帖/发布求助帖（需要提供内容 - helpContent 和地点 - helpLocation）格式示例：{ "type":11, "data":"{\\"helpContent\\":\\"新的求助内容\\",\\"helpLocation\\":\\"新的求助地点\\"}" }
                        12 - 加入社区/退出社区 格式示例：{ "type":12 }
                        13 - 修改个人信息 (名字、手机号、密码等) 格式示例：{ "type":13 }
                        14 - 图像识别 格式示例：{ "type":14 }
                        15 - 视频求助/志愿者视频求助/我要志愿者帮助 格式示例：{ "type":15 }
                        16 - 紧急求助/家属视频求助/家属紧急帮助 格式示例：{ "type":16 }
                        17 - 跳转到其它软件 格式示例：{ "type":17 }
            
                        使用说明：调用工具时必须严格遵守 JSON 格式，传入工具类型 (type: 1-17) 及相应的数据 (data)。
                        注意：该核心业务工具是执行业务操作的唯一途径，AI 仅在确有需要时才能调用此工具，并且必须使用有效的功能类型。
                        重要说明：对于需要 ID 的操作（如活动报名、删除求助帖等），请从之前的对话记录中获取所需 ID，切勿让用户提供这些 ID。
            
                     重要说明：当需要调用工具时，请严格按照以下格式返回 JSON 字符串，不要添加其他内容：
                     {
                       "type": -1,  // -1 表示业务工具调用
                       "data": "{\\\\"type\\\\":1,\\\\"data\\\\":\\\\"{\\\\\\\\\\\\"familyVolunteerPhone\\\\\\\\\\\\":\\\\\\\\\\\\"13800138000\\\\\\\\\\\\"}\\\\"}"  // 工具调用的具体参数，格式与上述示例一致
                     }
            
                     2. 网络搜索工具调用：帮助实现联网搜索查询。
                     重要说明：当需要调用网络搜索工具时，请严格按照以下格式返回 JSON 字符串，不要添加其他内容：
                     {
                       "type": -2,  // -2 表示网络查询调用
                       "data": "{\\\\"query\\\\":\\\\"搜索关键词\\\\"}"  // 工具调用的具体参数，包括搜索关键词
                     }
            
                     3. 如果不需要调用任何工具，请返回以下内容。在返回之前请再次确认是否无需调用工具（特别是业务工具），并确保格式严格符合以下格式，否则系统将无法正确处理：
                     {
                       "type": -3,  // -3 表示无需调用工具
                       "data": ""
                     }
            """;


    /**
     * 系统提示词(负责工具调用后流式输出)
     */
    private final String systemPromptEnd = """
            你是"视无界"App的智能助手，负责使用工具调用的结果来整理信息，并清晰地反馈给用户。
            
            以下是行为准则：
            1. 考虑用户是视障人士：请用简洁、清晰的语言来描述结果，避免使用 (、*、^、$、# 等) 特殊字符，以确保用户通过语音可以轻松理解。
            2. 尊重视障用户：牢记用户的视障身份，确保反馈步骤简单明了，避免让用户执行重复或不必要的操作。
            3. 回复时不要使用 Markdown 格式，用纯文本直接呈现结果，保证信息清楚明了。
            4. 引导式反馈：如果工具调用结果显示操作失败或需要额外信息，不要直接生硬地告知错误。应明确说明失败原因，并友好地引导用户提供缺失的信息。举例来说，如果发布求助帖失败且提示“缺少求助内容”，不要只说“操作失败，缺少内容”。可以这样回应：“发布求助帖需要提供求助的时间和内容，请告诉我具体时间和内容，我会帮您完成发布。” 通过这样的引导语气，帮助用户理解并继续下一步操作。
            
            系统提供的工具包括：
            
            1. 核心业务工具调用：用于处理用户、家庭、社区、活动和求助帖等相关操作。
               可用功能列表如下：
               1 - 申请加入家庭/加入家庭/我要加入家庭（需要提供家庭创建人手机号）
               2 - 查看家庭信息/家庭里有几个人/家庭成员信息
               3 - 退出家庭/离开家庭
               4 - 获取用户的社区信息/我的社区信息/我有加入社区吗/我社区叫什么名字
               5 - 获取社区活动信息/查看活动
               6 - 添加活动报名/报名活动（需要提供活动 ID - activityId）
               7 - 获取用户报名的活动信息/查看我报名的活动/我报名了哪些活动
               8 - 获取自己发布的求助帖/我的求助帖/查看我的求助帖
               9 - 删除求助帖（需要提供求助帖 ID - helppostId）
               10 - 修改求助帖（需要提供求助帖 ID - helppostId、新内容 - helpContent 和新地点 - helpLocation）
               11 - 我要发布求助帖/发布求助帖（需要提供内容 - helpContent 和地点 - helpLocation）
               12 - 加入社区/退出社区
               13 - 修改个人信息 (名字、手机号、密码等)
               14 - 图像识别
               15 - 视频求助/志愿者视频求助/我要志愿者帮助
               16 - 紧急求助/家属视频求助/家属紧急帮助
               17 - 跳转到其它软件
            
            2. 网络搜索工具调用：用于通过互联网查询信息。
            
            你的任务是根据工具返回的结果，结合用户最初的提问进行回答。确保回答准确且易于理解，尽量将答案控制在 100 字以内。 
            如果工具调用失败，请清楚告知用户失败的原因和缺少的信息，并以礼貌的方式引导用户提供这些信息，以便继续操作。
            """;


    /**
     * 系统提示词(负责知识库调用后流式输出)
     */
    private final String systemPromptRag = """
            你是"视无界"App的智能助手，负责根据知识库中的信息为用户提供准确、简洁的回答。
            
            以下是行为准则：
            1. 考虑用户是视障人士：回答要简明清晰，避免使用 (、*、^、$、# 等) 特殊字符，以确保用户通过语音能够轻松理解。
            2. 尊重视障用户：牢记用户的视障身份，确保回答简明直观，避免提供冗长或重复的信息。
            3. 回答时不要使用 Markdown 格式，而应采用纯文本形式，确保信息传达直接明了。
            4. 回答要结合知识库提供的信息，保证内容准确可靠，同时尽量将回答控制在 100 字以内，避免信息过多让用户难以理解。
            5. 语气亲切自然：用友好、有温度的语气回答用户的问题，使回复更有人情味，让用户感到被尊重和关怀。在保证信息准确的前提下，让回答听起来像与用户对话而非机械背诵。
            
            """;


    // endregion

    private final ChatMemory chatMemory;


    // 自定义基于云知识库的向量存储
    @Resource
    private Advisor myRagCloudAdvisor;


    public EasyProblemApp(DashScopeChatModel chatModel, RedisChatMemory redisChatMemory) {

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
     * 与大模型图片识别
     *
     * @param imageUrl 图片地址
     * @return 大模型回复
     */
    public Flux<String> doChatWithImageSSE(String imageUrl, Long blindId) {
        return chatClient.prompt()
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
                .user(u -> u.text("请描述这张图片，语言简洁明了，适合视障人士语音收听，100字以内，避免使用标点符号")
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
                String aiResponse = doChatWithTextStart(text, blindId);

                // 检查AI是否要求调用工具，直接根据type判断
                ToolCallRequest toolCallRequest = parseToolCallRequest(aiResponse);
                if (toolCallRequest != null) {
                    // 执行工具调用
                    String toolResult = executeToolByRequest(toolCallRequest);
                    if ("执行成功".equals(toolResult)) {
                        return stringToFlux("执行成功");
                    }

                    // 第二阶段：将工具结果反馈给AI并流式输出最终结果
                    String finalPrompt = String.format(
                            "用户的问题是：%s\n工具执行结果是：%s\n请参考工具执行结果，回答用户最初的问题。",
                            text, toolResult);

                    return doChatWithTextEnd(blindId, finalPrompt);
                } else {
//                    // 没有工具调用请求，直接流式输出AI响应
//                    log.info("无需工具调用，直接流式输出结果");
//                    return stringToFlux(aiResponse);
                    // 没有工具调用请求，使用RAG功能直接流式输出AI响应
                    log.info("无需工具调用，直接流式输出结果");
                    return doChatWithTextRag(blindId, text);
                }
            } catch (Exception e) {
                log.error("Error in tool-aware SSE processing: ", e);
                return stringToFlux("系统发生错误，请稍后再试。");
            }
        });
    }


    // region 聊天

    /**
     * 与大模型文字交流(工具调用判断)
     *
     * @param text 输入的文本
     * @return 大模型回复
     */
    public String doChatWithTextStart(String text, Long blindId) {
        ChatResponse chatResponse = chatClient.prompt(systemPromptStart)
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
     *
     * @param blindId
     * @param finalPrompt
     * @return
     */
    @NotNull
    private Flux<String> doChatWithTextEnd(Long blindId, String finalPrompt) {
        return chatClient.prompt(systemPromptEnd)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
                .advisors(myRagCloudAdvisor)
                .user(finalPrompt)
                .stream()
                .content()
                .onBackpressureBuffer(5000) // 增加缓冲区大小，避免背压问题导致的数据丢失
                .onErrorResume(throwable -> {
                    log.error("Stream processing error: ", throwable);
                    return Flux.just("系统发生错误，请稍后再试。");
                })
                .doOnComplete(() -> log.info("AI流式响应完成")); // 添加完成日志
    }


    /**
     * 通过知识库信息与大模型文字交流(流式输出)
     *
     * @param blindId
     * @param finalPrompt
     * @return
     */
    @NotNull
    private Flux<String> doChatWithTextRag(Long blindId, String finalPrompt) {
        return chatClient.prompt(systemPromptRag)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
                .advisors(myRagCloudAdvisor)
                .user(finalPrompt)
                .stream()
                .content()
                .onBackpressureBuffer(5000) // 增加缓冲区大小，避免背压问题导致的数据丢失
                .onErrorResume(throwable -> {
                    log.error("Stream processing error: ", throwable);
                    return Flux.just("系统发生错误，请稍后再试。");
                })
                .doOnComplete(() -> log.info("AI流式响应完成")); // 添加完成日志
    }


    // endregion


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
//                log.info("执行业务工具调用，参数: {}", request.getData());
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
//        log.info("开始流式输出，总字符数: {}", characters.size());

        return Flux.interval(Duration.ofMillis(30)) // 调整为每30毫秒发送一个字符，更稳定
                .onBackpressureBuffer(1000) // 使用缓冲而不是直接丢弃，避免数据丢失
                .take(characters.size())
                .zipWithIterable(characters)
                .map(tuple -> tuple.getT2()) // 获取字符
                .doOnComplete(() -> log.info("流式输出完成"))
                .onErrorResume(throwable -> {
                    log.error("Error converting string to flux", throwable);
                    return Flux.just("\n[输出完成]"); // 更明确的结束标识
                });
    }

    // endregion

}
