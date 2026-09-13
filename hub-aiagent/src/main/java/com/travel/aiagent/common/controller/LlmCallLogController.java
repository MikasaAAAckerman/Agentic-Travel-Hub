package com.travel.aiagent.common.controller;

import com.travel.aiagent.common.service.LlmCallLogService;
import com.travel.common.domain.LlmCallLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * LLM 调用日志 Controller
 *
 * 提供查询 LLM 调用日志的 API 接口
 */
@RestController
@RequestMapping("/api/llm-logs")
public class LlmCallLogController {

    private final LlmCallLogService llmCallLogService;

    public LlmCallLogController(LlmCallLogService llmCallLogService) {
        this.llmCallLogService = llmCallLogService;
    }

    /**
     * 按 traceId 查询完整调用链
     *
     * @param traceId 链路追踪ID
     * @return 调用日志列表
     */
    @GetMapping("/trace/{traceId}")
    public List<LlmCallLog> getCallChain(@PathVariable String traceId) {
        return llmCallLogService.getCallChain(traceId);
    }

    /**
     * 查询最近的调用记录
     *
     * @param limit 数量限制，默认 50
     * @return 调用日志列表
     */
    @GetMapping("/recent")
    public List<LlmCallLog> getRecentCalls(@RequestParam(defaultValue = "50") int limit) {
        return llmCallLogService.getRecentCalls(limit);
    }

    /**
     * 查询失败的调用
     *
     * @return 失败的调用日志列表
     */
    @GetMapping("/failures")
    public List<LlmCallLog> getFailedCalls() {
        return llmCallLogService.getFailedCalls();
    }

    /**
     * 按调用类型查询
     *
     * @param callType 调用类型（PLANNER/WORKER/ROUTER）
     * @return 调用日志列表
     */
    @GetMapping("/type/{callType}")
    public List<LlmCallLog> getCallsByType(@PathVariable String callType) {
        return llmCallLogService.getCallsByType(callType);
    }

    /**
     * 按Agent名称查询
     *
     * @param agentName Agent名称
     * @return 调用日志列表
     */
    @GetMapping("/agent/{agentName}")
    public List<LlmCallLog> getCallsByAgent(@PathVariable String agentName) {
        return llmCallLogService.getCallsByAgent(agentName);
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getStatistics() {
        return llmCallLogService.getStatistics();
    }
}
