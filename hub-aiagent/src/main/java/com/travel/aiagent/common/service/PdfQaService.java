package com.travel.aiagent.common.service;

import com.alibaba.fastjson2.JSON;
import com.travel.aiagent.common.core.pdf.PdfFileStore;
import com.travel.aiagent.common.core.rag.PdfRagChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PDF 多模态问答：多路召回 → 组装（文本块 + 命中页图片）→ 视觉模型作答 → 结构化 SSE。
 *
 * <p>SSE 事件协议（前端按 type 分发）：
 * <pre>
 *   {"type":"citations", "citations":[{index,chunkId,docId,fileName,page,kind,bbox,snippet,rrfScore,rerankScore}]}
 *   {"type":"text", "text":"..."}     ← 流式增量
 *   {"type":"done"}
 *   {"type":"error", "message":"..."}
 * </pre>
 *
 * <p>引用里带 bbox（PDF 用户空间，原点左下）→ 前端 pdf.js 直接
 * {@code viewport.convertToViewportRectangle()} 换算后画高亮框。
 */
@Slf4j
@Service
public class PdfQaService {

    /** 送入生成阶段的召回条数 */
    private static final int TOP_K = 5;

    /** 最多附几张页面图片给视觉模型（控制图片 token 成本） */
    private static final int MAX_PAGE_IMAGES = 3;

    private final PdfRagChannel pdfRagChannel;
    private final PdfFileStore pdfFileStore;
    private final ObjectProvider<ChatClient> visionClientProvider;
    private final ObjectProvider<ChatClient> assistantClientProvider;

    public PdfQaService(PdfRagChannel pdfRagChannel,
                        PdfFileStore pdfFileStore,
                        @Qualifier("visionClient") ObjectProvider<ChatClient> visionClientProvider,
                        @Qualifier("assistantClient") ObjectProvider<ChatClient> assistantClientProvider) {
        this.pdfRagChannel = pdfRagChannel;
        this.pdfFileStore = pdfFileStore;
        this.visionClientProvider = visionClientProvider;
        this.assistantClientProvider = assistantClientProvider;
    }

