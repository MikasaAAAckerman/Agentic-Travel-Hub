package com.travel.aiagent.common.core.rag;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.travel.common.constant.BizException;
import com.travel.common.constant.ServiceResponseTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 多路召回引擎：Dense (PGVector) + Sparse (ES BM25) → 合并去重 → 重排 → 截断。
 *
 * <p>从 LocalToolRagChannel 抽出的公共抽象——工具知识库与 PDF 知识库共用同一套
 * 检索流程，差异全部收敛到 {@link RagSourceConfig}（过滤表达式 / ES 索引 / TopK）。
 *
 * <p>各步骤的容错策略（沿用原实现）：
 * <ul>
 *   <li>Dense 异常 → 抛 BizException（向量库是地基，挂了直接失败）</li>
 *   <li>Sparse 异常 → fail-open 返回空列表（BM25 是补充，挂了不阻塞）</li>
 *   <li>重排异常 → fail-open 返回原始候选（见 ToolReranker）</li>
 * </ul>
 */
@Slf4j
@Component
public class HybridRagRetriever {

    /**
     * ES 地址（带默认值，本地 docker 环境）
     */
    @Value("${tool-rag.es-url:http://localhost:9200}")
    private String esUrl;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 工具重排器（可选）：配置了 tool-rerank.model 才有 bean，未配置时 getIfAvailable() 返回 null
     */
    private final ObjectProvider<ToolReranker> toolRerankerProvider;

    private final RestClient restClient;

