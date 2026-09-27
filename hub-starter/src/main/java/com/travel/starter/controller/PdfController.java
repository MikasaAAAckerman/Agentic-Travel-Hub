package com.travel.starter.controller;

import com.travel.aiagent.common.core.pdf.PdfFileStore;
import com.travel.aiagent.common.domain.PdfDocument;
import com.travel.aiagent.common.repository.PdfDocumentRepository;
import com.travel.aiagent.common.service.PdfIngestService;
import com.travel.aiagent.common.service.PdfQaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * PDF 多模态 RAG —— 文档管理 API。
 *
 * <ul>
 *   <li>POST   /api/pdf/upload            上传 PDF（异步解析入库，立即返回 docId）</li>
 *   <li>GET    /api/pdf/documents         文档列表（含解析状态，前端轮询）</li>
 *   <li>DELETE /api/pdf/documents/{docId} 删除文档（向量 + ES + 文件 + 记录）</li>
 *   <li>GET    /api/pdf/file/{docId}      原 PDF 文件流（前端 pdf.js 加载）</li>
 *   <li>GET    /api/pdf/page/{docId}/{p}  页面图片（调试/预览用）</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfIngestService pdfIngestService;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfFileStore pdfFileStore;
    private final PdfQaService pdfQaService;

    public PdfController(PdfIngestService pdfIngestService,
                         PdfDocumentRepository pdfDocumentRepository,
                         PdfFileStore pdfFileStore,
                         PdfQaService pdfQaService) {
        this.pdfIngestService = pdfIngestService;
        this.pdfDocumentRepository = pdfDocumentRepository;
        this.pdfFileStore = pdfFileStore;
        this.pdfQaService = pdfQaService;
    }

    /**
     * 上传 PDF：受理后立即返回 docId，解析在后台线程进行。
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Map.of("code", 400, "message", "文件为空");
        }
        String fileName = file.getOriginalFilename();
        log.info("[PdfApi] 收到上传 | fileName={} | bytes={}", fileName, file.getSize());

        String docId = pdfIngestService.submit(file.getBytes(), fileName);
        return Map.of(
                "code", 200,
                "docId", docId,
                "status", PdfDocument.STATUS_PARSING);
    }

    /**
     * 文档列表（按上传时间倒序），前端轮询看 PARSING → READY / FAILED。
     */
    @GetMapping("/documents")
    public List<PdfDocument> documents() {
        return pdfDocumentRepository.findAll();
    }

    /**
     * 多模态问答（SSE 结构化事件流）。
     *
     * <p>事件：citations（引用 + bbox）→ text（流式增量）→ done / error。
     */
    @GetMapping(value = "/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> qaStream(@RequestParam("question") String question) {
        log.info("[PdfApi] 收到提问 | question={}", question);
        return pdfQaService.ask(question);
    }

    /**
     * 删除文档：向量 + ES 分块 + 本地文件 + 元数据记录一并清理。
     */
    @DeleteMapping("/documents/{docId}")
    public Map<String, Object> delete(@PathVariable("docId") String docId) {
        pdfIngestService.delete(docId);
        return Map.of("code", 200, "message", "已删除", "docId", docId);
    }

    /**
     * 原 PDF 文件流（前端 pdf.js 加载并做 bbox 高亮）。
     */
    @GetMapping("/file/{docId}")
    public ResponseEntity<byte[]> file(@PathVariable("docId") String docId) {
        byte[] bytes = pdfFileStore.readOriginal(docId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.pdf\"")
                .body(bytes);
    }

    /**
     * 页面图片（调试 / 预览用）。
     */
    @GetMapping("/page/{docId}/{page}")
    public ResponseEntity<byte[]> pageImage(@PathVariable("docId") String docId,
                                            @PathVariable("page") int page) {
        byte[] bytes = pdfFileStore.readPageImage(docId, page);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .body(bytes);
    }
}
