package com.travel.aiagent.common.core.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地多路召回通道（工具知识库）。
 *
 * <p>检索流程（Dense + Sparse + 合并去重 + 重排 + 截断）已抽到 {@link HybridRagRetriever}
 * 公共引擎，本类只负责声明「工具知识库」这个检索源：
 * <pre>
 *   filterExpression: type == 'tool_knowledge'
 *     → 与 LongTermMemory（用户偏好，同表无 type 标记）等其他数据隔离，避免互相污染
 *   esIndex: travel_hub_tools（cjk 分析器，真 BM25）
 * </pre>
 *
 * <p>对比实验：想用 PG ts_rank 版稀疏召回替换 ES，见
 * {@link HybridRagRetriever#sparseRetrieveByPG}（改 retrieve 内的调用即可）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tool-rag.channel", havingValue = "local")
public class LocalToolRagChannel implements ToolRagChannel {

    /**
     * 工具知识库检索源配置
     */
    private static final RagSourceConfig TOOL_SOURCE = new RagSourceConfig(
            "tool_knowledge",
            "type == 'tool_knowledge'",
            "travel_hub_tools",
            20,   // denseTopK：向量召回候选数
            20,   // sparseTopK：BM25 召回候选数
            5,    // finalTopK：重排后最终返回条数
            0.5   // similarityThreshold：向量相似度阈值
    );

    private final HybridRagRetriever hybridRagRetriever;

    public LocalToolRagChannel(HybridRagRetriever hybridRagRetriever) {
        this.hybridRagRetriever = hybridRagRetriever;
        log.info("[ToolRag] 初始化本地多路召回通道");
    }

    @Override
    public List<Document> retrieve(Query query) {
        return hybridRagRetriever.retrieve(query.text(), TOOL_SOURCE);
    }

    @Override
    public String getChannelName() {
        return "local";
    }
}
