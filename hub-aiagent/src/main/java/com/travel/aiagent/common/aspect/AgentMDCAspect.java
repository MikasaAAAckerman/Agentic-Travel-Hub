package com.travel.aiagent.common.aspect;

import com.travel.aiagent.common.constant.AgentEventType;
import com.travel.aiagent.common.metrics.AgentMetrics;
import com.travel.aiagent.common.tracing.AgentTracing;
import com.travel.aiagent.common.utils.AgentMDC;
import io.micrometer.tracing.Span;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Agent MDC 自动织入切面
 *
 * 统一拦截所有 Agent 的 execute 方法，自动管理两层监控：
 *
 * ═══ Layer 1：结构化日志（ELK）═══
 * 目的：在 Kibana 中按字段搜索 LLM 的输入与输出，排查问题
 * 实现：通过 SLF4J MDC 注入 traceId、agentName、eventType 等字段，
 *       配合 logback JSON 编码器 → Logstash 解析 → ES 存储 → Kibana 查询
 * 效果：能在 Kibana 里按 agentName 过滤、按 eventType 搜索、查看 plannerInput/workerConclusion
 *
 * ═══ Layer 2：指标监控（Prometheus + Grafana）═══
 * 目的：实时监控服务运行状态（调用次数、耗时、失败率）
 * 实现：通过 AgentMetrics 记录 Counter/Timer/Gauge，
 *       配合 Actuator 暴露 /actuator/prometheus → Prometheus 抓取 → Grafana 渲染
 * 效果：Grafana 仪表盘展示 P50/P95/P99 耗时、工具失败率、活跃 Agent 数
 *
 * 业务语义事件（CLARIFY、TASK_DISPATCH、RETRY）由各 Agent 内部手动埋点
 */
@Slf4j
@Aspect
@Component
public class AgentMDCAspect {

    // Layer 2：Prometheus 指标（Counter/Timer/Gauge）
    @Resource
    private AgentMetrics agentMetrics;

    // Layer 3：Jaeger 链路追踪（Span）
    @Resource
    private AgentTracing agentTracing;

