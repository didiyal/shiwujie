package com.swj.shiwujie.rag;


import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


/**
 * ETL向量数据库配置
 */
@Configuration
public class MyVectorStoreConfig {

    @Resource
    private MyMarkdownReader myMarkdownReader;


    @Bean
    VectorStore MyVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        // 基于内存的向量数据库
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载文档
        List<Document> documents = myMarkdownReader.loadMarkdown();
        simpleVectorStore.doAdd(documents);
        return simpleVectorStore;
    }


}