    public HybridRagRetriever(VectorStore vectorStore,
                              JdbcTemplate jdbcTemplate,
                              ObjectProvider<ToolReranker> toolRerankerProvider) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.toolRerankerProvider = toolRerankerProvider;
        this.restClient = RestClient.builder().build();
        log.info("[HybridRag] 初始化多路召回引擎");
    }

    /**
     * 多路召回主流程：召回 → 重排 → 截断。
     *
     * @param queryText 查询文本（工具召回时是 plan，PDF 问答时是用户问题）
     * @param cfg       检索源配置
     * @return 合并去重 + 重排后的最终 TopK 文档
     */
    public List<Document> retrieve(String queryText, RagSourceConfig cfg) {
        List<Document> candidates = recall(queryText, cfg);

        // 重排（配置了 tool-rerank.model 才启用）：Cross-Encoder 对 (query, 候选) 逐对精排
        ToolReranker toolReranker = toolRerankerProvider.getIfAvailable();
        if (toolReranker != null) {
            candidates = toolReranker.rerank(queryText, candidates);
            log.info("[HybridRag] 重排完成 | source={} | count={}", cfg.name(), candidates.size());
        }

        // 截断到最终 TopK
        if (candidates.size() > cfg.finalTopK()) {
            candidates = candidates.subList(0, cfg.finalTopK());
        }

        log.info("[HybridRag] 多路召回完成 | source={} | finalCount={}", cfg.name(), candidates.size());
        return candidates;
    }

    /**
     * 只做召回（Dense + Sparse + 合并去重），<b>不重排、不截断</b>。
     *
     * <p>供「多路融合」场景使用——调用方拿到各路的原始候选后自己做 RRF 融合，
     * 再统一重排（见 PdfRagChannel 的文本路 + 图片路融合）。
     */
    public List<Document> recall(String queryText, RagSourceConfig cfg) {
        log.info("[HybridRag] 召回开始 | source={} | query={}", cfg.name(), queryText);

        // 1. Dense 召回：PGVector 向量检索
        List<Document> denseResults = denseRetrieve(queryText, cfg);
        log.info("[HybridRag] Dense召回完成 | source={} | count={}", cfg.name(), denseResults.size());

        // 2. Sparse 召回：BM25 全文检索（默认 ES；想对比 PG 版见 sparseRetrieveByPG）
        List<Document> sparseResults = sparseRetrieveByES(queryText, cfg);
        log.info("[HybridRag] Sparse召回完成 | source={} | count={}", cfg.name(), sparseResults.size());

        // 3. 合并去重
        List<Document> mergedResults = mergeAndDeduplicate(denseResults, sparseResults);
        log.info("[HybridRag] 合并去重完成 | source={} | count={}", cfg.name(), mergedResults.size());
        return mergedResults;
    }

    /**
     * Dense 召回：使用 PGVector 进行向量相似度检索。
     *
     * <p>filterExpression 用于知识库隔离（同一张表里存了工具知识 / PDF 分块 / 用户偏好等多种数据）。
     */
    private List<Document> denseRetrieve(String queryText, RagSourceConfig cfg) {
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(queryText)
                    .topK(cfg.denseTopK())
                    .similarityThreshold(cfg.similarityThreshold());
            if (cfg.filterExpression() != null && !cfg.filterExpression().isBlank()) {
                builder.filterExpression(cfg.filterExpression());
            }
            return vectorStore.similaritySearch(builder.build());
        } catch (Exception e) {
            log.error("[HybridRag] Dense召回异常 | source={}", cfg.name(), e);
            throw new BizException(ServiceResponseTypeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * Sparse 召回：使用 Elasticsearch BM25 全文检索（真 BM25 + cjk 中文双字分词）。
     *
     * <p>调用 ES {@code _search} API：match 查询（默认 OR + 评分排序），
     * 索引使用 cjk 分析器（ES 内置，中文按双字 bigram 切分，无需 IK 插件即可中文检索）。
     *
     * <p>_source 同时取 metadata（若索引里存了），供引用溯源（页码/bbox）使用。
     */
    private List<Document> sparseRetrieveByES(String queryText, RagSourceConfig cfg) {
        try {
            String body = JSON.toJSONString(Map.of(
                    "query", Map.of("match", Map.of("content", queryText)),
                    "size", cfg.sparseTopK(),
                    "_source", List.of("id", "content", "metadata")
            ));

            String resp = restClient.post()
                    .uri(esUrl + "/" + cfg.esIndex() + "/_search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JSONObject root = JSON.parseObject(resp);
            JSONArray hits = root.getJSONObject("hits").getJSONArray("hits");

            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < hits.size(); i++) {
                JSONObject hit = hits.getJSONObject(i);
                JSONObject source = hit.getJSONObject("_source");
                double score = hit.getDoubleValue("_score");

                Map<String, Object> metadata = new HashMap<>();
                // 索引里存了 metadata 就带上（PDF 分块的页码/bbox 靠它溯源）
                JSONObject esMetadata = source.getJSONObject("metadata");
                if (esMetadata != null) {
                    metadata.putAll(esMetadata);
                }
                metadata.put("source", "bm25_es");
                metadata.put("rank", score);

                docs.add(new Document(source.getString("id"), source.getString("content"), metadata));
            }
            return docs;
        } catch (Exception e) {
            log.error("[HybridRag] Sparse(ES) 召回异常 | esUrl={} | index={}", esUrl, cfg.esIndex(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Sparse 召回：使用 PostgreSQL 全文检索（模拟 BM25）——对比用，默认不启用。
     *
     * <p>使用 ts_rank 进行相关性排序：
     *   ┌─────────────────────────┬─────────────────┬──────────────────────────────────────────────────────┐
     *   │         BM25 有         │ PG ts_rank 有吗 │                         影响                         │
     *   ├─────────────────────────┼─────────────────┼──────────────────────────────────────────────────────┤
     *   │ IDF 逆文档频率          │ ❌ 没有！       │ 「的」「是」和「高迪」「米其林」权重一样——最大的缺陷 │
     *   ├─────────────────────────┼─────────────────┼──────────────────────────────────────────────────────┤
     *   │ 文档长度归一化（avgdl） │ ❌ 没有         │ 长文档天然词频高、白拿分                             │
     *   ├─────────────────────────┼─────────────────┼──────────────────────────────────────────────────────┤
     *   │ 词频非线性饱和（k₁）    │ ❌ 近似线性     │ 重复出现 10 次的词会过度加分                         │
     *   └─────────────────────────┴─────────────────┴──────────────────────────────────────────────────────┘
     *
     * <p>⚠️ 注意：本方法固定查主表 travel_hub_vectors（对比实验用），不支持 RagSourceConfig 的表切换。
     */
    private List<Document> sparseRetrieveByPG(String queryText, RagSourceConfig cfg) {
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

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tsQuery, tsQuery, cfg.sparseTopK());

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
            log.error("[HybridRag] Sparse(PG) 召回异常", e);
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
