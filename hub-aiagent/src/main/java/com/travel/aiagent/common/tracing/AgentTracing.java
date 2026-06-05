package com.travel.aiagent.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.TraceContext;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 分布式追踪组件
 *
 * 基于 Micrometer Tracing（桥接 OpenTelemetry），
 * 在关键调用点创建 Span，形成完整的调用链路。
 *
 * Span 层级示例：
 * [Controller]
 *   └── [OrchestratorAgent]
 *         ├── [Planner] doOrchestratorAgentPlan
 *         ├── [WeatherGraphAgent]
 *         │     ├── [Planner] doSubAgentPlan
 *         │     └── [Worker] doWorkWithRagParallel
 *         │           └── [Tool] weatherQueryTool
 *         └── [FoodGraphAgent]
 *               ├── [Planner] doSubAgentPlan
 *               └── [Worker] doWorkWithRagParallel
 *                     └── [Tool] restaurantSearchTool
 */
@Component
@Getter
public class AgentTracing {

    private final Tracer tracer;

    /** 当前活跃的 Span 引用（用于嵌套） */
    private final AtomicReference<Span> currentSpan = new AtomicReference<>();

    public AgentTracing(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * 创建 Agent 级别的 Span
     */
    public Span createAgentSpan(String agentName) {
        Span span = tracer.nextSpan()
                .name("Agent." + agentName)
                .tag("agent.name", agentName)
                .tag("span.type", "agent")
                .start();
        currentSpan.set(span);
        return span;
    }

    /**
     * 创建 Planner 级别的 Span
     */
    public Span createPlannerSpan(String methodName) {
        Span parent = currentSpan.get();
        Span span;
        if (parent != null) {
            span = tracer.nextSpan(parent)
                    .name("Planner." + methodName)
                    .tag("planner.method", methodName)
                    .tag("span.type", "planner")
                    .start();
        } else {
            span = tracer.nextSpan()
                    .name("Planner." + methodName)
                    .tag("planner.method", methodName)
                    .tag("span.type", "planner")
                    .start();
        }
        return span;
    }

    /**
     * 创建 Worker 级别的 Span
     */
    public Span createWorkerSpan(String methodName) {
        Span parent = currentSpan.get();
        Span span;
        if (parent != null) {
            span = tracer.nextSpan(parent)
                    .name("Worker." + methodName)
                    .tag("worker.method", methodName)
                    .tag("span.type", "worker")
                    .start();
        } else {
            span = tracer.nextSpan()
                    .name("Worker." + methodName)
                    .tag("worker.method", methodName)
                    .tag("span.type", "worker")
                    .start();
        }
        return span;
    }

    /**
     * 创建工具调用级别的 Span
     */
    public Span createToolSpan(String toolName) {
        Span parent = currentSpan.get();
        Span span;
        if (parent != null) {
            span = tracer.nextSpan(parent)
                    .name("Tool." + toolName)
                    .tag("tool.name", toolName)
                    .tag("span.type", "tool")
                    .start();
        } else {
            span = tracer.nextSpan()
                    .name("Tool." + toolName)
                    .tag("tool.name", toolName)
                    .tag("span.type", "tool")
                    .start();
        }
        return span;
    }

    /**
     * 结束 Span 并记录结果
     */
    public void endSpan(Span span, boolean success) {
        if (span != null) {
            span.tag("success", String.valueOf(success));
            span.end();
        }
    }

    /**
     * 结束 Span 并记录异常
     */
    public void endSpanWithError(Span span, Throwable t) {
        if (span != null) {
            span.tag("success", "false");
            span.tag("error.type", t.getClass().getSimpleName());
            span.tag("error.message", t.getMessage() != null ? t.getMessage().substring(0, Math.min(100, t.getMessage().length())) : "unknown");
            span.error(t);
            span.end();
        }
    }

    /**
     * 获取当前 TraceId（用于日志关联）
     */
    public String getCurrentTraceId() {
        Span span = tracer.currentSpan();
        if (span != null) {
            TraceContext context = span.context();
            return context.traceId();
        }
        return null;
    }
}
