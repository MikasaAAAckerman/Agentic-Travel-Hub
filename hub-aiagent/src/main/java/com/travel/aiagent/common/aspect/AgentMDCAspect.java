package com.travel.aiagent.common.aspect;

import com.travel.aiagent.common.constant.AgentEventType;
import com.travel.aiagent.common.utils.AgentMDC;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Agent MDC 自动织入切面
 *
 * 统一拦截所有 Agent 的 execute 方法，自动管理 MDC 上下文：
 * - traceId    : 请求级别追踪ID，自动传递
 * - agentName  : 从类名自动提取（如 OrchestratorAgent → OrchestratorAgent）
 * - eventType  : 入口标记 AGENT_INVOKE，出口标记 AGENT_FINISH，异常标记 ERROR
 *
 * 业务语义事件（CLARIFY、TASK_DISPATCH、RETRY）由各 Agent 内部手动埋点
 */
@Slf4j
@Aspect
@Component
public class AgentMDCAspect {

    /**
     * 拦截所有 Agent 的 execute 方法
     * 匹配规则：com.travel.aiagent 包下所有类名以 Agent 结尾的 execute 方法
     */
    @Around("execution(* com.travel.aiagent..*Agent.execute(..)) || " +
            "execution(* com.travel.aiagent..*Engine.execute(..))")
    public Object aroundAgentExecute(ProceedingJoinPoint pjp) throws Throwable {

        // 自动提取 Agent 名称
        String agentName = pjp.getTarget().getClass().getSimpleName();

        // 设置 MDC 上下文
        AgentMDC.setTraceIdIfAbsent();
        AgentMDC.setAgentName(agentName);
        AgentMDC.setEventType(AgentEventType.AGENT_INVOKE.getType());

        log.info("[{}] 开始执行 | args={}", agentName, maskArgs(pjp.getArgs()));

        long startTime = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();

            long elapsed = System.currentTimeMillis() - startTime;
            AgentMDC.setEventType(AgentEventType.AGENT_FINISH.getType());
            log.info("[{}] 执行完成 | elapsed={}ms", agentName, elapsed);

            return result;

        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - startTime;
            AgentMDC.setEventType(AgentEventType.ERROR.getType());
            log.error("[{}] 执行异常 | elapsed={}ms | error={}", agentName, elapsed, t.getMessage());
            throw t;

        } finally {
            AgentMDC.clearAgentContext();
        }
    }

    /**
     * 拦截 Planner 调用（DeepSeekPlannerService）
     */
    @Around("execution(* com.travel.aiagent.common.core.planner.DeepSeekPlannerService.*(..))")
    public Object aroundPlanner(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getTarget().getClass().getSimpleName() + "." + pjp.getSignature().getName();

        long startTime = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[Planner] {} 完成 | elapsed={}ms", methodName, elapsed);
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[Planner] {} 异常 | elapsed={}ms | error={}", methodName, elapsed, t.getMessage());
            throw t;
        }
    }

    /**
     * 拦截 Worker 调用（QwenWorkerService）
     */
    @Around("execution(* com.travel.aiagent.common.core.worker.QwenWorkerService.*(..))")
    public Object aroundWorker(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getTarget().getClass().getSimpleName() + "." + pjp.getSignature().getName();
        AgentMDC.setEventType(AgentEventType.TOOL_CALL.getType());

        long startTime = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            AgentMDC.setEventType(AgentEventType.TOOL_FINISH.getType());
            log.info("[Worker] {} 完成 | elapsed={}ms", methodName, elapsed);
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - startTime;
            AgentMDC.setEventType(AgentEventType.TOOL_ERROR.getType());
            log.error("[Worker] {} 异常 | elapsed={}ms | error={}", methodName, elapsed, t.getMessage());
            throw t;
        }
    }

    /**
     * 拦截 Controller 入口，初始化 traceId
     */
    @Around("execution(* com.travel.starter.controller..*(..))")
    public Object aroundController(ProceedingJoinPoint pjp) throws Throwable {
        // 在请求入口初始化 traceId
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
