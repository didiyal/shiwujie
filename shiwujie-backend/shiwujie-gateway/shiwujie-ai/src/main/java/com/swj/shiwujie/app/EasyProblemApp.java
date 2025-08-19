package com.swj.shiwujie.app;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.swj.shiwujie.chatmemory.RedisChatMemory;
import com.swj.shiwujie.common.ToolCallRequest;
import com.swj.shiwujie.constant.AiConstants;
import com.swj.shiwujie.tools.app.WorkChooseTool;
import com.swj.shiwujie.tools.mytools.WebSearchTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
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

    @Resource
    private ToolChooseApp toolChooseApp;

    // region 提示词

    /**
     * 系统提示词(负责工具调用后流式输出)
     */
    private final String systemPromptEnd = """
            你是"视无界"App的智能助手，负责根据工具调用结果生成简洁明了的回复，帮助视障用户轻松理解信息。
            
            请遵循以下准则：
            1. 使用简单、清晰的语言，避免特殊符号（如*、#、^等），确保语音播报清晰易懂。
            2. 回复内容应直接明了，使用txt格式，以纯文本形式呈现,比如"**西陵峡**"应该是"西陵峡"。
            3. 若工具执行成功，请总结关键信息并告知用户。
            4. 若工具执行失败或缺少必要信息，请友善地说明原因，并引导用户提供所需内容,若是系统错误,则告诉用户是网络问题。
               例如：若发布求助帖失败提示“缺少内容”，可回复：“发布求助帖需要您提供具体的内容和地点，能告诉我吗？”
            
            可调用的工具包括：
            1. 核心业务工具：处理家庭、社区、活动、求助帖等操作。
            2. 网络搜索工具：用于联网查询信息。 
            3. 无需工具调用：问题已可直接回答。
            4. 需要更多信息：需向用户询问详情。
            
            请根据工具返回结果和用户原始问题进行精准回答，控制在100字以内。若失败，请礼貌引导用户补充信息。
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
                // 取消图片的上下文存储
//                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
//                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, AiConstants.CONVERSATION_ROUND))
                .user(u -> u.text("请描述这张图片，语言简洁明了，适合视障人士语音收听，100字以内")
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
                String aiResponse = toolChooseApp.doChatWithTextStart(text, blindId);
                log.info("工具分析AI返回: {}", aiResponse);
                // 检查AI是否要求调用工具，直接根据type判断
                ToolCallRequest toolCallRequest = parseToolCallRequest(aiResponse);


                String content = null;
                if (toolCallRequest != null) {
                    switch (toolCallRequest.getType()) {
                        case -1:// 执行业务工具调用
                            log.info("执行业务工具调用{}", toolCallRequest);
                            content = workChooseTool.questionChoose(toolCallRequest.getData());
                            break;
                        case -2:// 网页搜索
                            log.info("开始执行web搜索,{}", toolCallRequest);
                            content = webSearchTool.searchWeb(toolCallRequest.getData());
                            log.info("web搜索结果: {}", content);
                            // 第二阶段：将工具结果反馈给AI并流式输出最终结果
                            String finalPrompt = String.format(
                                    "用户的问题是：%s\n工具执行结果是：%s\n若执行了工具,请参考工具执行结果，回答用户最初的问题。",
                                    text, content);

                            return doChatWithTextEnd(blindId, finalPrompt);
                        case -3:// 不执行工具调用
                            log.info("不执行工具调用,{}", toolCallRequest);
                            content = "程序认为无需执行工具调用";
                            break;
                        case -4:// 需要获取用户输入信息
                            log.info("需要获取用户输入信息,{}", toolCallRequest);
                            content = toolCallRequest.getData();
                            break;
                    }
                }


                // 第二阶段：将工具结果反馈给AI并流式输出最终结果
                String finalPrompt = String.format(
                        "用户的问题是：%s\n工具执行结果是：%s\n若执行了工具,请参考工具执行结果，回答用户最初的问题。",
                        text, content);

                return doChatWithTextEndRAG(blindId, finalPrompt);

            } catch (Exception e) {
                log.error("Error in tool-aware SSE processing: ", e);
                return stringToFlux("系统发生错误，请稍后再试。");
            }
        });
    }


    // region 聊天


    /**
     * 与大模型文字交流RAG(流式输出)
     *
     * @param blindId
     * @param finalPrompt
     * @return
     */
    @NotNull
    private Flux<String> doChatWithTextEndRAG(Long blindId, String finalPrompt) {
        return chatClient.prompt(systemPromptEnd)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, blindId.toString())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, AiConstants.CONVERSATION_ROUND))
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
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, AiConstants.CONVERSATION_ROUND))
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

                if ((ObjUtil.isNotNull(type)) && data != null) {
                    return new ToolCallRequest(type, data);
                }
            }
        } catch (Exception e) {
            log.warn("解析工具调用请求失败，响应内容: {}，错误: {}", aiResponse, e.getMessage());
        }
        return null;
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
