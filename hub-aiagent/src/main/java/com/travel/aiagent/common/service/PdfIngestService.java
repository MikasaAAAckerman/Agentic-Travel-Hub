package com.travel.aiagent.common.service;

import com.travel.aiagent.common.core.pdf.CoordinateAwareTextStripper;
import com.travel.aiagent.common.core.pdf.PdfChunk;
import com.travel.aiagent.common.core.pdf.PdfChunker;
import com.travel.aiagent.common.core.pdf.PdfEsIndexer;
import com.travel.aiagent.common.core.pdf.PdfFileStore;
import com.travel.aiagent.common.core.pdf.PdfPageRenderer;
import com.travel.aiagent.common.core.pdf.PdfTextLine;
import com.travel.aiagent.common.core.rag.PdfImageVectorStore;
import com.travel.aiagent.common.core.rag.VisionEmbeddingModel;
import com.travel.aiagent.common.domain.PdfDocument;
import com.travel.aiagent.common.repository.PdfDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PDF 入库编排：解析 → 分块 → 向量化 → 双写（PGVector + ES）→ 页面渲染。
 *
 * <p>入库是重活（解析 + embedding + 渲染），放独立线程池异步执行，
 * 上传接口立即返回 docId，前端轮询文档列表看状态（PARSING → READY / FAILED）。
 *
 * <p>数据落点：
 * <pre>
 *   文本分块 → travel_hub_vectors（metadata.type = 'pdf_knowledge'，与其他知识库隔离）
 *            → ES travel_pdf_chunks（cjk 分析器，BM25 稀疏召回）
 *   页面图片 → 本地磁盘 pdf-store/{docId}/pages/（多模态向量化的原料）
 * </pre>
 */
@Slf4j
@Service
public class PdfIngestService {

    /** PDF 分块的 ES 索引名（BM25 稀疏召回） */
    public static final String PDF_ES_INDEX = "travel_pdf_chunks";

    /** 文本分块所在的 PGVector 表 */
    private static final String TEXT_VECTOR_TABLE = "travel_hub_vectors";

    /** 页面图片向量表（阶段 3 使用，删除时一并清理） */
    private static final String IMAGE_VECTOR_TABLE = "travel_pdf_images";

    /** 向量写入批大小（DashScope Embedding 单次上限 25） */
    private static final int VECTOR_BATCH_SIZE = 20;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final PdfChunker pdfChunker;
    private final PdfPageRenderer pdfPageRenderer;
    private final PdfFileStore pdfFileStore;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfEsIndexer pdfEsIndexer;

    /** 页面图片向量库（可选：配置了多模态向量模型才有 bean） */
    private final ObjectProvider<PdfImageVectorStore> pdfImageVectorStoreProvider;

    /** 视觉模型（可选：配置了 agent-model.vision 才有 bean），用于生成页面图文描述 */
    private final ObjectProvider<ChatClient> visionClientProvider;

