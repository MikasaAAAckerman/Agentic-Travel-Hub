package com.travel.starter;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 🧪 PGVector 知识库版本管理 Demo
 *
 * 演示如何通过 metadata 中的 version 字段实现版本管理：
 * 1. 导入不同版本的知识库文档
 * 2. 按版本查询（filterExpression）
 * 3. 对比不同版本的检索结果
 *
 * 运行前确保：
 * 1. PG 容器已启动：cd docker && docker-compose up -d postgres
 * 2. 已执行 sql/init_pgvector.sql 建表
 * 3. application-dev.yml 中 PG 连接配置正确
 * 4. DashScope API Key 可用（Embedding 需要调用云端）
 */
@Slf4j
@SpringBootTest
public class VersionedKnowledgeBaseTest {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 正则：匹配 [TOOL_BEAN_NAME: xxx]
     */
    private static final Pattern TOOL_BEAN_PATTERN = Pattern.compile("\\[TOOL_BEAN_NAME:\\s*([a-zA-Z0-9_]+)]");

    /**
     * 测试1：导入指定版本的知识库文档
     *
     * 使用方式：修改 VERSION 常量，运行此方法导入不同版本
     */
    @Test
    void importVersionedDocuments() throws IOException {
        // ========== 配置区：修改这里来导入不同版本 ==========
        String version = "1.0";  // 版本号：v1.0, v1.1, v1.2...
        String domain = "tool_rag";  // 领域标识
        // ==================================================

        log.info("🚀 开始导入版本 {} 的知识库文档...", version);

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
                            String inferredDomain = inferDomain(fileName);

                            // 按 [TOOL_BEAN_NAME: xxx] 拆分，每个工具一个 Document
                            List<Document> toolDocs = splitByToolBean(content, fileName, inferredDomain, version, domain);
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

        log.info("✅ 版本 {} 导入完成！共写入 {} 个工具文档到 PGVector", version, total);
    }

    /**
     * 测试2：按版本查询
     *
     * 使用 filterExpression 过滤指定版本的文档
     */
    @Test
    void searchByVersion() {
        // ========== 配置区：修改这里来查询不同版本 ==========
        String query = "三亚有什么好玩的景点？";
        String version = "1.0";  // 查询 v1.0 版本的文档
        // ==================================================

        log.info("🔍 按版本查询：query='{}', version='{}'", query, version);

        // 使用 filterExpression 过滤版本
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .filterExpression("version == '" + version + "'")
                        .build()
        );

        log.info("📋 版本 {} 检索到 {} 条结果：", version, results.size());
        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            String text = doc.getText();
            String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
            Map<String, Object> metadata = doc.getMetadata();

            log.info("  [{}] toolBeanName={}, version={}", i + 1, metadata.get("toolBeanName"), metadata.get("version"));
            log.info("      {}", preview);
        }
    }

    /**
     * 测试3：对比不同版本的检索结果
     *
     * 分别查询 v1.0 和 v1.1，对比召回的文档差异
     */
    @Test
    void compareVersions() {
        String query = "三亚有什么好玩的景点？";
        String[] versions = {"1.0", "1.1"};

        log.info("🔍 版本对比测试：query='{}'", query);
        log.info("========================================");

        for (String version : versions) {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(5)
                            .filterExpression("version == '" + version + "'")
                            .build()
            );

            log.info("📋 版本 {} 检索到 {} 条结果：", version, results.size());
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                String toolBeanName = (String) doc.getMetadata().get("toolBeanName");
                log.info("  [{}] {}", i + 1, toolBeanName);
            }
            log.info("----------------------------------------");
        }

        log.info("✅ 版本对比完成！请查看上方日志，对比两个版本的召回差异");
    }

    /**
     * 测试4：查询所有版本的文档（不过滤）
     *
     * 用于验证版本字段是否正确写入
     */
    @Test
    void searchAllVersions() {
        String query = "三亚有什么好玩的景点？";

        log.info("🔍 查询所有版本（不过滤）：query='{}'", query);

        // 不使用 filterExpression，查询所有版本
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(10)
                        .build()
        );

        log.info("📋 检索到 {} 条结果：", results.size());

        // 按版本分组统计
        Map<String, List<Document>> groupByVersion = new HashMap<>();
        for (Document doc : results) {
            String version = (String) doc.getMetadata().get("version");
            groupByVersion.computeIfAbsent(version, k -> new ArrayList<>()).add(doc);
        }

        log.info("📊 版本分布：");
        for (Map.Entry<String, List<Document>> entry : groupByVersion.entrySet()) {
            log.info("  版本 {}: {} 条", entry.getKey(), entry.getValue().size());
        }
    }

    /**
     * 按 [TOOL_BEAN_NAME: xxx] 分割文档，每个工具生成独立 Document（带版本信息）
     */
    private List<Document> splitByToolBean(String content, String fileName, String domain, String version, String kbDomain) {
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

            // 生成唯一 ID：toolBeanName + version
            String docId = toolBeanName + "_v" + version;

            Document doc = new Document(
                    docId,
                    fullContent,
                    Map.of(
                            "fileName", fileName,
                            "domain", domain,
                            "toolBeanName", toolBeanName,
                            "type", "tool_knowledge",
                            "version", version,           // 版本号
                            "kbDomain", kbDomain,         // 知识库领域
                            "isActive", true              // 是否激活
                    )
            );

            docs.add(doc);
            log.info("  🔧 {} → {} (version={})", toolBeanName, domain, version);
        }

        return docs;
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
