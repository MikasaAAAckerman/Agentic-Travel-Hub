package com.travel.aiagent.common.domain.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成质量指标（LLM-as-Judge 评分）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationMetrics {

    /**
     * Relevance（相关性）：答案与问题的相关程度
     * 范围：1-5 分
     * 5 = 完全相关，直接回答问题
     * 4 = 高度相关
     * 3 = 部分相关
     * 2 = 弱相关
     * 1 = 完全不相关
     */
    private int relevance;

    /**
     * Faithfulness（忠实度）：答案是否忠实于检索到的文档
     * 范围：1-5 分
     * 5 = 所有事实声明都有文档支持
     * 4 = 大部分有文档支持
     * 3 = 部分有文档支持
     * 2 = 少部分有文档支持
     * 1 = 无法在文档中找到依据
     */
    private int faithfulness;

    /**
     * Completeness（完整性）：答案是否完整回答了问题
     * 范围：1-5 分
     * 5 = 非常完整，包含所有必要信息
     * 4 = 比较完整
     * 3 = 基本完整
     * 2 = 不够完整
     * 1 = 非常不完整
     */
    private int completeness;

    /**
     * LLM 评分理由
     */
    private String reasoning;
}
