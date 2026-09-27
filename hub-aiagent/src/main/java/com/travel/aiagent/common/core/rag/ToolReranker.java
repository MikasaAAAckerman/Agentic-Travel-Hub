package com.travel.aiagent.common.core.rag;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具召回重排器（Cross-Encoder 重排）
 *
 * <p>多路召回（Dense + Sparse）拿到候选池后，用专门的 Rerank 模型对
 * (query, 候选工具) 逐对打分，把真正相关的工具排到前面，弥补双塔召回
 * 丢失的细粒度语义交互。
 *
 * <p>调用 DashScope 原生 Rerank API（不在 OpenAI 兼容模式下，独立路径）：
 * <pre>
 *   POST {scheme+host}/api/v1/services/rerank/text-rerank/text-rerank
 * </pre>
 *
 * <p>配置（引用模型池路径，与 ChatClientConfig 相同的解析逻辑）：
 * <pre>
 *   tool-rerank.model: qwen.models.qwen3.7-text-rerank
 * </pre>
 * 未配置 tool-rerank.model 时本组件不启用（@ConditionalOnProperty），
 * 召回通道不调用重排，保持原行为。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tool-rerank.model")
public class ToolReranker {

    /** DashScope 原生 Rerank API 路径（与 provider.endpoint 的 scheme+host 拼接） */
    private static final String RERANK_API_PATH = "/api/v1/services/rerank/text-rerank/text-rerank";

    private final String model;
    private final String apiKey;
    private final String rerankUrl;
    private final RestClient restClient;

    public ToolReranker(Environment environment) {
        // 引用路径解析：{provider}.models.{modelKey} → qwen.models.qwen3.7-text-rerank
        String ref = environment.getProperty("tool-rerank.model");
        int idx = ref.indexOf(".models.");
        String provider = ref.substring(0, idx);
        String modelKey = ref.substring(idx + ".models.".length());

        this.model = environment.getProperty(provider + ".models." + modelKey);
        this.apiKey = environment.getProperty(provider + ".api-key");
        String endpoint = environment.getProperty(provider + ".endpoint");

        if (model == null || apiKey == null || endpoint == null) {
            throw new IllegalStateException("重排模型配置缺失 | provider=" + provider
                    + " | modelKey=" + modelKey
                    + " | apiKey存在=" + (apiKey != null)
                    + " | endpoint存在=" + (endpoint != null));
        }

        // 从 provider.endpoint 推导 Rerank API 地址（scheme+host + 固定路径）
        URI uri = URI.create(endpoint);
        this.rerankUrl = uri.getScheme() + "://" + uri.getHost() + RERANK_API_PATH;
        this.restClient = RestClient.builder().build();

        log.info("[ToolRerank] 初始化 | model={} | rerankUrl={}", model, rerankUrl);
    }

    /**
     * 对候选工具重排：调用 Rerank API 按 (query, 工具描述) 相关性打分，
     * 按分数降序返回全部候选（截断由调用方 FINAL_TOP_K 负责），
     * 分数写入 metadata["rerank_score"] 供评测/日志使用。
     *
     * <p>fail-open：重排异常时原样返回候选（召回不因重排挂掉）。
     */
    public List<Document> rerank(String query, List<Document> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }

        List<String> texts = candidates.stream().map(Document::getText).toList();
        String body = JSON.toJSONString(Map.of(
                "model", model,
                "input", Map.of("query", query, "documents", texts),
                "parameters", Map.of("top_n", texts.size(), "return_documents", true)
        ));

        try {
            String resp = restClient.post()
                    .uri(rerankUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JSONObject root = JSON.parseObject(resp);
            JSONArray results = root.getJSONObject("output").getJSONArray("results");

            // 按结果顺序重排（result.index 指向入参 documents 的下标）
            List<Document> ranked = new ArrayList<>(texts.size());
            for (int i = 0; i < results.size(); i++) {
                JSONObject r = results.getJSONObject(i);
                int idx = r.getIntValue("index");
                if (idx >= 0 && idx < candidates.size()) {
                    Document doc = candidates.get(idx);
                    doc.getMetadata().put("rerank_score", r.getDoubleValue("relevance_score"));
                    ranked.add(doc);
                }
            }
            log.info("[ToolRerank] 重排完成 | query={} | candidates={} | ranked={}",
                    query, candidates.size(), ranked.size());
            return ranked;
        } catch (Exception e) {
            log.error("[ToolRerank] 重排异常，返回原始候选 | query={}", query, e);
            return candidates;
        }
    }
}
