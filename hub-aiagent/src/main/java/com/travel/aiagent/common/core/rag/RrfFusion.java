package com.travel.aiagent.common.core.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF（Reciprocal Rank Fusion）多路融合排序——纯计算，零 token 成本。
 *
 * <p>多路召回各自的分数不可比（向量相似度 vs BM25 分 vs 跨模态相似度，
 * 量纲完全不同），RRF 绕开分数、只看<b>排名</b>：
 * <pre>
 *   score(doc) = Σ 1 / (k + rank_i(doc))      k 默认 60
 * </pre>
 * 一个文档在多路里都排得靠前 → 融合分高；只在单路出现 → 分低。
 *
 * <p>这是「多路召回 + 融合排序」的企业标准做法：不拍脑袋加权，也不依赖分数可比性。
 */
@Slf4j
@Component
public class RrfFusion {

    /** RRF 平滑常数（业界常用 60，抑制头部排名的过度优势） */
    private static final int RRF_K = 60;

    /**
     * 融合多路召回结果。
     *
     * @param rankedLists 各路召回结果（各自已按相关性降序）
     * @param topN        融合后保留条数
     * @return 按融合分降序的文档（metadata 里写入 rrf_score）
     */
    public List<Document> fuse(List<List<Document>> rankedLists, int topN) {
        Map<String, Document> documents = new LinkedHashMap<>();
        Map<String, Double> scores = new LinkedHashMap<>();

        for (List<Document> list : rankedLists) {
            if (list == null || list.isEmpty()) {
                continue;
            }
            for (int i = 0; i < list.size(); i++) {
                Document doc = list.get(i);
                double score = 1.0 / (RRF_K + i + 1);
                scores.merge(doc.getId(), score, Double::sum);
                documents.putIfAbsent(doc.getId(), doc);
            }
        }

        List<Document> fused = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> {
                    Document doc = documents.get(entry.getKey());
                    doc.getMetadata().put("rrf_score", entry.getValue());
                    return doc;
                })
                .toList();

        log.info("[RRF] 融合完成 | paths={} | candidates={} | fused={}",
                rankedLists.size(),
                rankedLists.stream().mapToInt(l -> l == null ? 0 : l.size()).sum(),
                fused.size());
        return fused;
    }
}
