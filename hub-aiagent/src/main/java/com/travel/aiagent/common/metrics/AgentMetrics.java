package com.travel.aiagent.common.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 指标组件（Layer 2：Prometheus + Grafana）
 *
 * 目的：实时监控服务运行状态，通过 Grafana 仪表盘可视化
 * 数据流：AgentMetrics → Micrometer → Actuator /actuator/prometheus → Prometheus → Grafana
 *
 * 三种指标类型：
 * - Counter: 累计计数（调用次数、错误次数）→ Grafana Stat 面板
 * - Timer:   耗时统计（P50/P95/P99 分位数）→ Grafana TimeSeries 面板
 * - Gauge:   瞬时值（当前活跃 Agent 数）   → Grafana Gauge 面板
 *
 * 配合 AgentMDCAspect（AOP 切面）自动记录，业务代码无需手动调用
 */
@Component
@Getter
public class AgentMetrics {

    // ═══════════════════════════════════════════════════════════
    // Counter 指标（只增不减，Grafana Stat 面板展示累计值）
    // ═══════════════════════════════════════════════════════════

    /** Agent 调用总次数 → Grafana "Agent 调用总次数" 面板 */
    private final Counter agentInvokeTotal;

    /** Agent 重试总次数 → Grafana "重试次数" 面板，突增说明有问题 */
        private final Counter agentRetryTotal;

    /** 工具调用总次数 → Grafana "工具调用总数" 面板 */
    private final Counter toolCallTotal;

    /** 工具调用失败次数 → Grafana "工具错误数" 面板，配合 toolCallTotal 算失败率 */
    private final Counter toolErrorTotal;

    /** CLARIFY 事件次数 → Grafana "CLARIFY 次数" 面板，说明 Planner 需要用户补充信息 */
    private final Counter clarifyTotal;

    // ═══════════════════════════════════════════════════════════
    // Timer 指标（统计耗时分布，Grafana TimeSeries 面板展示 P50/P95/P99）
    // ═══════════════════════════════════════════════════════════

    /** Agent 执行耗时 → Grafana "Agent 执行耗时 (P50/P95/P99)" 面板 */
    private final Timer agentDuration;

    /** Planner 调用 DeepSeek 的耗时 → Grafana "Planner 执行耗时" 面板 */
    private final Timer plannerDuration;

    /** Worker 调用工具的耗时 → Grafana "Worker 执行耗时" 面板 */
    private final Timer workerDuration;

    /** Orchestrator 单轮耗时 → Grafana "Orchestrator 单轮耗时" 面板 */
    private final Timer orchestratorRoundDuration;

    // ═══════════════════════════════════════════════════════════
    // Gauge 指标（瞬时值，可增可减，Grafana Gauge 面板展示当前状态）
    // ═══════════════════════════════════════════════════════════

    /** 当前活跃 Agent 数量 → Grafana "当前活跃 Agent" 面板，0 表示空闲 */
    private final AtomicInteger activeAgentCount;

    /** 当前 Orchestrator 轮次 → Grafana 面板，追踪多轮编排进度 */
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
