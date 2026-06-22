package com.travel.aiagent.common.core.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 本地多路召回通道
 * 实现 Dense (PGVector) + Sparse (BM25/全文检索) 双路召回 + 合并去重
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tool-rag.channel", havingValue = "local")
public class LocalToolRagChannel implements ToolRagChannel {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 向量召回 TopK
     */
    private static final int DENSE_TOP_K = 5;

    /**
     * BM25 全文检索 TopK
     */
    private static final int SPARSE_TOP_K = 5;

    /**
     * 最终返回的文档数量
     */
    private static final int FINAL_TOP_K = 5;

    /**
     * 向量相似度阈值（低于此值的文档会被过滤）
     */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    public LocalToolRagChannel(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        log.info("[ToolRag] 初始化本地多路召回通道");
    }

    @Override
    public List<Document> retrieve(Query query) {
        String queryText = query.text();
        log.info("[ToolRag] 本地多路召回开始 | query={}", queryText);

        // 1. Dense 召回：PGVector 向量检索
        List<Document> denseResults = denseRetrieve(queryText);
        log.info("[ToolRag] Dense召回完成 | count={}", denseResults.size());

        // 2. Sparse 召回：BM25 全文检索
        List<Document> sparseResults = sparseRetrieve(queryText);
        log.info("[ToolRag] Sparse召回完成 | count={}", sparseResults.size());

        // 3. 合并去重
        List<Document> mergedResults = mergeAndDeduplicate(denseResults, sparseResults);
        log.info("[ToolRag] 合并去重完成 | count={}", mergedResults.size());

        // 4. 截断到最终 TopK
        if (mergedResults.size() > FINAL_TOP_K) {
            mergedResults = mergedResults.subList(0, FINAL_TOP_K);
        }

        log.info("[ToolRag] 本地多路召回完成 | finalCount={}", mergedResults.size());
        return mergedResults;
    }

    @Override
    public String getChannelName() {
        return "local";
    }

    /**
     * Dense 召回：使用 PGVector 进行向量相似度检索
     */
    private List<Document> denseRetrieve(String queryText) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(queryText)
                    .topK(DENSE_TOP_K)
                    .similarityThreshold(SIMILARITY_THRESHOLD)
                    .build();
            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.error("[ToolRag] Dense召回异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * Sparse 召回：使用 PostgreSQL 全文检索（模拟 BM25）
     * 使用 ts_rank 进行相关性排序
     */
    private List<Document> sparseRetrieve(String queryText) {
        try {
            // 将查询文本转换为 PostgreSQL 全文检索的 tsquery 格式
            // 例如："北京天气" -> "北京 & 天气"
            String tsQuery = convertToTsQuery(queryText);

            String sql = """
                    SELECT id, content, metadata,
                           ts_rank(to_tsvector('simple', content), to_tsquery('simple', ?)) AS rank
                    FROM travel_hub_vectors
                    WHERE to_tsvector('simple', content) @@ to_tsquery('simple', ?)
                    ORDER BY rank DESC
                    LIMIT ?
                    """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tsQuery, tsQuery, SPARSE_TOP_K);

            return rows.stream()
                    .map(row -> {
                        String id = (String) row.get("id");
                        String content = (String) row.get("content");
                        Double rank = (Double) row.get("rank");

                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("source", "bm25");
                        metadata.put("rank", rank);

                        return new Document(id, content, metadata);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[ToolRag] Sparse召回异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 将普通文本转换为 PostgreSQL tsquery 格式
     * 使用 simple 分词器，按空格分词后用 & 连接
     */
    private String convertToTsQuery(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // 移除特殊字符，按空格分词
        String[] tokens = text.replaceAll("[^\\w\\s]", "").trim().split("\\s+");
        return Arrays.stream(tokens)
                .filter(t -> !t.isBlank())
                .collect(Collectors.joining(" & "));
    }

    /**
     * 合并 Dense 和 Sparse 结果并去重
     * 优先保留 Dense 结果（向量语义更准确），Sparse 结果作为补充
     */
    private List<Document> mergeAndDeduplicate(List<Document> denseResults, List<Document> sparseResults) {
        // 使用 LinkedHashMap 保持插入顺序
        Map<String, Document> mergedMap = new LinkedHashMap<>();

        // 先加入 Dense 结果（优先级高）
        for (Document doc : denseResults) {
            mergedMap.put(doc.getId(), doc);
        }

        // 再加入 Sparse 结果（补充）
        for (Document doc : sparseResults) {
            if (!mergedMap.containsKey(doc.getId())) {
                mergedMap.put(doc.getId(), doc);
            }
        }

        return new ArrayList<>(mergedMap.values());
    }
}
