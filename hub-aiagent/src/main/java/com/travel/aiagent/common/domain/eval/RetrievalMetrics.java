package com.travel.aiagent.common.domain.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索质量指标
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalMetrics {

    /**
     * Recall@K：Top K 结果中包含了多少个正确文档
     * 范围：[0, 1]，越高越好
     */
    private double recallAtK;

    /**
     * Precision@K：Top K 结果中有多少是正确的
     * 范围：[0, 1]，越高越好
     */
    private double precisionAtK;

    /**
     * MRR（Mean Reciprocal Rank）：第一个正确结果的排名倒数
     * 范围：[0, 1]，越高越好
     */
    private double mrr;
}
