package com.swj.shiwujie.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.swj.shiwujie.constants.AiConstants.IMAGE_MODEL;
import static com.swj.shiwujie.constants.AiConstants.TEXT_MODEL;

/**
 * 模型配置
 *
 * <p>文本与图像分两套客户端：
 * <ul>
 *   <li>{@link #qwenText()} —— 文本模型走 <b>OpenAI 兼容直连</b>（DashScope compatible-mode）。
 *       spring-ai-alibaba 的 DashScopeChatModel 调 qwen3.x 文本模型报 {@code url error}
 *       （旧客户端与新模型请求格式不兼容），故文本路径改用官方 {@link OpenAiChatModel}。
 *       baseUrl 取自 yml {@code spring.ai.dashscope.base-url}（compatible-mode/v1）。</li>
 *   <li>{@link #qwenImage()} —— 图像模型仍用 DashScope（多模态走原生端点正常）。</li>
 * </ul>
 * 两个 bean 均以 {@link org.springframework.ai.chat.model.ChatModel} 接口注入消费方
 * （TextApp/ToolChoiceApp/ImageApp/MyManus）。
 */
@Configuration
public class AiConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${spring.ai.dashscope.base-url}")
    private String baseUrl;


    /**
     * 文本模型 —— OpenAI 兼容直连（DashScope compatible-mode）
     */
    @Bean(name = "qwenText")
    public OpenAiChatModel qwenText() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(TEXT_MODEL)
                        .build())
                .build();
    }


    /**
     * 图像模型 —— 仍用 DashScope（多模态走原生端点正常）
     */
    @Bean(name = "qwenImage")
    public DashScopeChatModel qwenImage() {
        return DashScopeChatModel.builder()
                .dashScopeApi(DashScopeApi.builder().apiKey(apiKey).build())
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(IMAGE_MODEL)
                        .withMultiModel(true)
                        .build())
                .build();
    }


}
