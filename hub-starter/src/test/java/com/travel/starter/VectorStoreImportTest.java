package com.travel.starter;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 🧪 PGVector 知识库文档批量导入测试
 * <p>
 * 每个 md 文件按 [TOOL_BEAN_NAME: xxx] 拆分成多个独立 Document，
 * 每个 Document 只包含一个工具的描述，检索更精准。
 * <p>
 * 运行前确保：
 * 1. PG 容器已启动：cd docker && docker-compose up -d postgres
 * 2. 已执行 sql/init_pgvector.sql 建表
 * 3. application-dev.yml 中 PG 连接配置正确
 * 4. DashScope API Key 可用（Embedding 需要调用云端）
 */
@Slf4j
@SpringBootTest
public class VectorStoreImportTest {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 正则：匹配 [TOOL_BEAN_NAME: xxx]
     */
    private static final Pattern TOOL_BEAN_PATTERN = Pattern.compile("\\[TOOL_BEAN_NAME:\\s*([a-zA-Z0-9_]+)]");

    /**
     * 批量导入 hub-tools/知识库文档/ 下的所有 md 文件到 PGVector
     * 每个工具拆分为独立 Document
     */
    @Test
    void importAllKnowledgeDocs() throws IOException {
        Path docsDir = Paths.get(System.getProperty("user.dir"), "..", "hub-tools", "知识库文档");

        if (!Files.exists(docsDir)) {
            log.error("❌ 目录不存在：{}", docsDir.toAbsolutePath());
            return;
        }

        List<Document> documents = new ArrayList<>();

        try (Stream<Path> files = Files.list(docsDir)) {
            files.filter(p -> p.toString().endsWith(".md"))
                    .forEach(filePath -> {
                        try {
                            String fileName = filePath.getFileName().toString();
                            String content = Files.readString(filePath);
                            String domain = inferDomain(fileName);

                            // 按 [TOOL_BEAN_NAME: xxx] 拆分，每个工具一个 Document
                            List<Document> toolDocs = splitByToolBean(content, fileName, domain);
                            documents.addAll(toolDocs);

                            log.info("📄 文档 {} 拆分为 {} 个工具 Document", fileName, toolDocs.size());

                        } catch (IOException e) {
                            log.error("❌ 读取文件失败：{}", filePath, e);
                        }
                    });
        }

        if (documents.isEmpty()) {
            log.warn("⚠️ 没有找到任何文档");
            return;
        }

        // DashScope Embedding API 单次最多处理 25 个文本，分批写入
        int batchSize = 20;
        int total = documents.size();
        log.info("🚀 开始向量化并写入 PGVector，共 {} 个工具文档，每批 {} 个...", total, batchSize);

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<Document> batch = documents.subList(i, end);
            vectorStore.add(batch);
            log.info("  ✅ 第 {} 批完成：{}-{}/{}", (i / batchSize + 1), i + 1, end, total);
        }

        log.info("✅ 全部导入完成！共写入 {} 个工具文档到 PGVector", total);

    }

    /**
     * 按 [TOOL_BEAN_NAME: xxx] 分割文档，每个工具生成独立 Document
     *
     * @param content  md 文件全文
     * @param fileName 文件名
     * @param domain   领域
     * @return 拆分后的 Document 列表
     */
    private List<Document> splitByToolBean(String content, String fileName, String domain) {
        List<Document> docs = new ArrayList<>();

        // 按 [TOOL_BEAN_NAME: xxx] 分割
        String[] blocks = TOOL_BEAN_PATTERN.split(content);
        Matcher matcher = TOOL_BEAN_PATTERN.matcher(content);

        // 收集所有 toolBeanName
        List<String> toolBeanNames = new ArrayList<>();
        while (matcher.find()) {
            toolBeanNames.add(matcher.group(1));
        }

        // blocks[0] 是文件头部（通常是标题），跳过
        // blocks[i] 对应 toolBeanNames[i-1]
        for (int i = 1; i < blocks.length && i - 1 < toolBeanNames.size(); i++) {
            String toolContent = blocks[i].trim();
            String toolBeanName = toolBeanNames.get(i - 1);

            if (toolContent.isEmpty()) continue;

            // 拼接完整内容：工具描述 + Bean 名
            String fullContent = toolContent + "\n[TOOL_BEAN_NAME: " + toolBeanName + "]";

            Document doc = new Document(
                    fullContent,
                    Map.of(
                            "fileName", fileName,
                            "domain", domain,
                            "toolBeanName", toolBeanName,
                            "type", "tool_knowledge"
                    )
            );

            docs.add(doc);
            log.info("  🔧 {} → {}", toolBeanName, domain);
        }

        return docs;
    }

    /**
     * 验证：语义检索测试
     */
    @Test
    void testSemanticSearch() {
        String query = "我想找一家火锅店";
        log.info("🔍 语义检索测试：'{}'", query);

        List<Document> results = vectorStore.similaritySearch(query);

        log.info("📋 检索到 {} 条结果：", results.size());
        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            String text = doc.getText();
            String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
            log.info("  [{}] {}", i + 1, preview);
        }
    }

    /**
     * 从文件名推断领域
     */
    private String inferDomain(String fileName) {
        if (fileName.contains("Restaurant") || fileName.contains("餐饮")) return "餐饮美食";
        if (fileName.contains("Route") || fileName.contains("路线")) return "出行路线";
        if (fileName.contains("Hotel") || fileName.contains("酒店")) return "酒店住宿";
        if (fileName.contains("Weather") || fileName.contains("天气")) return "天气穿搭";
        if (fileName.contains("Attraction") || fileName.contains("娱乐")) return "景点娱乐";
        if (fileName.contains("Flight") || fileName.contains("机票")) return "机票预订";
        if (fileName.contains("Amap") || fileName.contains("高德")) return "高德通用";
        return "通用";
    }
}
