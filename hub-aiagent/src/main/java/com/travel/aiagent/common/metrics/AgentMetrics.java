package com.travel.aiagent.common.metrics;

import com.travel.aiagent.common.core.constraint.AuditResult;
import com.travel.aiagent.common.core.constraint.Finding;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Locale;
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

    /**
     * ⚠️ Bug 修复（dashboard sum by (agentName) 数据为空）：
     * agentInvokeTotal 改为懒加载：去掉字段，改为在 recordAgentInvoke 中
     * 用 MeterRegistry.counter(name, tags...) 便利方法按需创建（Micrometer 内部按 name+tags 去重）。
     * 字段已删除（之前是有字段但没设值，现在是彻底没有字段了，避免编译错误）。
     */

    /** Agent 重试总次数 → Grafana "重试次数" 面板，突增说明有问题 */
    private final Counter agentRetryTotal;

    /** 工具调用总次数 → Grafana "工具调用总数" 面板 */
    private final Counter toolCallTotal;

    /** 工具调用失败次数 → Grafana "工具错误数" 面板，配合 toolCallTotal 算失败率 */
    private final Counter toolErrorTotal;

    /** CLARIFY 事件次数 → Grafana "CLARIFY 次数" 面板，说明 Planner 需要用户补充信息 */
    private final Counter clarifyTotal;

    /** 用户手动停止会话次数 → Grafana "停止次数" 面板，配合会话总数可评估用户满意度 */
    private final Counter agentStopTotal;

    // ═══════════════════════════════════════════════════════════
    // Timer 指标（统计耗时分布，Grafana TimeSeries 面板展示 P50/P95/P99）
    // ═══════════════════════════════════════════════════════════

    /**
     * ⚠️ 同样的 bug：Timer 也需要按 agentName 切分。
     * agentDuration 字段已删除，改在 recordAgentFinish 中按需 register 带 tag 的 Timer。
     */

    /** Planner 调用 DeepSeek 的耗时 → Grafana "Planner 执行耗时" 面板 */
    private final Timer plannerDuration;

    /** Worker 调用工具的耗时 → Grafana "Worker 执行耗时" 面板 */
    private final Timer workerDuration;

    /** Orchestrator 单轮耗时 → Grafana "Orchestrator 单轮耗时" 面板 */
    private final Timer orchestratorRoundDuration;

    /** 持有 MeterRegistry，供 recordAgentInvoke/Finish 内部按需 register 带 tag 的 meter */
    private final MeterRegistry meterRegistry;

    /** 约束对账耗时 → Grafana "约束对账耗时" 面板（应 < 5ms，否则说明偷偷调了 LLM） */
    private final Timer constraintAuditDuration;

    // ═══════════════════════════════════════════════════════════
    // Gauge 指标（瞬时值，可增可减，Grafana Gauge 面板展示当前状态）
    // ═══════════════════════════════════════════════════════════

    /** 当前活跃 Agent 数量 → Grafana "当前活跃 Agent" 面板，0 表示空闲 */
    private final AtomicInteger activeAgentCount;

    /** 当前 Orchestrator 轮次 → Grafana 面板，追踪多轮编排进度 */
    private final AtomicInteger currentOrchestratorRound;

    // ==================== 构造函数 ====================

    public AgentMetrics(MeterRegistry registry) {
        this.meterRegistry = registry;

        // Counter - agentInvokeTotal 改为懒加载（见 recordAgentInvoke 中按需 register，带 agentName tag）
        // this.agentInvokeTotal = ...   ← 删掉
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

        this.agentStopTotal = Counter.builder("agent.cancel.total")
                .description("用户手动停止会话次数")
                .register(registry);

        // Timer - agentDuration 改为懒加载（见 recordAgentFinish 中按需 register，带 agentName tag）
        // this.agentDuration = ...   ← 删掉
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

        this.constraintAuditDuration = Timer.builder("constraint.audit.duration")
                .description("约束对账耗时（纯函数，应远低于 5ms）")
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
        // 按需注册带 agentName tag 的 Counter（Micrometer 内部按 name+tags 去重，返回同一 meter）
        meterRegistry.counter("agent.invoke.total", "agentName", agentName).increment();
        activeAgentCount.incrementAndGet();
    }

    /** 记录 Agent 完成 */
    public void recordAgentFinish(String agentName, long durationMs) {
        // 按需注册带 agentName tag 的 Timer（Micrometer 内部按 name+tags 去重）
        Timer.builder("agent.duration")
                .description("Agent 执行耗时")
                .tag("agentName", agentName)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
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

    /** 记录用户手动停止会话 */
    public void recordAgentStop() {
        agentStopTotal.increment();
    }

    // ==================== 约束治理 ====================

    /**
     * 记录一次约束对账。
     *
     * <p>指标口径对齐 agent.* / tool.*：
     * <ul>
     *   <li>{@code constraint.audit.total{result=pass|soft_fail|hard_fail}} —— 对账通过率</li>
     *   <li>{@code constraint.violation.total{type=missing_must|ghost_receipt|...}} —— 违规分布，哪类最常见</li>
     * </ul>
     */
    public void recordConstraintAudit(AuditResult audit, long durationMs) {
        if (audit == null) {
            return;
        }
        meterRegistry.counter("constraint.audit.total",
                "result", audit.verdict().name().toLowerCase(Locale.ROOT)).increment();
        for (Finding finding : audit.findings()) {
            meterRegistry.counter("constraint.violation.total",
                    "type", finding.type().name().toLowerCase(Locale.ROOT)).increment();
        }
        constraintAuditDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** 记录一条裁决状态，用于算约束履行率 */
    public void recordConstraintVerdict(String status) {
        if (status == null || status.isBlank()) {
            return;
        }
        meterRegistry.counter("constraint.verdict.total",
                "status", status.toLowerCase(Locale.ROOT)).increment();
    }

    /**
     * 记录一次"约束回执被拒"（前置校验失败，本轮不执行工具）。
     *
     * <p>这个指标突增通常说明 Prompt 或 schema 出了问题，而不是模型变笨了。
     */
    public void recordConstraintAuditReject() {
        meterRegistry.counter("constraint.audit.reject.total").increment();
    }
}
