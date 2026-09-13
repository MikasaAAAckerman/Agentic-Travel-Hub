package com.travel.aiagent.common.core.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 百炼云端召回通道
 * 使用阿里云百炼的 DashScope 进行文档检索（包含 Dense + Sparse + Rerank）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tool-rag.channel", havingValue = "bailian", matchIfMissing = true)
public class BailianToolRagChannel implements ToolRagChannel {

    private final DocumentRetriever documentRetriever;

    public BailianToolRagChannel(DashScopeApi dashScopeApi,
                                  @org.springframework.beans.factory.annotation.Value("${travel-document-retriever-name}") String indexName) {
        log.info("[ToolRag] 初始化百炼召回通道 | index={}", indexName);
        this.documentRetriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .indexName(indexName)
                        .denseSimilarityTopK(3)
                        .sparseSimilarityTopK(3)
                        .rerankTopN(5)
                        .build());
    }

    @Override
    public List<Document> retrieve(Query query) {
        log.info("[ToolRag] 百炼通道召回 | query={}", query.text());
        List<Document> documents = documentRetriever.retrieve(query);
        log.info("[ToolRag] 百炼通道召回完成 | count={}", documents.size());
        return documents;
    }

    @Override
    public String getChannelName() {
        return "bailian";
    }
}
