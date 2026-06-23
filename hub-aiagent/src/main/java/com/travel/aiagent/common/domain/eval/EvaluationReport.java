package com.travel.aiagent.common.domain.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评测报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationReport {

    /**
     * 报告 ID
     */
    private String reportId;

    /**
     * 评测时间
     */
    private LocalDateTime timestamp;

    /**
     * 测试用例总数
     */
    private int totalTestCases;

    /**
     * 知识库版本
     */
    private String knowledgeBaseVersion;

    // ==================== 汇总指标 ====================

    /**
     * 平均 Recall@K
     */
    private double avgRecallAtK;

    /**
     * 平均 Precision@K
     */
    private double avgPrecisionAtK;

    /**
     * 平均 MRR
     */
    private double avgMrr;

    /**
     * 平均 Relevance（相关性）
     */
    private double avgRelevance;

    /**
     * 平均 Faithfulness（忠实度）
     */
    private double avgFaithfulness;

    /**
     * 平均 Completeness（完整性）
     */
    private double avgCompleteness;

    // ==================== 分类统计 ====================

    /**
     * 按问题分类统计
     */
    private Map<String, CategoryMetrics> categoryMetrics;

    /**
     * 按难度等级统计
     */
    private Map<String, DifficultyMetrics> difficultyMetrics;

    // ==================== 详细结果 ====================

    /**
     * 所有评测结果
     */
    private List<EvaluationResult> detailedResults;

    /**
     * 失败用例分析
     */
    private List<FailureCase> failureCases;

    /**
     * 分类指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryMetrics {
        private String category;
        private int count;
        private double avgRecallAtK;
        private double avgRelevance;
    }

    /**
     * 难度指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyMetrics {
        private String difficulty;
        private int count;
        private double avgRecallAtK;
        private double avgRelevance;
    }

    /**
     * 失败用例
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureCase {
        private String testCaseId;
        private String question;
        private String failureType;  // retrieval_failed, low_relevance, low_faithfulness
        private String description;
    }
}
