package com.travel.aiagent.common.core.rag;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 多模态向量模型（跨模态检索的地基）。
 *
 * <p>把【页面图片】和【文本 query】编码进【同一个向量空间】——这样用户用文字提问，
 * 就能直接检索到相关的页面图片（方案 D 的核心）。
 *
 * <p>实现 Spring AI 的 {@link EmbeddingModel} 接口，因此可以直接喂给
 * {@code PgVectorStore}：add() 存图片向量、similaritySearch() 用文本查图片。
 *
 * <p>配置（照 ChatClient 的模型池模式，只加一个节点）：
 * <pre>
 *   pdf-rag.vision-embedding-model: qwen.models.qwen3-vl-embedding
 *   # 模型池里：qwen.api-key / qwen.endpoint / qwen.models.qwen3-vl-embedding
 * </pre>
 *
 * <p>⚠️ <b>刻意不注册成 Spring bean</b>（没有 @Component）：它 implements EmbeddingModel，
 * 一旦进容器，PgVectorStoreAutoConfiguration 注入 EmbeddingModel 时就会和
 * DashScope 的文本 embedding bean 产生歧义，启动直接失败。
 * 由 {@link PdfImageVectorStore} 手工 new 出来持有即可。
 */
@Slf4j
public class VisionEmbeddingModel implements EmbeddingModel {

    /** 图片 Document 的 metadata key：页面 PNG 的本地路径 */
    public static final String META_IMAGE_PATH = "imagePath";

    /** 单图大小上限（百炼文档：3MB），超了接口会拒绝 */
    private static final int MAX_IMAGE_BYTES = 3 * 1024 * 1024;

    /**
     * DashScope 原生多模态向量 API 路径（不在 OpenAI 兼容模式下）。
     * 出处：百炼「如何使用 Multimodal-Embedding API」文档。
     */
    private static final String DEFAULT_API_PATH =
            "/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";

    private final String model;
    private final String apiKey;
    private final String apiUrl;
    private final int dimensions;
    private final RestClient restClient;

    public VisionEmbeddingModel(Environment environment) {
        // 引用路径解析：{provider}.models.{modelKey}（与 ChatClientConfig / ToolReranker 同款）
        String ref = environment.getProperty("pdf-rag.vision-embedding-model");
        int idx = ref.indexOf(".models.");
        String provider = ref.substring(0, idx);
        String modelKey = ref.substring(idx + ".models.".length());

        this.model = environment.getProperty(provider + ".models." + modelKey);
        this.apiKey = environment.getProperty(provider + ".api-key");
        String endpoint = environment.getProperty(provider + ".endpoint");
        this.dimensions = environment.getProperty("pdf-rag.vision-embedding-dimensions", Integer.class, 1536);
        String apiPath = environment.getProperty("pdf-rag.vision-embedding-path", DEFAULT_API_PATH);

        if (model == null || apiKey == null || endpoint == null) {
            throw new IllegalStateException("多模态向量模型配置缺失 | provider=" + provider
                    + " | modelKey=" + modelKey
                    + " | apiKey存在=" + (apiKey != null)
                    + " | endpoint存在=" + (endpoint != null));
        }

        // 从 provider.endpoint 推导原生 API 地址（scheme+host + 路径）
        URI uri = URI.create(endpoint);
        this.apiUrl = uri.getScheme() + "://" + uri.getHost() + apiPath;
        this.restClient = RestClient.builder().build();

        log.info("[VisionEmbed] 初始化 | model={} | dimensions={} | apiUrl={}", model, dimensions, apiUrl);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> instructions = request.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            embeddings.add(new Embedding(embedText(instructions.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        Object imagePath = document.getMetadata().get(META_IMAGE_PATH);
        if (imagePath != null) {
            return embedImage(String.valueOf(imagePath));
        }
        return embedText(document.getText());
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    // ═══════════════════════════════════════════════════════════
    // 内部：文本 / 图片 → 向量
    // ═══════════════════════════════════════════════════════════

    /**
     * 文本 → 向量（独立向量模式：文本/图片各自出向量，共享同一语义空间，故可跨模态检索）。
     *
     * <p>⚠️ 请求体必须是 {@code "input": {"contents": [...]}} 这种嵌套结构。
     * 若写成 {@code "input": [...]}（数组），DashScope 会走「URL 列表」解析分支，
     * 报 {@code InvalidParameter: url error, please check url}。
     *
     * <p>参考（百炼 Multimodal-Embedding API 的 cURL 示例）：
     * <pre>
     *   {"model":"qwen3-vl-embedding",
     *    "input":{"contents":[{"text":"..."}]},
     *    "parameters":{"dimension":1536}}
     * </pre>
     */
    private float[] embedText(String text) {
        Map<String, Object> body = Map.of(
                "model", model,
                "input", Map.of("contents", List.of(Map.of("text", text))),
                "parameters", Map.of("dimension", dimensions)
        );
        return doEmbed(body);
    }

    /**
     * 图片 → 向量（base64 data URI 直传，本地文件不用先传 OSS）。
     *
     * <p>限制（百炼文档）：单图 ≤ 3MB；支持 PNG/JPEG/WEBP/BMP 等；
     * data URI 格式固定 {@code data:image/{format};base64,{data}}。
     */
    private float[] embedImage(String imagePath) {
        try {
            byte[] png = Files.readAllBytes(Paths.get(imagePath));
            if (png.length > MAX_IMAGE_BYTES) {
                log.warn("[VisionEmbed] 图片超过 3MB 上限，接口可能拒绝 | imagePath={} | bytes={}",
                        imagePath, png.length);
            }
            String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(png);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", Map.of("contents", List.of(Map.of("image", dataUri))),
                    "parameters", Map.of("dimension", dimensions)
            );
            return doEmbed(body);
        } catch (Exception e) {
            log.error("[VisionEmbed] 图片向量化失败 | imagePath={}", imagePath, e);
            return new float[dimensions];
        }
    }

    private float[] doEmbed(Map<String, Object> body) {
        try {
            String resp = restClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(JSON.toJSONString(body))
                    .retrieve()
                    .body(String.class);

            JSONObject root = JSON.parseObject(resp);
            JSONArray embeddings = root.getJSONObject("output").getJSONArray("embeddings");
            JSONArray vector = embeddings.getJSONObject(0).getJSONArray("embedding");

            float[] result = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                result[i] = vector.getFloatValue(i);
            }
            return result;
        } catch (Exception e) {
            log.error("[VisionEmbed] 向量化调用异常 | model={}", model, e);
            return new float[dimensions];
        }
    }
}