    /**
     * 提问（SSE 流式返回）。
     */
    public Flux<ServerSentEvent<String>> ask(String question) {
        return Flux.defer(() -> {
            long start = System.currentTimeMillis();

            // 1. 多路召回 + 融合 + 重排
            List<Document> hits = pdfRagChannel.retrieve(question, TOP_K);
            log.info("[PdfQa] 检索完成 | question={} | hits={} | cost={}ms",
                    question, hits.size(), System.currentTimeMillis() - start);

            // 2. 引用事件（前端据此渲染 [n] 锚点 + 悬浮高亮）
            ServerSentEvent<String> citationsEvent = event("citations", Map.of(
                    "type", "citations",
                    "citations", buildCitations(hits)));

            if (hits.isEmpty()) {
                return Flux.just(
                        citationsEvent,
                        textEvent("知识库里没有检索到相关内容喵，请换种问法或先上传文档～"),
                        event("done", Map.of("type", "done")));
            }

            // 3. 选生成模型：优先视觉模型（能看图），退化到文本模型（只喂文本块）
            ChatClient visionClient = visionClientProvider.getIfAvailable();
            ChatClient client = visionClient != null ? visionClient : assistantClientProvider.getIfAvailable();
            if (client == null) {
                return Flux.just(citationsEvent,
                        event("error", Map.of("type", "error", "message", "未配置可用的生成模型")));
            }

            // 4. 命中页图片（视觉模型才附）
            List<Media> pageImages = visionClient != null ? collectPageImages(hits) : List.of();
            log.info("[PdfQa] 生成开始 | model={} | pages={}",
                    visionClient != null ? "vision" : "text", pageImages.size());

            // 5. 流式作答
            String prompt = buildPrompt(question, buildContext(hits));
            Flux<ServerSentEvent<String>> answerStream = client.prompt()
                    .user(u -> {
                        u.text(prompt);
                        if (!pageImages.isEmpty()) {
                            u.media(pageImages.toArray(new Media[0]));
                        }
                    })
                    .stream()
                    .content()
                    .map(this::textEvent)
                    .onErrorResume(e -> {
                        log.error("[PdfQa] 生成异常 | question={}", question, e);
                        return Flux.just(event("error",
                                Map.of("type", "error", "message", "生成失败：" + e.getMessage())));
                    });

            return Flux.concat(Flux.just(citationsEvent), answerStream,
                    Flux.just(event("done", Map.of("type", "done"))));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ═══════════════════════════════════════════════════════════
    // 内部：引用 / 上下文 / 图片
    // ═══════════════════════════════════════════════════════════

    /**
     * 构建引用列表（含 bbox，供前端高亮）。
     */
    private List<Map<String, Object>> buildCitations(List<Document> hits) {
        List<Map<String, Object>> citations = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            Document doc = hits.get(i);
            Map<String, Object> meta = doc.getMetadata();

            Map<String, Object> citation = new LinkedHashMap<>();
            citation.put("index", i + 1);
            citation.put("chunkId", doc.getId());
            citation.put("docId", meta.get("docId"));
            citation.put("fileName", meta.get("fileName"));
            citation.put("page", meta.get("page"));
            citation.put("kind", meta.get("type"));      // pdf_knowledge（正文）/ pdf_image（页面图）

            // bbox：PDF 用户空间（原点左下）；图片类引用没有 bbox → 前端整页高亮
            if (meta.get("bboxX0") != null) {
                citation.put("bbox", Map.of(
                        "x0", meta.get("bboxX0"),
                        "y0", meta.get("bboxY0"),
                        "x1", meta.get("bboxX1"),
                        "y1", meta.get("bboxY1")));
            }

            // 分数（召回可视化面板用）
            citation.put("rrfScore", meta.get("rrf_score"));
            citation.put("rerankScore", meta.get("rerank_score"));

            String text = doc.getText();
            citation.put("snippet", text.length() > 120 ? text.substring(0, 120) + "..." : text);

            citations.add(citation);
        }
        return citations;
    }

    /**
     * 把命中文档拼成带编号的上下文（编号与引用一一对应）。
     */
    private String buildContext(List<Document> hits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            Document doc = hits.get(i);
            Map<String, Object> meta = doc.getMetadata();
            String kind = "pdf_image".equals(meta.get("type")) ? "页面图" : "正文";
            sb.append("[").append(i + 1).append("] ")
                    .append(meta.getOrDefault("fileName", "未知文档"))
                    .append(" 第").append(meta.getOrDefault("page", "?")).append("页")
                    .append("（").append(kind).append("）\n")
                    .append(doc.getText()).append("\n\n");
        }
        return sb.toString();
    }

    private String buildPrompt(String question, String context) {
        return """
                你是 PDF 文档问答助手，请严格依据下面提供的资料回答问题。

                【资料】
                %s
                【要求】
                1. 只依据资料作答；资料中没有的信息不要编造，明确说"资料中未提及"
                2. 引用资料时在句末标注编号，如 [1][2]
                3. 涉及表格的问题用 markdown 表格回答

                【问题】
                %s
                """.formatted(context, question);
    }

    /**
     * 收集命中页的图片（去重 + 限流），喂给视觉模型。
     */
    private List<Media> collectPageImages(List<Document> hits) {
        List<Media> medias = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Document doc : hits) {
            if (medias.size() >= MAX_PAGE_IMAGES) {
                break;
            }
            Object docId = doc.getMetadata().get("docId");
            Object page = doc.getMetadata().get("page");
            if (docId == null || page == null) {
                continue;
            }
            if (!seen.add(docId + "-" + page)) {
                continue;
            }
            try {
                byte[] png = pdfFileStore.readPageImage(String.valueOf(docId), ((Number) page).intValue());
                medias.add(Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_PNG)
                        .data(new ByteArrayResource(png))
                        .build());
            } catch (Exception e) {
                log.warn("[PdfQa] 读取页面图失败 | docId={} | page={}", docId, page);
            }
        }
        return medias;
    }

    // ═══════════════════════════════════════════════════════════
    // SSE 事件封装
    // ═══════════════════════════════════════════════════════════

    private ServerSentEvent<String> textEvent(String text) {
        return event("text", Map.of("type", "text", "text", text));
    }

    private ServerSentEvent<String> event(String name, Map<String, Object> payload) {
        return ServerSentEvent.<String>builder()
                .event(name)
                .data(JSON.toJSONString(payload))
                .build();
    }
}
