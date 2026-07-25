package com.swj.shiwujie.advisor;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.swj.shiwujie.constants.AiConstants.KNOWLEDGE_INDEX;


@Configuration
public class MyRagAdvisor {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;


    @Bean(name = "myRetrievalAugmentationAdvisor")
    public RetrievalAugmentationAdvisor myRetrievalAugmentationAdvisor() {
        // 创建知识库检索
        DashScopeDocumentRetriever dashScopeDocumentRetriever = new DashScopeDocumentRetriever(
                DashScopeApi.builder().apiKey(apiKey).build(),
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(KNOWLEDGE_INDEX)
                        .build());
        return RetrievalAugmentationAdvisor.builder()
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true) // 允许空上下文，避免检索不到内容时抛出异常
                        .build())
                .documentRetriever(dashScopeDocumentRetriever)
                .build();
    }



}
