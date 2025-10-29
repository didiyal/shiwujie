package com.swj.shiwujie.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class MyAppRagCloudAdvisorConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Bean
public Advisor myRagCloudAdvisor() {
    DashScopeApi dashScopeApi = new DashScopeApi(dashScopeApiKey);
    final String KNOWLEDGE_INDEX = "视无界";

    DashScopeDocumentRetriever dashScopeDocumentRetriever = new DashScopeDocumentRetriever(dashScopeApi,
            DashScopeDocumentRetrieverOptions.builder()
                    .withIndexName(KNOWLEDGE_INDEX)
                    .build());

    List<QueryTransformer> queryTransformers = new ArrayList<>();

    // 优化RAG检索结果的系统提示词
    String ragSystemPrompt = """
            你是"视无界"App的智能助手，专门为视障人士提供帮助。
            
            你的任务是根据检索到的信息，回复用户：
            1. 如果问题涉及以下业务操作，请不要添加其他内容：
               - 加入家庭、查看家庭信息、退出家庭
               - 查看社区信息、查看活动、报名活动
               - 发布求助帖、查看求助帖、修改求助帖、删除求助帖
               - 加入社区、修改个人信息、退出社区
               - 图像识别、跳转到其它软件
            2. 如果是其它问题，请根据检索到的信息，用简洁、清晰、口语化的语言回答：
               - 回答必须在100字以内，只说重点
               - 使用简单易懂的词汇，适合语音播报
               - 不要使用任何特殊符号或Markdown格式
               - 不要分点说明或使用编号
               - 不要添加与问题无关的内容
               - 直接回答问题，不要过多解释
               - 注意：此时的知识库内容仅供回答问题时参考，不能直接用于执行业务操作
            
            检索到的信息：
            {context}
            
            用户问题：
            {query}
            """;

    return RetrievalAugmentationAdvisor.builder()
                // 允许查询不到数据
                .queryAugmenter(
                        ContextualQueryAugmenter.builder()
                                .allowEmptyContext(true) // 允许空上下文，避免检索不到内容时抛出异常
                                .build()
                )
                .queryTransformers(queryTransformers)
                .documentRetriever(dashScopeDocumentRetriever)
                .build();
}
}