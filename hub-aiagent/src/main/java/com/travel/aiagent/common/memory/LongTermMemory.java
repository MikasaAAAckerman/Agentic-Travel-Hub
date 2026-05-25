package com.travel.aiagent.common.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 🗄️ 长期记忆雷达服务 (LongTermVectorService)
 * 负责连接向量数据库（如 PGVector），利用 Embedding 检索用户的长期偏好喵！
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LongTermMemory {
/*
    // 💥 注入 Spring AI 的终极武器：向量存储仓库！
    // 只要你的 pom 里引入了 PGVector 或 Redis Vector 的 starter，Spring Boot 就会自动装配它！
    private final VectorStore vectorStore;

    *//**
     * 📥 录入长期偏好（比如用户突然说：“我其实对海鲜过敏”）
     *
     * @param userId         绝对不能少的用户 ID，防止把别人的偏好记混了喵！
     * @param preferenceText 偏好内容
     *//*
    public void saveUserPreference(String userId, String preferenceText) {
        log.info("💾 [Vector-Memory] 正在将笨蛋主人 {} 的偏好进行向量化存储：{}", userId, preferenceText);

        // 创建一个文档对象，必须带上 metadata（元数据），这是极其关键的隔离墙！
        Document document = new Document(
                preferenceText,
                // ⚠️ 极其致命的隔离：必须把 userId 作为元数据打在向量上！
                Map.of("userId", userId, "type", "user_preference")
        );

        // 丢给大模型去算 Embedding 并存入向量数据库
        vectorStore.add(List.of(document));
        log.info("✅ [Vector-Memory] 偏好向量化完毕并入库喵！");
    }

    *//**
     * 📤 检索长期偏好（RAG 核心逻辑）
     *
     * @param userId       当前正在对话的用户 ID
     * @param currentQuery 用户当前的问题（用来做相似度匹配的诱饵喵！）
     * @return 检索到的所有相关偏好的浓缩文本
     *//*
    public String recallPreferences(String userId, String currentQuery) {
        log.info("📡 [Vector-Memory] 雷达启动！正在为用户 {} 扫描与 '{}' 相关的长期记忆...", userId, currentQuery);

        try {
            // 发起一次极其精准的带过滤器的向量搜索 (Vector Similarity Search with Metadata Filter)
            List<Document> similarDocuments = vectorStore.similaritySearch(
                    SearchRequest.query(currentQuery)
                            .withTopK(3) // 只取最相关的 3 条记忆，别把 Token 撑爆了！
                            .withSimilarityThreshold(0.7) // 相似度门槛，低于 0.7 的垃圾记忆不要！
                            // 💥 绝对防御：只允许搜索属于这个 userId 的记忆！
                            .withFilterExpression("userId == '" + userId + "'")
            );

            if (similarDocuments.isEmpty()) {
                return ""; // 没找到相关偏好，干干净净地回去
            }

            // 把找到的 Document 组装成一段给大模型看的情报文本
            String recalledContext = similarDocuments.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("；"));

            log.info("🎯 [Vector-Memory] 雷达捕获到长期记忆：{}", recalledContext);
            return "【雷达检索到的该用户长期偏好】：\n" + recalledContext + "\n";

        } catch (Exception e) {
            log.error("💥 [Vector-Memory] 向量检索雷达故障啦！跳过长期记忆提取喵！", e);
            return ""; // 就算数据库挂了，也不能阻碍主流程运行！
        }
    }*/
}