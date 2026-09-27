package com.travel.aiagent.common.core.rag;

import com.travel.aiagent.common.service.PdfIngestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * PDF 多模态召回通道：文本路 + 图片路 → RRF 融合 → 重排 → TopK。
 *
 * <pre>
 *   文本路：dense(PGVector) + sparse(ES BM25)  →  PDF 分块（带页码 + bbox）
 *   图片路：qwen3-vl-embedding 跨模态检索      →  页面图片（带页码，整页高亮）
 *   ↓
 *   RrfFusion（排名融合，分数不可比问题交给 RRF）
 *   ↓
 *   ToolReranker（对文本块/图片描述精排）
 * </pre>
 *
 * <p>文本路复用 {@link HybridRagRetriever}（与工具知识库同一套引擎，差异只在 RagSourceConfig）。
 */
@Slf4j
@Component
public class PdfRagChannel {

    /**
     * 文本路检索源：PDF 分块（type 隔离，与工具知识/用户偏好互不污染）
     */
    private static final RagSourceConfig PDF_TEXT_SOURCE = new RagSourceConfig(
            "pdf_knowledge",
            "type == 'pdf_knowledge'",
            PdfIngestService.PDF_ES_INDEX,
            20,   // denseTopK
            20,   // sparseTopK
            10,   // finalTopK（融合场景先多取一些，最终由调用方截断）
            0.5   // similarityThreshold
    );

    /** 图片路候选数 */
    private static final int IMAGE_TOP_K = 5;

    /** 融合后送入重排的候选数 */
    private static final int FUSION_TOP_K = 10;

    private final HybridRagRetriever hybridRagRetriever;
    private final RrfFusion rrfFusion;
    private final ObjectProvider<PdfImageVectorStore> pdfImageVectorStoreProvider;
    private final ObjectProvider<ToolReranker> toolRerankerProvider;

    public PdfRagChannel(HybridRagRetriever hybridRagRetriever,
                         RrfFusion rrfFusion,
                         ObjectProvider<PdfImageVectorStore> pdfImageVectorStoreProvider,
                         ObjectProvider<ToolReranker> toolRerankerProvider) {
        this.hybridRagRetriever = hybridRagRetriever;
        this.rrfFusion = rrfFusion;
        this.pdfImageVectorStoreProvider = pdfImageVectorStoreProvider;
        this.toolRerankerProvider = toolRerankerProvider;
    }

    /**
     * 多路召回 + 融合 + 重排。
     *
     * @param question 用户问题
     * @param topK     最终返回条数
     */
    public List<Document> retrieve(String question, int topK) {
        long start = System.currentTimeMillis();

        List<List<Document>> paths = new ArrayList<>();

        // ① 文本路：dense + sparse（不重排不截断，融合后统一重排）
        paths.add(hybridRagRetriever.recall(question, PDF_TEXT_SOURCE));

        // ② 图片路：文本 query → 图片向量空间（跨模态）
        PdfImageVectorStore imageStore = pdfImageVectorStoreProvider.getIfAvailable();
        if (imageStore != null) {
            try {
                paths.add(imageStore.search(question, IMAGE_TOP_K));
            } catch (Exception e) {
                // fail-open：图片路是增强，挂了继续文本路
                log.warn("[PdfRag] 图片路召回失败，继续文本路 | question={}", question, e);
            }
        } else {
            log.info("[PdfRag] 未启用图片向量库，仅文本路召回");
        }

        // ③ RRF 融合
        List<Document> fused = rrfFusion.fuse(paths, FUSION_TOP_K);

        // ④ 重排（文本块用正文、图片用页面描述参与精排）
        ToolReranker reranker = toolRerankerProvider.getIfAvailable();
        if (reranker != null) {
            fused = reranker.rerank(question, fused);
        }

        // ⑤ 截断
        if (fused.size() > topK) {
            fused = fused.subList(0, topK);
        }

        log.info("[PdfRag] 召回完成 | question={} | paths={} | final={} | cost={}ms",
                question, paths.size(), fused.size(), System.currentTimeMillis() - start);
        return fused;
    }
}