    /** 入库线程池（守护线程，2 并发够用） */
    private final ExecutorService ingestExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "pdf-ingest");
        t.setDaemon(true);
        return t;
    });

    public PdfIngestService(VectorStore vectorStore,
                            JdbcTemplate jdbcTemplate,
                            PdfChunker pdfChunker,
                            PdfPageRenderer pdfPageRenderer,
                            PdfFileStore pdfFileStore,
                            PdfDocumentRepository pdfDocumentRepository,
                            PdfEsIndexer pdfEsIndexer,
                            ObjectProvider<PdfImageVectorStore> pdfImageVectorStoreProvider,
                            @Qualifier("visionClient") ObjectProvider<ChatClient> visionClientProvider) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.pdfChunker = pdfChunker;
        this.pdfPageRenderer = pdfPageRenderer;
        this.pdfFileStore = pdfFileStore;
        this.pdfDocumentRepository = pdfDocumentRepository;
        this.pdfEsIndexer = pdfEsIndexer;
        this.pdfImageVectorStoreProvider = pdfImageVectorStoreProvider;
        this.visionClientProvider = visionClientProvider;
    }

    /**
     * 受理上传：存原件 + 登记状态，重活丢后台线程。
     *
     * @return docId（前端用它轮询状态）
     */
    public String submit(byte[] content, String fileName) {
        String docId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 原件先落盘（前端 pdf.js 要加载它，越快越好）
        String filePath = pdfFileStore.saveOriginal(docId, fileName, content);

        pdfDocumentRepository.insert(PdfDocument.builder()
                .id(docId)
                .fileName(fileName)
                .filePath(filePath)
                .status(PdfDocument.STATUS_PARSING)
                .build());

        ingestExecutor.submit(() -> parseAndIndex(docId, content, fileName));
        log.info("[PdfIngest] 已受理上传 | docId={} | fileName={} | bytes={}", docId, fileName, content.length);
        return docId;
    }

    /**
     * 解析 + 入库主流程（后台线程）。
     */
    private void parseAndIndex(String docId, byte[] content, String fileName) {
        long start = System.currentTimeMillis();
        try (PDDocument pdDocument = Loader.loadPDF(content)) {
            int pageCount = pdDocument.getNumberOfPages();

            // 1. 抽取文本行（带坐标，供 bbox 高亮）
            CoordinateAwareTextStripper stripper = new CoordinateAwareTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(pageCount);
            stripper.getText(pdDocument);   // 触发 writeString 回调，结果在 getLines()
            List<PdfTextLine> lines = stripper.getLines();

            // 2. 分块（按页切，块不跨页，保证 bbox 单页有效）
            List<PdfChunk> chunks = pdfChunker.chunk(lines);

            // 3. 构建 Document 并写入向量库（分批，避免超 DashScope 单次上限）
            List<Document> documents = buildDocuments(docId, fileName, chunks);
            for (int i = 0; i < documents.size(); i += VECTOR_BATCH_SIZE) {
                vectorStore.add(documents.subList(i, Math.min(i + VECTOR_BATCH_SIZE, documents.size())));
            }
            log.info("[PdfIngest] 文本向量化完成 | docId={} | chunks={}", docId, documents.size());

            // 4. 写入 ES（BM25 稀疏召回）
            pdfEsIndexer.ensureIndex(PDF_ES_INDEX);
            pdfEsIndexer.bulkIndex(PDF_ES_INDEX, documents);

            // 5. 渲染页面图片（多模态向量的原料 + 看图作答用）
            List<String> pageImagePaths = new ArrayList<>(pageCount);
            for (int page = 1; page <= pageCount; page++) {
                byte[] png = pdfPageRenderer.renderPageToPng(pdDocument, page - 1);
                pageImagePaths.add(pdfFileStore.savePageImage(docId, page, png));
            }

            // 6. 多模态：页面图片 → 向量（跨模态检索）+ 图文描述（图片的"文本侧身"）
            vectorizePageImages(docId, fileName, pageImagePaths);

            pdfDocumentRepository.updateResult(docId, pageCount, chunks.size(), pageCount,
                    PdfDocument.STATUS_READY);
            log.info("[PdfIngest] 入库完成 | docId={} | pages={} | chunks={} | cost={}ms",
                    docId, pageCount, chunks.size(), System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("[PdfIngest] 入库失败 | docId={} | fileName={}", docId, fileName, e);
            pdfDocumentRepository.updateFailed(docId, e.getMessage());
        }
    }

    /**
     * 把分块转成 Spring AI Document（metadata 带 docId / 页码 / bbox，供引用溯源）。
     */
    private List<Document> buildDocuments(String docId, String fileName, List<PdfChunk> chunks) {
        List<Document> documents = new ArrayList<>(chunks.size());
        for (PdfChunk chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "pdf_knowledge");      // 知识库隔离标记
            metadata.put("docId", docId);
            metadata.put("fileName", fileName);
            metadata.put("page", chunk.page());
            metadata.put("chunkIndex", chunk.index());
            // bbox：PDF 用户空间（原点左下），前端 pdf.js 直接换算
            metadata.put("bboxX0", chunk.x0());
            metadata.put("bboxY0", chunk.y0());
            metadata.put("bboxX1", chunk.x1());
            metadata.put("bboxY1", chunk.y1());

            documents.add(new Document(docId + "_c" + chunk.index(), chunk.content(), metadata));
        }
        return documents;
    }

    /**
     * 页面图片多模态化：图片向量（跨模态检索）+ 图文描述（图片的"文本侧身"）。
     *
     * <p>为什么要「文本侧身」：图片只有向量的话，ES 稀疏召回和 Rerank（都是文本算法）都碰不到它；
     * 给它配一段 VLM 生成的图文描述，图片就能同时参与稠密、稀疏、重排三条路。
     *
     * <p>未配置多模态向量模型时整段跳过（PDF 链路降级为纯文本）。
     */
    private void vectorizePageImages(String docId, String fileName, List<String> pageImagePaths) {
        PdfImageVectorStore imageStore = pdfImageVectorStoreProvider.getIfAvailable();
        if (imageStore == null) {
            log.info("[PdfIngest] 未配置多模态向量模型，跳过图片向量化 | docId={}", docId);
            return;
        }

        ChatClient visionClient = visionClientProvider.getIfAvailable();
        if (visionClient == null) {
            log.info("[PdfIngest] 未配置视觉模型，页面描述用占位文本 | docId={}", docId);
        }

        List<Document> imageDocuments = new ArrayList<>(pageImagePaths.size());
        for (int i = 0; i < pageImagePaths.size(); i++) {
            int page = i + 1;
            String imagePath = pageImagePaths.get(i);

            // 图片的"文本侧身"：VLM 生成页面图文描述（表格转 markdown、图表说结论）
            String description = visionClient != null
                    ? describePage(visionClient, imagePath, page)
                    : "第 " + page + " 页";

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "pdf_image");
            metadata.put("docId", docId);
            metadata.put("fileName", fileName);
            metadata.put("page", page);
            metadata.put(VisionEmbeddingModel.META_IMAGE_PATH, imagePath);   // 供向量模型读图

            imageDocuments.add(new Document(docId + "_p" + page, description, metadata));
        }

        try {
            imageStore.add(imageDocuments);
        } catch (Exception e) {
            // fail-open：图片路是增强，挂了不影响文本检索
            log.error("[PdfIngest] 图片向量写入失败 | docId={} | pages={}", docId, pageImagePaths.size(), e);
        }
    }

    /**
     * 用视觉模型给页面图片生成图文描述（表格转 markdown、图表说结论）。
     */
    private String describePage(ChatClient visionClient, String imagePath, int page) {
        try {
            byte[] png = Files.readAllBytes(Paths.get(imagePath));
            Media media = Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_PNG)
                    .data(new ByteArrayResource(png))
                    .build();

            String description = visionClient.prompt()
                    .user(u -> u.text("""
                                    请描述这一页 PDF 的内容，用于后续检索：
                                    1. 提取正文要点（保持关键名词、数字、结论）
                                    2. 若包含表格，用 markdown 表格还原
                                    3. 若包含图表或图片，描述其内容与结论
                                    直接输出描述正文，不要寒暄。
                                    """)
                            .media(media))
                    .call()
                    .content();

            return (description == null || description.isBlank()) ? "第 " + page + " 页" : description;
        } catch (Exception e) {
            log.warn("[PdfIngest] 页面描述生成失败，用占位文本 | docId-page={}", page, e);
            return "第 " + page + " 页";
        }
    }

    /**
     * 删除文档：向量（文本 + 图片）+ ES 分块 + 本地文件 + 元数据记录。
     */
    public void delete(String docId) {
        if (pdfDocumentRepository.findById(docId) == null) {
            log.warn("[PdfIngest] 文档不存在，跳过删除 | docId={}", docId);
            return;
        }
        deleteVectors(TEXT_VECTOR_TABLE, docId);
        deleteVectors(IMAGE_VECTOR_TABLE, docId);
        pdfEsIndexer.deleteByDocId(PDF_ES_INDEX, docId);
        pdfFileStore.delete(docId);
        pdfDocumentRepository.deleteById(docId);
        log.info("[PdfIngest] 文档已删除 | docId={}", docId);
    }

    /**
     * 按 metadata.docId 删除某张向量表里的文档数据。
     * ⚠️ 表名是内部常量（非用户输入），不存在注入风险。
     */
    private void deleteVectors(String table, String docId) {
        try {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM " + table + " WHERE metadata->>'docId' = ?", docId);
            log.info("[PdfIngest] 向量已删除 | table={} | docId={} | count={}", table, docId, deleted);
        } catch (Exception e) {
            // 表可能尚未创建（阶段 3 才用），fail-open
            log.warn("[PdfIngest] 删除向量失败 | table={} | docId={}", table, docId, e);
        }
    }
}
