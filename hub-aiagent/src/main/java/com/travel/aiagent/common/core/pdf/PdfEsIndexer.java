package com.travel.aiagent.common.core.pdf;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF 分块的 ES 索引维护（BM25 稀疏召回的写入侧）。
 *
 * <p>工具知识库的 ES 索引是手工一次性建的，代码里只有查询侧；
 * 这里把「建索引 + 写入 + 删除」补全，让 PDF 知识库可增删。
 *
 * <p>索引 mapping 用 ES 内置 cjk 分析器（中文双字 bigram，无需 IK 插件），
 * docId 显式映射为 keyword，方便按文档删除。
 */
@Slf4j
@Component
public class PdfEsIndexer {

    /**
     * ES 地址（复用 tool-rag.es-url，本地 docker 环境）
     */
    @Value("${tool-rag.es-url:http://localhost:9200}")
    private String esUrl;

    private final RestClient restClient = RestClient.builder().build();

    /**
     * 确保索引存在（幂等：已存在时 ES 返回 400 resource_already_exists_exception，视为成功）。
     */
    public void ensureIndex(String index) {
        String mapping = JSON.toJSONString(Map.of(
                "mappings", Map.of(
                        "properties", Map.of(
                                "content", Map.of("type", "text", "analyzer", "cjk"),
                                "docId", Map.of("type", "keyword"),
                                "metadata", Map.of("type", "object")
                        )
                )
        ));

        try {
            restClient.put()
                    .uri(esUrl + "/" + index)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapping)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[PdfES] 索引已创建 | index={}", index);
        } catch (HttpClientErrorException e) {
            if (e.getResponseBodyAsString().contains("resource_already_exists_exception")) {
                log.info("[PdfES] 索引已存在 | index={}", index);
            } else {
                log.warn("[PdfES] 索引创建失败（继续尝试写入）| index={} | body={}",
                        index, e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.warn("[PdfES] 索引创建异常（继续尝试写入）| index={}", index, e);
        }
    }

    /**
     * 批量写入分块（NDJSON bulk）。
     */
    public void bulkIndex(String index, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        StringBuilder ndjson = new StringBuilder();
        for (Document doc : documents) {
            Map<String, Object> action = Map.of("index",
                    Map.of("_index", index, "_id", doc.getId()));
            ndjson.append(JSON.toJSONString(action)).append('\n');

            Map<String, Object> body = new HashMap<>();
            body.put("id", doc.getId());
            body.put("content", doc.getText());
            body.put("docId", doc.getMetadata().get("docId"));
            body.put("metadata", doc.getMetadata());
            ndjson.append(JSON.toJSONString(body)).append('\n');
        }

        try {
            String resp = restClient.post()
                    .uri(esUrl + "/_bulk")
                    .contentType(MediaType.APPLICATION_NDJSON)
                    .body(ndjson.toString())
                    .retrieve()
                    .body(String.class);

            JSONObject root = JSON.parseObject(resp);
            if (root != null && root.getBooleanValue("errors")) {
                log.warn("[PdfES] bulk 部分失败 | index={} | resp={}", index, resp);
            } else {
                log.info("[PdfES] bulk 写入完成 | index={} | count={}", index, documents.size());
            }
        } catch (Exception e) {
            // fail-open：ES 是稀疏召回的补充，写入失败不阻塞入库
            log.error("[PdfES] bulk 写入异常 | index={} | count={}", index, documents.size(), e);
        }
    }

    /**
     * 按 docId 删除该文档的全部分块。
     */
    public void deleteByDocId(String index, String docId) {
        try {
            restClient.post()
                    .uri(esUrl + "/" + index + "/_delete_by_query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(JSON.toJSONString(Map.of("query", Map.of("term", Map.of("docId", docId)))))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[PdfES] 已删除文档分块 | index={} | docId={}", index, docId);
        } catch (Exception e) {
            log.warn("[PdfES] 删除文档分块失败 | index={} | docId={}", index, docId, e);
        }
    }
}
