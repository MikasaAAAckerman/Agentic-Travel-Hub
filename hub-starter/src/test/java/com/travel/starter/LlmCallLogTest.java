package com.travel.starter;

import com.travel.aiagent.common.controller.LlmCallLogController;
import com.travel.aiagent.common.service.LlmCallLogService;
import com.travel.common.domain.LlmCallLog;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * LLM 调用日志测试
 */
@SpringBootTest
@Slf4j
public class LlmCallLogTest {

    @Autowired
    private LlmCallLogService llmCallLogService;

    @Autowired
    private LlmCallLogController llmCallLogController;

    /**
     * 测试手动记录一条 Planner 调用
     */
    @Test
    public void testRecordPlannerCall() {
        log.info("=== 测试记录 Planner 调用 ===");

        Long id = llmCallLogService.recordPlannerCall(
                "OrchestratorGraphAgent",  // agentName
                "v3",                      // version
                "你是一个旅行规划助手...",    // systemPrompt
                "我想去三亚玩3天",           // userInput
                "用户之前问过酒店信息",       // historyContext
                "{\"action\":\"TASK_DISPATCH\",\"subAgentName\":\"FlightExpert\"}",  // llmOutput
                null,                      // parsedOutput
                1500L,                     // durationMs
                true,                      // success
                null                       // errorMessage
        );

        log.info("✅ 记录成功 | id={}", id);
    }

    /**
     * 测试手动记录一条 Worker 调用
     */
    @Test
    public void testRecordWorkerCall() {
        log.info("=== 测试记录 Worker 调用 ===");

        Long id = llmCallLogService.recordWorkerCall(
                "FlightExpert",            // agentName
                "v3",                      // version
                "你是负责执行工具调用的 Worker...",  // systemPrompt
                "查询三亚的航班信息",         // userInput
                "已为您查询到三亚的航班信息...",  // llmOutput
                "[\"flightSearchTool\"]",  // toolCalls
                "[{\"tool\":\"flightSearchTool\",\"result\":\"...\"}]",  // toolResults
                3000L,                     // durationMs
                true,                      // success
                null                       // errorMessage
        );

        log.info("✅ 记录成功 | id={}", id);
    }

    /**
     * 测试查询最近的调用记录
     */
    @Test
    public void testGetRecentCalls() {
        log.info("=== 测试查询最近的调用记录 ===");

        List<LlmCallLog> recentCalls = llmCallLogService.getRecentCalls(10);
        log.info("查询到 {} 条记录", recentCalls.size());

        for (LlmCallLog callLog : recentCalls) {
            log.info("  - id={} | callType={} | agentName={} | duration={}ms | success={}",
                    callLog.getId(),
                    callLog.getCallType(),
                    callLog.getAgentName(),
                    callLog.getDurationMs(),
                    callLog.getSuccess());
        }
    }

    /**
     * 测试查询失败的调用
     */
    @Test
    public void testGetFailedCalls() {
        log.info("=== 测试查询失败的调用 ===");

        List<LlmCallLog> failedCalls = llmCallLogService.getFailedCalls();
        log.info("查询到 {} 条失败记录", failedCalls.size());

        for (LlmCallLog callLog : failedCalls) {
            log.info("  - id={} | callType={} | agentName={} | errorMessage={}",
                    callLog.getId(),
                    callLog.getCallType(),
                    callLog.getAgentName(),
                    callLog.getErrorMessage());
        }
    }

    /**
     * 测试按调用类型查询
     */
    @Test
    public void testGetCallsByType() {
        log.info("=== 测试按调用类型查询 ===");

        List<LlmCallLog> plannerCalls = llmCallLogService.getCallsByType("PLANNER");
        log.info("PLANNER 调用：{} 条", plannerCalls.size());

        List<LlmCallLog> workerCalls = llmCallLogService.getCallsByType("WORKER");
        log.info("WORKER 调用：{} 条", workerCalls.size());
    }

    /**
     * 测试按Agent名称查询
     */
    @Test
    public void testGetCallsByAgent() {
        log.info("=== 测试按Agent名称查询 ===");

        List<LlmCallLog> orchestratorCalls = llmCallLogService.getCallsByAgent("OrchestratorGraphAgent");
        log.info("OrchestratorGraphAgent 调用：{} 条", orchestratorCalls.size());

        List<LlmCallLog> flightCalls = llmCallLogService.getCallsByAgent("FlightExpert");
        log.info("FlightExpert 调用：{} 条", flightCalls.size());
    }

    /**
     * 测试获取统计信息
     */
    @Test
    public void testGetStatistics() {
        log.info("=== 测试获取统计信息 ===");

        Map<String, Object> stats = llmCallLogService.getStatistics();
        log.info("统计信息：{}", stats);
    }

    /**
     * 测试 Controller 接口
     */
    @Test
    public void testController() {
        log.info("=== 测试 Controller 接口 ===");

        // 测试 /api/llm-logs/recent
        List<LlmCallLog> recentCalls = llmCallLogController.getRecentCalls(5);
        log.info("GET /api/llm-logs/recent → {} 条", recentCalls.size());

        // 测试 /api/llm-logs/stats
        Map<String, Object> stats = llmCallLogController.getStatistics();
        log.info("GET /api/llm-logs/stats → {}", stats);

        // 测试 /api/llm-logs/type/PLANNER
        List<LlmCallLog> plannerCalls = llmCallLogController.getCallsByType("PLANNER");
        log.info("GET /api/llm-logs/type/PLANNER → {} 条", plannerCalls.size());
    }
}
