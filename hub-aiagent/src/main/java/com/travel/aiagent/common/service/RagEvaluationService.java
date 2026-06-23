package com.travel.aiagent.common.service;

import com.travel.aiagent.common.domain.eval.*;
import com.travel.aiagent.common.utils.SpringAIDocumentUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 问答质量评测服务
 *
 * 提供 Recall@K、Relevance、Faithfulness 等指标的评测能力
 */
@Service
@Slf4j
public class RagEvaluationService {

    private final VectorStore vectorStore;
    private final ChatClient evaluationClient;

    /**
     * 评测用的 Top K
     */
    private static final int EVAL_TOP_K = 5;

    /**
     * 评测用的相似度阈值
     */
    private static final double EVAL_SIMILARITY_THRESHOLD = 0.3;

    public RagEvaluationService(VectorStore vectorStore,
                                 ChatClient qwenChatClient) {
        this.vectorStore = vectorStore;
        this.evaluationClient = qwenChatClient;
        log.info("[Eval] 初始化 RAG 评测服务");
    }

    /**
     * 执行单条评测
     *
     * @param testCase 测试用例
     * @param version  知识库版本
     * @return 评测结果
     */
    public EvaluationResult evaluate(QaTestCase testCase, String version) {
        log.info("[Eval] 开始评测 | id={} | question={} | version={}", testCase.getId(), testCase.getQuestion(), version);

        // 1. 执行 RAG 检索
        List<Document> retrievedDocs = retrieveWithVersion(testCase.getQuestion(), version);
        List<String> retrievedDocIds = retrievedDocs.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        // 2. 提取工具名称
        List<String> retrievedToolBeans = SpringAIDocumentUtils.getToolBeanList(retrievedDocs, SpringAIDocumentUtils.TOOL_NAME_PATTERN);

        // 3. 计算检索质量指标
        RetrievalMetrics retrievalMetrics = calculateRetrievalMetrics(retrievedDocIds, testCase.getExpectedDocIds());

        // 4. 生成答案（简化版：直接使用检索结果作为答案上下文）
        String generatedAnswer = generateAnswer(testCase.getQuestion(), retrievedDocs);

        // 5. 计算生成质量指标（LLM-as-Judge）
        GenerationMetrics generationMetrics = evaluateGeneration(testCase.getQuestion(), generatedAnswer, retrievedDocs);

        // 6. 构建评测结果
        EvaluationResult result = EvaluationResult.builder()
                .testCaseId(testCase.getId())
                .question(testCase.getQuestion())
                .retrievedDocIds(retrievedDocIds)
                .retrievedToolBeans(retrievedToolBeans)
                .generatedAnswer(generatedAnswer)
                .retrievalMetrics(retrievalMetrics)
                .generationMetrics(generationMetrics)
                .timestamp(LocalDateTime.now())
                .build();

        log.info("[Eval] 评测完成 | id={} | Recall@{}={} | Relevance={}",
                testCase.getId(), EVAL_TOP_K, retrievalMetrics.getRecallAtK(), generationMetrics.getRelevance());

        return result;
    }

    /**
     * 批量评测
     *
     * @param testCases 测试用例列表
     * @param version   知识库版本
     * @return 评测报告
     */
    public EvaluationReport evaluateBatch(List<QaTestCase> testCases, String version) {
        log.info("[Eval] 开始批量评测 | count={} | version={}", testCases.size(), version);

        List<EvaluationResult> results = new ArrayList<>();
        for (QaTestCase testCase : testCases) {
            try {
                EvaluationResult result = evaluate(testCase, version);
                results.add(result);
            } catch (Exception e) {
                log.error("[Eval] 评测失败 | id={}", testCase.getId(), e);
            }
        }

        return generateReport(results, version);
    }

    /**
     * 检索文档（带版本过滤）
     */
    private List<Document> retrieveWithVersion(String queryText, String version) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(queryText)
                .topK(EVAL_TOP_K)
                .similarityThreshold(EVAL_SIMILARITY_THRESHOLD)
                .filterExpression("version == '" + version + "'")
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * 计算检索质量指标
     */
    private RetrievalMetrics calculateRetrievalMetrics(List<String> retrievedDocIds, List<String> expectedDocIds) {
        if (expectedDocIds == null || expectedDocIds.isEmpty()) {
            return RetrievalMetrics.builder()
                    .recallAtK(1.0)
                    .precisionAtK(1.0)
                    .mrr(1.0)
                    .build();
        }

        Set<String> expectedSet = new HashSet<>(expectedDocIds);
        Set<String> retrievedSet = new HashSet<>(retrievedDocIds);

        // Recall@K：命中了多少个期望文档
        long hitCount = expectedSet.stream()
                .filter(retrievedSet::contains)
                .count();
        double recall = (double) hitCount / expectedSet.size();

        // Precision@K：检索结果中有多少是正确的
        long relevantInRetrieved = retrievedSet.stream()
                .filter(expectedSet::contains)
                .count();
        double precision = retrievedDocIds.isEmpty() ? 0 : (double) relevantInRetrieved / retrievedDocIds.size();

        // MRR：第一个正确结果的排名倒数
        double mrr = 0.0;
        for (int i = 0; i < retrievedDocIds.size(); i++) {
            if (expectedSet.contains(retrievedDocIds.get(i))) {
                mrr = 1.0 / (i + 1);
                break;
            }
        }

        return RetrievalMetrics.builder()
                .recallAtK(recall)
                .precisionAtK(precision)
                .mrr(mrr)
                .build();
    }

