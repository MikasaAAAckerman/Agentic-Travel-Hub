package com.travel.aiagent.common.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 🗄️ 长期记忆服务 (LongTermMemory)
 * 基于 PGVector 向量数据库，利用 Embedding 检索用户的长期偏好。
 * <p>
 * 工作流程：
 * 1. saveUserPreference → DashScope Embedding 将文本转为向量 → 存入 PGVector
 * 2. recallPreferences → 用户提问转为向量 → 在 PGVector 中做相似度搜索 → 返回匹配结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LongTermMemory {

    private final VectorStore vectorStore;

    /**
     * 📥 录入长期偏好（比如用户说："我其实对海鲜过敏"）
     *
     * @param userId         用户 ID，防止把别人的偏好记混
     * @param preferenceText 偏好内容
     */
    public void saveUserPreference(String userId, String preferenceText) {
        log.info("💾 [LongTermMemory] 正在将用户 {} 的偏好进行向量化存储：{}", userId, preferenceText);

        // 创建文档，带上 metadata 用于后续按 userId 过滤
        Document document = new Document(
                preferenceText,
                Map.of("userId", userId, "type", "user_preference")
        );

        // DashScope Embedding 自动将文本转为向量，存入 PGVector
        vectorStore.add(List.of(document));
        log.info("✅ [LongTermMemory] 偏好向量化完毕并入库");
    }

    /**
     * 📤 检索长期偏好（RAG 核心逻辑）
     *
     * @param userId       当前用户 ID
     * @param currentQuery 用户当前的问题（用于语义匹配）
     * @return 检索到的相关偏好的浓缩文本，无匹配时返回空字符串
     */
    public String recallPreferences(String userId, String currentQuery) {
        log.info("📡 [LongTermMemory] 正在为用户 {} 检索与 '{}' 相关的长期记忆...", userId, currentQuery);

        try {
            // 带过滤器的向量搜索：按 userId 隔离 + 相似度阈值 + TopK
            List<Document> similarDocuments = vectorStore.similaritySearch(
                    SearchRequest.builder().query(currentQuery)
                            .topK(3)
                            .similarityThreshold(0.7)
                            .filterExpression("userId == '" + userId + "'").build()
            );

            if (similarDocuments.isEmpty()) {
                return "";
            }

            String recalledContext = similarDocuments.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("；"));

            log.info("🎯 [LongTermMemory] 捕获到长期记忆：{}", recalledContext);
            return "【该用户长期偏好】：\n" + recalledContext + "\n";

        } catch (Exception e) {
            log.error("💥 [LongTermMemory] 向量检索异常，跳过长期记忆提取", e);
            return "";
        }
    }
}
