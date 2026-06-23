package com.travel.starter.eval;

import com.travel.aiagent.common.domain.eval.EvaluationReport;
import com.travel.aiagent.common.domain.eval.EvaluationResult;
import com.travel.aiagent.common.domain.eval.QaTestCase;
import com.travel.aiagent.common.service.RagEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 🧪 RAG 问答质量评测测试
 *
 * 演示如何使用评测服务评估 RAG 系统的质量：
 * 1. 单条评测：评测单个问题
 * 2. 批量评测：评测整个数据集
 * 3. 版本对比：对比不同版本的评测结果
 *
 * 运行前确保：
 * 1. PG 容器已启动
 * 2. 知识库文档已导入（运行 VectorStoreImportTest）
 * 3. application-dev.yml 配置正确
 */
@Slf4j
@SpringBootTest
public class RagEvaluationTest {

    @Autowired
    private RagEvaluationService evaluationService;

    /**
     * 测试1：单条评测
     *
     * 评测单个问题的 RAG 效果
     */
    @Test
    void testSingleEvaluation() {
        // 准备测试用例
        QaTestCase testCase = QaTestCase.builder()
                .id("qa_001")
                .question("三亚有什么好玩的景点？")
                .expectedToolBeans(List.of("AttractionAndEntertainmentTool"))
                .expectedDocIds(List.of("subculturePoiRadarTool_v1.0"))
                .groundTruth("三亚有亚龙湾、天涯海角、南山寺等著名景点")
                .difficulty("easy")
                .category("景点查询")
                .build();

        // 执行评测（版本 1.0）
        EvaluationResult result = evaluationService.evaluate(testCase, "1.0");

        // 打印结果
        log.info("========================================");
        log.info("📊 单条评测结果");
        log.info("========================================");
        log.info("问题：{}", result.getQuestion());
        log.info("检索到的工具：{}", result.getRetrievedToolBeans());
        log.info("检索到的文档数：{}", result.getRetrievedDocIds().size());
        log.info("----------------------------------------");
        log.info("检索质量指标：");
        log.info("  Recall@5：{:.2f}", result.getRetrievalMetrics().getRecallAtK());
        log.info("  Precision@5：{:.2f}", result.getRetrievalMetrics().getPrecisionAtK());
        log.info("  MRR：{:.2f}", result.getRetrievalMetrics().getMrr());
        log.info("----------------------------------------");
        log.info("生成质量指标（LLM-as-Judge）：");
        log.info("  Relevance：{}/5", result.getGenerationMetrics().getRelevance());
        log.info("  Faithfulness：{}/5", result.getGenerationMetrics().getFaithfulness());
        log.info("  Completeness：{}/5", result.getGenerationMetrics().getCompleteness());
        log.info("----------------------------------------");
        log.info("生成的答案：{}", result.getGeneratedAnswer());
        log.info("========================================");
    }

    /**
     * 测试2：批量评测
     *
     * 评测整个数据集，生成评测报告
     */
    @Test
    void testBatchEvaluation() {
        // 获取评测数据集
        List<QaTestCase> testCases = QaTestDataset.getTestCases();

        log.info("🚀 开始批量评测 | 测试用例数={} | 版本=1.0", testCases.size());

        // 执行批量评测
        EvaluationReport report = evaluationService.evaluateBatch(testCases, "1.0");

        // 打印报告
        printReport(report);
    }

    /**
     * 测试3：版本对比评测
     *
     * 对比不同版本的评测结果，验证版本管理的效果
     */
    @Test
    void testVersionComparison() {
        List<QaTestCase> testCases = QaTestDataset.getTestCases();
        String[] versions = {"1.0", "1.1"};

        log.info("🔍 版本对比评测");
        log.info("========================================");

        for (String version : versions) {
            log.info("📋 评测版本：{}", version);
            EvaluationReport report = evaluationService.evaluateBatch(testCases, version);

            log.info("  平均 Recall@5：{:.2f}", report.getAvgRecallAtK());
            log.info("  平均 Relevance：{:.2f}", report.getAvgRelevance());
            log.info("  平均 Faithfulness：{:.2f}", report.getAvgFaithfulness());
            log.info("----------------------------------------");
        }

        log.info("✅ 版本对比完成！请查看上方数据，选择最优版本");
    }

    /**
     * 打印评测报告
     */
    private void printReport(EvaluationReport report) {
        log.info("========================================");
        log.info("📊 评测报告");
        log.info("========================================");
        log.info("报告 ID：{}", report.getReportId());
        log.info("评测时间：{}", report.getTimestamp());
        log.info("知识库版本：{}", report.getKnowledgeBaseVersion());
        log.info("测试用例数：{}", report.getTotalTestCases());
        log.info("----------------------------------------");
        log.info("汇总指标：");
        log.info("  平均 Recall@5：{:.2f}", report.getAvgRecallAtK());
        log.info("  平均 Precision@5：{:.2f}", report.getAvgPrecisionAtK());
        log.info("  平均 MRR：{:.2f}", report.getAvgMrr());
        log.info("  平均 Relevance：{:.2f}", report.getAvgRelevance());
        log.info("  平均 Faithfulness：{:.2f}", report.getAvgFaithfulness());
        log.info("  平均 Completeness：{:.2f}", report.getAvgCompleteness());
        log.info("----------------------------------------");

        // 打印失败用例
        if (report.getFailureCases() != null && !report.getFailureCases().isEmpty()) {
            log.info("❌ 失败用例分析：");
            for (EvaluationReport.FailureCase failure : report.getFailureCases()) {
                log.info("  [{}] {} - {}", failure.getTestCaseId(), failure.getQuestion(), failure.getFailureType());
                log.info("      {}", failure.getDescription());
            }
        } else {
            log.info("✅ 没有失败用例");
        }

        log.info("========================================");
    }
}
