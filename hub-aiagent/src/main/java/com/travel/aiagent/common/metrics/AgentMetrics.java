package com.travel.aiagent.common.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 指标组件
 *
 * 定义并记录所有 Agent 相关的 Prometheus 指标：
 * - Counter: 累计计数（调用次数、错误次数）
 * - Timer: 耗时统计（Agent/Planner/Worker 执行时间）
 * - Gauge: 瞬时值（当前活跃 Agent 数）
 */
@Component
@Getter
public class AgentMetrics {

    // ==================== Counter 指标 ====================

    /** Agent 调用总次数 */
    private final Counter agentInvokeTotal;

    /** Agent 重试总次数 */
    private final Counter agentRetryTotal;

    /** 工具调用总次数 */
    private final Counter toolCallTotal;

    /** 工具调用失败次数 */
    private final Counter toolErrorTotal;

    /** CLARIFY 事件次数 */
    private final Counter clarifyTotal;

    // ==================== Timer 指标 ====================

    /** Agent 执行耗时 */
    private final Timer agentDuration;

    /** Planner 执行耗时 */
    private final Timer plannerDuration;

    /** Worker 执行耗时 */
    private final Timer workerDuration;

    /** Orchestrator 单轮耗时 */
    private final Timer orchestratorRoundDuration;

    // ==================== Gauge 指标 ====================

    /** 当前活跃 Agent 数量 */
    private final AtomicInteger activeAgentCount;

    /** 当前 Orchestrator 轮次 */
    private final AtomicInteger currentOrchestratorRound;

    // ==================== 构造函数 ====================

    public AgentMetrics(MeterRegistry registry) {

        // Counter
        this.agentInvokeTotal = Counter.builder("agent.invoke.total")
                .description("Agent 调用总次数")
                .register(registry);

        this.agentRetryTotal = Counter.builder("agent.retry.total")
                .description("Agent 重试总次数")
                .register(registry);

        this.toolCallTotal = Counter.builder("tool.call.total")
                .description("工具调用总次数")
                .register(registry);

        this.toolErrorTotal = Counter.builder("tool.error.total")
                .description("工具调用失败次数")
                .register(registry);

        this.clarifyTotal = Counter.builder("agent.clarify.total")
                .description("CLARIFY 事件次数")
                .register(registry);

        // Timer
        this.agentDuration = Timer.builder("agent.duration")
                .description("Agent 执行耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        this.plannerDuration = Timer.builder("planner.duration")
                .description("Planner 执行耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        this.workerDuration = Timer.builder("worker.duration")
                .description("Worker 执行耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        this.orchestratorRoundDuration = Timer.builder("orchestrator.round.duration")
                .description("Orchestrator 单轮耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Gauge
        this.activeAgentCount = new AtomicInteger(0);
        Gauge.builder("agent.active.count", activeAgentCount, AtomicInteger::get)
                .description("当前活跃 Agent 数量")
                .register(registry);

        this.currentOrchestratorRound = new AtomicInteger(0);
        Gauge.builder("orchestrator.current.round", currentOrchestratorRound, AtomicInteger::get)
                .description("当前 Orchestrator 轮次")
                .register(registry);
    }

    // ==================== 便捷方法 ====================

    /** 记录 Agent 调用 */
    public void recordAgentInvoke(String agentName) {
        agentInvokeTotal.increment();
        activeAgentCount.incrementAndGet();
    }

    /** 记录 Agent 完成 */
    public void recordAgentFinish(String agentName, long durationMs) {
        agentDuration.record(durationMs, TimeUnit.MILLISECONDS);
        activeAgentCount.decrementAndGet();
    }

    /** 记录 Agent 重试 */
    public void recordAgentRetry() {
        agentRetryTotal.increment();
    }

    /** 记录工具调用 */
    public void recordToolCall(long durationMs, boolean success) {
        toolCallTotal.increment();
        workerDuration.record(durationMs, TimeUnit.MILLISECONDS);
        if (!success) {
            toolErrorTotal.increment();
        }
    }

    /** 记录 Planner 调用 */
    public void recordPlannerCall(long durationMs) {
        plannerDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** 记录 Orchestrator 轮次 */
    public void recordOrchestratorRound(int round, long durationMs) {
        currentOrchestratorRound.set(round);
        orchestratorRoundDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** 记录 CLARIFY 事件 */
    public void recordClarify() {
        clarifyTotal.increment();
    }
}
