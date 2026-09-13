package com.travel.aiagent.common.domain.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单条评测结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    /**
     * 测试用例 ID
     */
    private String testCaseId;

    /**
     * 用户问题
     */
    private String question;

    /**
     * RAG 检索到的文档 ID 列表
     */
    private List<String> retrievedDocIds;

    /**
     * RAG 检索到的工具名称列表
     */
    private List<String> retrievedToolBeans;

    /**
     * RAG 生成的答案
     */
    private String generatedAnswer;

    /**
     * 检索质量指标
     */
    private RetrievalMetrics retrievalMetrics;

    /**
     * 生成质量指标
     */
    private GenerationMetrics generationMetrics;

    /**
     * 评测时间
     */
    private LocalDateTime timestamp;
}
