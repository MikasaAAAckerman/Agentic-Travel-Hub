package com.travel.aiagent.common.service;

import com.alibaba.fastjson.JSON;
import com.travel.aiagent.common.metrics.LlmCallMetrics;
import com.travel.aiagent.common.repository.LlmCallLogRepository;
import com.travel.aiagent.common.utils.AgentMDC;
import com.travel.common.domain.LlmCallLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * LLM 调用日志服务
 *
 * 封装 LLM 调用日志的记录和查询逻辑
 */
@Service
@Slf4j
public class LlmCallLogService {

    private final LlmCallLogRepository llmCallLogRepository;
    private final LlmCallMetrics llmCallMetrics;

    public LlmCallLogService(LlmCallLogRepository llmCallLogRepository, LlmCallMetrics llmCallMetrics) {
        this.llmCallLogRepository = llmCallLogRepository;
        this.llmCallMetrics = llmCallMetrics;
    }

    /**
     * 记录一次 LLM 调用
     *
     * @param callType      调用类型（PLANNER/WORKER/ROUTER/MEMORY/EVAL）
     * @param agentName     Agent名称
     * @param modelName     模型名称（deepseek/qwen）
     * @param version       架构版本（v0/v1/v2/v3）
     * @param systemPrompt  系统提示词
     * @param userInput     用户输入
     * @param historyContext 历史上下文
     * @param llmOutput     LLM原始输出
     * @param parsedOutput  解析后的结构化输出
     * @param toolCalls     工具调用列表
     * @param toolResults   工具执行结果
     * @param durationMs    调用耗时
     * @param success       是否成功
     * @param errorMessage  错误信息
     * @return 日志ID
     */
    public Long recordCall(String callType, String agentName, String modelName, String version,
                           String systemPrompt, String userInput, String historyContext,
                           String llmOutput, String parsedOutput,
                           String toolCalls, String toolResults,
                           Long durationMs, boolean success, String errorMessage) {

        String traceId = AgentMDC.getTraceId();

        LlmCallLog callLog = LlmCallLog.builder()
                .traceId(traceId != null ? traceId : "unknown")
                .callType(callType)
                .agentName(agentName)
                .modelName(modelName)
                .version(version)
                .systemPrompt(systemPrompt)
                .userInput(userInput)
                .historyContext(historyContext)
                .llmOutput(llmOutput)
                .parsedOutput(parsedOutput)
                .toolCalls(toolCalls)
                .toolResults(toolResults)
                .promptTokens(0)  // TODO: 从 ChatResponse 中获取
                .completionTokens(0)
                .totalTokens(0)
                .durationMs(durationMs)
                .success(success)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();

        llmCallLogRepository.insert(callLog);
        log.info("[LlmCallLog] 记录调用日志 | traceId={} | callType={} | agentName={} | duration={}ms | success={}",
                traceId, callType, agentName, durationMs, success);

        // 采集 Prometheus 指标
        try {
            llmCallMetrics.recordCall(callType, agentName, modelName, durationMs, success, 0);
        } catch (Exception e) {
            log.warn("[LlmCallLog] Prometheus 指标采集失败", e);
        }

        return callLog.getId();
    }

    /**
     * 记录 Planner 调用
     */
    public Long recordPlannerCall(String agentName, String version,
                                  String systemPrompt, String userInput, String historyContext,
                                  String llmOutput, Object parsedOutput,
                                  Long durationMs, boolean success, String errorMessage) {
        return recordCall(
                LlmCallLog.CallType.PLANNER,
                agentName,
                LlmCallLog.ModelName.DEEPSEEK,
                version,
                systemPrompt,
                userInput,
                historyContext,
                llmOutput,
                parsedOutput != null ? JSON.toJSONString(parsedOutput) : null,
                null,  // Planner 不涉及工具调用
                null,
                durationMs,
                success,
                errorMessage
        );
    }

    /**
     * 记录 Worker 调用
     */
    public Long recordWorkerCall(String agentName, String version,
                                 String systemPrompt, String userInput,
                                 String llmOutput, String toolCalls, String toolResults,
                                 Long durationMs, boolean success, String errorMessage) {
        return recordCall(
                LlmCallLog.CallType.WORKER,
                agentName,
                LlmCallLog.ModelName.QWEN,
                version,
                systemPrompt,
                userInput,
                null,  // Worker 不需要历史上下文
                llmOutput,
                null,
                toolCalls,
                toolResults,
                durationMs,
                success,
                errorMessage
        );
    }

    /**
     * 记录 Router 调用
     */
    public Long recordRouterCall(String agentName, String userInput, String llmOutput,
                                 Long durationMs, boolean success, String errorMessage) {
        return recordCall(
                LlmCallLog.CallType.ROUTER,
                agentName,
                LlmCallLog.ModelName.QWEN,
                "v3",  // Router 固定 v3
                null,
                userInput,
                null,
                llmOutput,
                null,
                null,
                null,
                durationMs,
                success,
                errorMessage
        );
    }

    // ========== 查询方法 ==========

    /**
     * 按 traceId 查询完整调用链
     */
    public List<LlmCallLog> getCallChain(String traceId) {
        return llmCallLogRepository.findByTraceId(traceId);
    }

    /**
     * 查询最近的调用记录
     */
    public List<LlmCallLog> getRecentCalls(int limit) {
        return llmCallLogRepository.findRecent(limit);
    }

    /**
     * 查询失败的调用
     */
    public List<LlmCallLog> getFailedCalls() {
        return llmCallLogRepository.findFailures();
    }

    /**
     * 按调用类型查询
     */
    public List<LlmCallLog> getCallsByType(String callType) {
        return llmCallLogRepository.findByCallType(callType);
    }

    /**
     * 按Agent名称查询
     */
    public List<LlmCallLog> getCallsByAgent(String agentName) {
        return llmCallLogRepository.findByAgentName(agentName);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        long plannerCount = llmCallLogRepository.countByCallType(LlmCallLog.CallType.PLANNER);
        long workerCount = llmCallLogRepository.countByCallType(LlmCallLog.CallType.WORKER);
        long routerCount = llmCallLogRepository.countByCallType(LlmCallLog.CallType.ROUTER);
        long totalTokens = llmCallLogRepository.sumTotalTokens();
        double avgPlannerDuration = llmCallLogRepository.avgDurationByCallType(LlmCallLog.CallType.PLANNER);
        double avgWorkerDuration = llmCallLogRepository.avgDurationByCallType(LlmCallLog.CallType.WORKER);

        return Map.of(
                "plannerCallCount", plannerCount,
                "workerCallCount", workerCount,
                "routerCallCount", routerCount,
                "totalCallCount", plannerCount + workerCount + routerCount,
                "totalTokens", totalTokens,
                "avgPlannerDurationMs", Math.round(avgPlannerDuration),
                "avgWorkerDurationMs", Math.round(avgWorkerDuration)
        );
    }
}
