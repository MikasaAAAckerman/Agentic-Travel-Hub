package com.travel.aiagent.common.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DocumentRetrieverConfig {

    @Value("${travel-document-retriever-name}")
    private String travelDocumentRetrieverName;

    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    @Bean
    public DashScopeApi dashscopeApi() {
        return DashScopeApi.builder().apiKey(dashscopeApiKey).build();
    }

    @Bean("travelDocumentRetriever")
    public DocumentRetriever travelDocumentRetriever(DashScopeApi dashscopeApi) {
        log.info("[Config] 初始化旅行文档检索器 | index={} ", travelDocumentRetrieverName);
        DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .indexName(travelDocumentRetrieverName)
                        .denseSimilarityTopK(3)
                        .sparseSimilarityTopK(3)
                        .rerankTopN(5)
                        .build());
        log.debug("[Config] 文档检索器配置完成");
        return retriever;
    }

}