    /**
     * 拦截所有 Agent 的 execute 方法
     * 匹配规则：com.travel.aiagent 包下所有类名以 Agent 结尾的 execute 方法
     */
    @Around("execution(* com.travel.aiagent..*Agent.execute(..)) || " +
            "execution(* com.travel.aiagent..*Engine.execute(..))")
    public Object aroundAgentExecute(ProceedingJoinPoint pjp) throws Throwable {

        // 自动提取 Agent 名称
        String agentName = pjp.getTarget().getClass().getSimpleName();

        // ── Layer 1：ELK 结构化日志 ──
        // 往 MDC 里塞字段，logback JSON 编码器会自动附带到每条日志
        // 效果：Kibana 里能按 agentName、eventType 过滤搜索
        AgentMDC.setTraceIdIfAbsent();
        AgentMDC.setAgentName(agentName);
        AgentMDC.setEventType(AgentEventType.AGENT_INVOKE.getType());

        // ── Layer 3：Jaeger 链路追踪 ──
        Span span = agentTracing.createAgentSpan(agentName);

        // ── Layer 2：Prometheus 指标 ──
        // Counter +1（调用次数），Gauge +1（活跃 Agent 数）
        agentMetrics.recordAgentInvoke(agentName);

        log.info("[{}] 开始执行 | args={}", agentName, maskArgs(pjp.getArgs()));

        long startTime = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();

            long elapsed = System.currentTimeMillis() - startTime;

            // ── Layer 1：ELK ── 标记事件类型为完成
            AgentMDC.setEventType(AgentEventType.AGENT_FINISH.getType());
            log.info("[{}] 执行完成 | elapsed={}ms", agentName, elapsed);

            // ── Layer 2：Prometheus ── Timer 记录耗时，Gauge -1（活跃数减少）
            agentMetrics.recordAgentFinish(agentName, elapsed);

            // ── Layer 3：Jaeger ── 正常结束 Span
            agentTracing.endSpan(span, true);

            return result;

        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - startTime;

            // ── Layer 1：ELK ── 标记事件类型为异常，Kibana 里能搜 eventType:ERROR
            AgentMDC.setEventType(AgentEventType.ERROR.getType());
            log.error("[{}] 执行异常 | elapsed={}ms | error={}", agentName, elapsed, t.getMessage());

            // ── Layer 2：Prometheus ── 异常也记录耗时（Timer），Gauge -1
            agentMetrics.recordAgentFinish(agentName, elapsed);

            // ── Layer 3：Jaeger ── 标记 Span 为异常状态（红色高亮）
            agentTracing.endSpanWithError(span, t);

            throw t;

        } finally {
            // ── Layer 1：ELK ── 清理 MDC，防止线程池复用导致日志字段污染
            AgentMDC.clearAgentContext();
        }
    }

    /**
     * 拦截 Planner 调用（DeepSeekPlannerService）
     */
    @Around("execution(* com.travel.aiagent.common.core.planner.DeepSeekPlannerService.*(..))")
    public Object aroundPlanner(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();

        // ── Layer 3：Jaeger ── 创建 Planner Span
        Span span = agentTracing.createPlannerSpan(methodName);

        long startTime = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - startTime;

            // ── Layer 1：ELK ── Planner 完成日志（plannerInput/Output 由 DeepSeekPlannerService 内部手动埋点）
            log.info("[Planner] {} 完成 | elapsed={}ms", methodName, elapsed);

            // ── Layer 2：Prometheus ── Timer 记录 Planner 耗时
            agentMetrics.recordPlannerCall(elapsed);

            // ── Layer 3：Jaeger ── 正常结束 Span
            agentTracing.endSpan(span, true);

            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - startTime;

            // ── Layer 1：ELK ── 异常日志
            log.error("[Planner] {} 异常 | elapsed={}ms | error={}", methodName, elapsed, t.getMessage());

            // ── Layer 2：Prometheus ── 异常也记录耗时
            agentMetrics.recordPlannerCall(elapsed);

            // ── Layer 3：Jaeger ── 标记 Span 异常
            agentTracing.endSpanWithError(span, t);

            throw t;
        }
    }

    /**
     * 拦截 Worker 调用（QwenWorkerService）
     */
    @Around("execution(* com.travel.aiagent.common.core.worker.QwenWorkerService.*(..))")
    public Object aroundWorker(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();

        // ── Layer 1：ELK ── 标记事件类型为工具调用
        AgentMDC.setEventType(AgentEventType.TOOL_CALL.getType());

        // ── Layer 3：Jaeger ── 创建 Worker Span
        Span span = agentTracing.createWorkerSpan(methodName);

        long startTime = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - startTime;

            // ── Layer 1：ELK ── 标记工具完成（toolNames/workerConclusion 由 QwenWorkerService 内部手动埋点）
            AgentMDC.setEventType(AgentEventType.TOOL_FINISH.getType());
            log.info("[Worker] {} 完成 | elapsed={}ms", methodName, elapsed);

            // ── Layer 2：Prometheus ── Counter +1，Timer 记录耗时
            agentMetrics.recordToolCall(elapsed, true);

            // ── Layer 3：Jaeger ── 正常结束 Span
            agentTracing.endSpan(span, true);

            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - startTime;

            // ── Layer 1：ELK ── 标记工具异常
            AgentMDC.setEventType(AgentEventType.TOOL_ERROR.getType());
            log.error("[Worker] {} 异常 | elapsed={}ms | error={}", methodName, elapsed, t.getMessage());

            // ── Layer 2：Prometheus ── 工具失败 Counter +1，Timer 也记录耗时
            agentMetrics.recordToolCall(elapsed, false);

            // ── Layer 3：Jaeger ── 标记 Span 异常
            agentTracing.endSpanWithError(span, t);

            throw t;
        }
    }

    /**
     * 拦截 Controller 入口，初始化 traceId
     * Layer 1：ELK ── 在请求入口生成 traceId，贯穿整个请求链路的日志
     */
    @Around("execution(* com.travel.starter.controller..*(..))")
    public Object aroundController(ProceedingJoinPoint pjp) throws Throwable {
        AgentMDC.setTraceIdIfAbsent();
        return pjp.proceed();
    }

    /**
     * 脱敏参数，避免日志过大
     */
    private Object[] maskArgs(Object[] args) {
        if (args == null) return new Object[0];
        Object[] masked = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String s && s.length() > 100) {
                masked[i] = s.substring(0, 100) + "...(" + s.length() + " chars)";
            } else {
                masked[i] = args[i];
            }
        }
        return masked;
    }
}
