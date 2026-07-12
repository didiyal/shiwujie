package com.swj.shiwujie.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.swj.shiwujie.constants.AiConstants.IMAGE_MODEL;
import static com.swj.shiwujie.constants.AiConstants.TEXT_MODEL;

/**
 * 模型配置
 */
@Configuration
public class AiConfig {



    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;


    /**
     * 文本模型
     */
    @Bean(name = "qwenText")
    public DashScopeChatModel qwenText() {
        return DashScopeChatModel.builder()
                .dashScopeApi(DashScopeApi.builder().apiKey(apiKey).build())
                .defaultOptions(DashScopeChatOptions.builder().withModel(TEXT_MODEL).build())
                .build();
    }


    /**
     * 图像模型
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