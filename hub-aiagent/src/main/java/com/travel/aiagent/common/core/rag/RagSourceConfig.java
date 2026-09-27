package com.travel.aiagent.common.core.rag;

/**
 * 多路召回「检索源」配置。
 *
 * <p>把「查哪个库、怎么查、取多少」从检索引擎里参数化出来——同一套
 * {@link HybridRagRetriever} 引擎，靠不同的 RagSourceConfig 服务不同知识库：
 * <pre>
 *   工具知识库 → filterExpression = type == 'tool_knowledge'，ES 索引 travel_hub_tools
 *   PDF 文本库 → filterExpression = type == 'pdf_knowledge'， ES 索引 travel_pdf_chunks
 * </pre>
 *
 * @param name                源名称（日志用）
 * @param filterExpression    PGVector 元数据过滤表达式（null = 不过滤）
 * @param esIndex             ES 索引名（BM25 稀疏召回）
 * @param denseTopK           向量召回候选数
 * @param sparseTopK          BM25 召回候选数
 * @param finalTopK           合并去重 + 重排后最终返回条数
 * @param similarityThreshold 向量相似度阈值（低于此值被过滤）
 */
public record RagSourceConfig(
        String name,
        String filterExpression,
        String esIndex,
        int denseTopK,
        int sparseTopK,
        int finalTopK,
        double similarityThreshold
) {
}
