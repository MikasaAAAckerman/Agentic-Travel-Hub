package com.travel.aiagent.common.domain.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评测用例：标准问答对
 *
 * 用于 RAG 系统的质量评测，包含问题、期望检索的文档、期望答案等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaTestCase {

    /**
     * 用例 ID
     */
    private String id;

    /**
     * 用户问题
     */
    private String question;

    /**
     * 期望检索到的工具 Bean 名称列表
     */
    private List<String> expectedToolBeans;

    /**
     * 期望的检索文档 ID 列表（用于计算 Recall@K）
     */
    private List<String> expectedDocIds;

    /**
     * 标准答案（Ground Truth，用于计算 Faithfulness）
     */
    private String groundTruth;

    /**
     * 难度等级：easy, medium, hard
     */
    private String difficulty;

    /**
     * 问题分类：景点查询、交通规划、酒店预订等
     */
    private String category;
}
