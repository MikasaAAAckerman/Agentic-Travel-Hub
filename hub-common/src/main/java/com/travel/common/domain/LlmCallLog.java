package com.travel.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM 调用日志实体
 *
 * 记录每次 Planner / Worker / Router 调用 LLM 的入参与出参
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmCallLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 链路追踪ID，关联 Jaeger Trace
     */
    private String traceId;

    /**
     * 调用类型：PLANNER / WORKER / ROUTER / MEMORY / EVAL
     */
    private String callType;

    /**
     * Agent名称：OrchestratorGraphAgent / FlightExpert 等
     */
    private String agentName;

    /**
     * 模型名称：deepseek / qwen
     */
    private String modelName;

    /**
     * 架构版本：v0 / v1 / v2 / v3
     */
    private String version;

    // ========== 输入信息 ==========

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 用户输入 / Planner输出
     */
    private String userInput;

    /**
     * 历史上下文
     */
    private String historyContext;

    // ========== 输出信息 ==========

    /**
     * LLM原始输出
     */
    private String llmOutput;

    /**
     * 解析后的结构化输出（JSON）
     */
    private String parsedOutput;

    // ========== 工具调用信息 ==========

    /**
     * 工具调用列表（JSON数组）
     * 格式：[{"name":"flightSearch","args":"{...}"}]
     */
    private String toolCalls;

    /**
     * 工具执行结果（JSON数组）
     * 格式：[{"name":"flightSearch","result":"..."}]
     */
    private String toolResults;

    // ========== Token 统计 ==========

    /**
     * 输入Token数
     */
    private Integer promptTokens;

    /**
     * 输出Token数
     */
    private Integer completionTokens;

    /**
     * 总Token数
     */
    private Integer totalTokens;

    // ========== 性能指标 ==========

    /**
     * 调用耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    // ========== 时间戳 ==========

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // ========== 枚举常量 ==========

    /**
     * 调用类型枚举
     */
    public static class CallType {
        public static final String PLANNER = "PLANNER";
        public static final String WORKER = "WORKER";
        public static final String ROUTER = "ROUTER";
        public static final String MEMORY = "MEMORY";
        public static final String EVAL = "EVAL";
    }

    /**
     * 模型名称枚举
     */
    public static class ModelName {
        public static final String DEEPSEEK = "deepseek";
        public static final String QWEN = "qwen";
    }
}