    /**
     * 生成答案（简化版）
     */
    private String generateAnswer(String question, List<Document> docs) {
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        String prompt = String.format("""
                基于以下检索到的文档，回答用户的问题。

                【用户问题】
                %s

                【检索到的文档】
                %s

                请用简洁的中文回答问题，如果文档中没有相关信息，请说明。
                """, question, context);

        try {
            return evaluationClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[Eval] 生成答案失败", e);
            return "生成答案失败: " + e.getMessage();
        }
    }

    /**
     * 使用 LLM-as-Judge 评测生成质量
     */
    private GenerationMetrics evaluateGeneration(String question, String answer, List<Document> context) {
        String contextText = context.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        String judgePrompt = String.format("""
                请作为评测专家，评估以下 RAG 系统的生成质量。

                【问题】
                %s

                【检索到的文档】
                %s

                【生成的答案】
                %s

                请从以下三个维度评分（1-5 分）：

                1. **Relevance（相关性）**：答案与问题的相关程度
                   - 5 = 完全相关，直接回答问题
                   - 4 = 高度相关
                   - 3 = 部分相关
                   - 2 = 弱相关
                   - 1 = 完全不相关

                2. **Faithfulness（忠实度）**：答案是否忠实于检索到的文档
                   - 5 = 所有事实声明都有文档支持
                   - 4 = 大部分有文档支持
                   - 3 = 部分有文档支持
                   - 2 = 少部分有文档支持
                   - 1 = 无法在文档中找到依据

                3. **Completeness（完整性）**：答案是否完整回答了问题
                   - 5 = 非常完整，包含所有必要信息
                   - 4 = 比较完整
                   - 3 = 基本完整
                   - 2 = 不够完整
                   - 1 = 非常不完整

                请以 JSON 格式输出：
                {"relevance": 5, "faithfulness": 4, "completeness": 5, "reasoning": "..."}
                """, question, contextText, answer);

        try {
            String judgeResponse = evaluationClient.prompt()
                    .user(judgePrompt)
                    .call()
                    .content();

            return parseJudgeResponse(judgeResponse);
        } catch (Exception e) {
            log.error("[Eval] LLM-as-Judge 评测失败", e);
            return GenerationMetrics.builder()
                    .relevance(3)
                    .faithfulness(3)
                    .completeness(3)
                    .reasoning("评测失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析 LLM-as-Judge 的响应
     */
    private GenerationMetrics parseJudgeResponse(String response) {
        try {
            // 简单的 JSON 解析（实际项目中应该用 Jackson）
            int relevance = extractScore(response, "relevance");
            int faithfulness = extractScore(response, "faithfulness");
            int completeness = extractScore(response, "completeness");

            return GenerationMetrics.builder()
                    .relevance(relevance)
                    .faithfulness(faithfulness)
                    .completeness(completeness)
                    .reasoning(response)
                    .build();
        } catch (Exception e) {
            log.warn("[Eval] 解析评分响应失败，使用默认值", e);
            return GenerationMetrics.builder()
                    .relevance(3)
                    .faithfulness(3)
                    .completeness(3)
                    .reasoning("解析失败: " + response)
                    .build();
        }
    }

    /**
     * 从 JSON 字符串中提取分数
     */
    private int extractScore(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 3; // 默认值
    }

    /**
     * 生成评测报告
     */
    private EvaluationReport generateReport(List<EvaluationResult> results, String version) {
        if (results.isEmpty()) {
            return EvaluationReport.builder()
                    .reportId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .knowledgeBaseVersion(version)
                    .totalTestCases(0)
                    .build();
        }

        // 计算汇总指标
        double avgRecallAtK = results.stream()
                .mapToDouble(r -> r.getRetrievalMetrics().getRecallAtK())
                .average()
                .orElse(0);

        double avgPrecisionAtK = results.stream()
                .mapToDouble(r -> r.getRetrievalMetrics().getPrecisionAtK())
                .average()
                .orElse(0);

        double avgMrr = results.stream()
                .mapToDouble(r -> r.getRetrievalMetrics().getMrr())
                .average()
                .orElse(0);

        double avgRelevance = results.stream()
                .mapToDouble(r -> r.getGenerationMetrics().getRelevance())
                .average()
                .orElse(0);

        double avgFaithfulness = results.stream()
                .mapToDouble(r -> r.getGenerationMetrics().getFaithfulness())
                .average()
                .orElse(0);

        double avgCompleteness = results.stream()
                .mapToDouble(r -> r.getGenerationMetrics().getCompleteness())
                .average()
                .orElse(0);

        // 识别失败用例
        List<EvaluationReport.FailureCase> failureCases = results.stream()
                .filter(r -> r.getRetrievalMetrics().getRecallAtK() < 0.5 ||
                        r.getGenerationMetrics().getRelevance() < 3)
                .map(r -> EvaluationReport.FailureCase.builder()
                        .testCaseId(r.getTestCaseId())
                        .question(r.getQuestion())
                        .failureType(r.getRetrievalMetrics().getRecallAtK() < 0.5 ? "retrieval_failed" : "low_relevance")
                        .description(String.format("Recall@%d=%.2f, Relevance=%d",
                                EVAL_TOP_K, r.getRetrievalMetrics().getRecallAtK(), r.getGenerationMetrics().getRelevance()))
                        .build())
                .collect(Collectors.toList());

        return EvaluationReport.builder()
                .reportId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .knowledgeBaseVersion(version)
                .totalTestCases(results.size())
                .avgRecallAtK(avgRecallAtK)
                .avgPrecisionAtK(avgPrecisionAtK)
                .avgMrr(avgMrr)
                .avgRelevance(avgRelevance)
                .avgFaithfulness(avgFaithfulness)
                .avgCompleteness(avgCompleteness)
                .detailedResults(results)
                .failureCases(failureCases)
                .build();
    }
}
