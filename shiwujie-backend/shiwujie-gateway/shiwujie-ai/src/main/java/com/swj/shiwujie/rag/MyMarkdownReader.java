package com.swj.shiwujie.rag;

import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取markdown文档
 */
@Component
class MyMarkdownReader {

    private ResourcePatternResolver resourcePatternResolver;

    MyMarkdownReader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    List<Document> loadMarkdown() {
        List<Document> documents = new ArrayList<>();
        Resource[] resources = null;
        try {
            resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();

                // 处理读入
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);

                List<Document> list = reader.get();

                documents.addAll(list);

            }

            return documents;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"document 读取失败");
        }

    }

}