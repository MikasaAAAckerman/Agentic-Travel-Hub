package com.travel.aiagent.common.core.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PDF 页面图片向量库（travel_pdf_images 表）。
 *
 * <p>设计取舍：<b>不把 PgVectorStore 暴露成 Spring bean</b>，而是包在这个组件里内部持有——
 * 因为 Spring AI 自动配置的 {@code VectorStore} bean 是全项目按类型注入的（LocalToolRagChannel /
 * LongTermMemory / RagEvaluationService），再多一个 VectorStore 类型 bean 会造成注入歧义。
 * 包成普通组件后，现有注入点零改动，也无需手写 @Primary 主库 bean。
 *
 * <p>表结构见 sql/init_pdf_rag.sql（VECTOR(1536)，与文本表同维度）。
 * 未配置多模态向量模型时本组件不启用。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pdf-rag.vision-embedding-model")
public class PdfImageVectorStore {

    /** 页面图片向量表 */
    private static final String TABLE_NAME = "travel_pdf_images";

    private final PgVectorStore store;

    public PdfImageVectorStore(JdbcTemplate jdbcTemplate, Environment environment) {
        // 手工 new，刻意不注入容器：VisionEmbeddingModel 也是 EmbeddingModel 类型，
        // 注册成 bean 会让 PgVectorStoreAutoConfiguration 的 EmbeddingModel 注入产生歧义
        VisionEmbeddingModel visionEmbeddingModel = new VisionEmbeddingModel(environment);

        this.store = PgVectorStore.builder(jdbcTemplate, visionEmbeddingModel)
                .vectorTableName(TABLE_NAME)
                .dimensions(visionEmbeddingModel.dimensions())
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .idType(PgVectorStore.PgIdType.TEXT)
                // 表由 sql/init_pdf_rag.sql 建好；启动时不校验 schema（避免未建表就启动失败）
                .initializeSchema(false)
                .vectorTableValidationsEnabled(false)
                .build();
        log.info("[PdfImageStore] 初始化页面图片向量库 | table={} | dimensions={}",
                TABLE_NAME, visionEmbeddingModel.dimensions());
    }

    /**
     * 写入页面图片向量（Document 的 metadata 需带 imagePath，供 VisionEmbeddingModel 读图）。
     */
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        store.add(documents);
        log.info("[PdfImageStore] 图片向量写入完成 | count={}", documents.size());
    }

    /**
     * 跨模态检索：文本 query → 图片向量空间 → 命中页面。
     */
    public List<Document> search(String query, int topK) {
        return store.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build());
    }
}
