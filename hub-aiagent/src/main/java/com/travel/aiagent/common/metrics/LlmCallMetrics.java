package com.travel.aiagent.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * LLM 调用 Prometheus 指标采集器（Layer 2：Metrics 可观测性）
 *
 * 指标列表：
 * - llm_call_total: LLM 调用总数（Counter）
 * - llm_call_errors_total: LLM 调用失败总数（Counter）
 * - llm_call_duration_seconds: LLM 调用耗时（Timer）
 * - llm_call_total_tokens: LLM 调用 Token 消耗（Counter）
 */
@Component
@Slf4j
public class LlmCallMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * LLM 调用总数
     */
    private final Counter llmCallTotal;

    /**
     * LLM 调用失败总数
     */
    private final Counter llmCallErrorsTotal;

    /**
     * LLM 调用耗时
     */
    private final Timer llmCallDuration;

    /**
     * LLM 调用 Token 消耗
     */
    private final Counter llmCallTotalTokens;

    public LlmCallMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化指标
        this.llmCallTotal = Counter.builder("llm_call_total")
                .description("LLM 调用总数")
                .register(meterRegistry);

        this.llmCallErrorsTotal = Counter.builder("llm_call_errors_total")
                .description("LLM 调用失败总数")
                .register(meterRegistry);

        this.llmCallDuration = Timer.builder("llm_call_duration_seconds")
                .description("LLM 调用耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.llmCallTotalTokens = Counter.builder("llm_call_total_tokens")
                .description("LLM 调用 Token 消耗")
                .register(meterRegistry);

        log.info("[Metrics] 初始化 LLM 调用指标采集器");
    }

    /**
     * 记录一次 LLM 调用
     *
     * @param callType   调用类型（PLANNER/WORKER/ROUTER）
     * @param agentName  Agent名称
     * @param modelName  模型名称（deepseek/qwen）
     * @param durationMs 调用耗时（毫秒）
     * @param success    是否成功
     * @param tokens     Token 消耗
     */
    public void recordCall(String callType, String agentName, String modelName,
                           long durationMs, boolean success, long tokens) {

        // 记录调用总数
        Counter.builder("llm_call_total")
                .tag("call_type", callType)
                .tag("agent_name", agentName)
                .tag("model_name", modelName)
                .register(meterRegistry)
                .increment();

        // 记录失败数
        if (!success) {
            Counter.builder("llm_call_errors_total")
                    .tag("call_type", callType)
                    .tag("agent_name", agentName)
                    .tag("model_name", modelName)
                    .register(meterRegistry)
                    .increment();
        }

        // 记录耗时
        Timer.builder("llm_call_duration_seconds")
                .tag("call_type", callType)
                .tag("agent_name", agentName)
                .tag("model_name", modelName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        // 记录 Token 消耗
        if (tokens > 0) {
            Counter.builder("llm_call_total_tokens")
                    .tag("call_type", callType)
                    .tag("agent_name", agentName)
                    .tag("model_name", modelName)
                    .register(meterRegistry)
                    .increment(tokens);
        }

        log.debug("[Metrics] 记录 LLM 调用 | callType={} | agentName={} | duration={}ms | success={} | tokens={}",
                callType, agentName, durationMs, success, tokens);
    }

    /**
     * 记录 Planner 调用
     */
    public void recordPlannerCall(String agentName, String modelName,
                                  long durationMs, boolean success, long tokens) {
        recordCall("PLANNER", agentName, modelName, durationMs, success, tokens);
    }

    /**
     * 记录 Worker 调用
     */
    public void recordWorkerCall(String agentName, String modelName,
                                 long durationMs, boolean success, long tokens) {
        recordCall("WORKER", agentName, modelName, durationMs, success, tokens);
    }

    /**
     * 记录 Router 调用
     */
    public void recordRouterCall(String agentName, String modelName,
                                 long durationMs, boolean success, long tokens) {
        recordCall("ROUTER", agentName, modelName, durationMs, success, tokens);
    }
}
