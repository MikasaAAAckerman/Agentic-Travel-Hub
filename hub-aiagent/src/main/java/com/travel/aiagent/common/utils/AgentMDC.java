package com.travel.aiagent.common.utils;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Agent MDC (Mapped Diagnostic Context) 工具类
 * 用于在日志中注入追踪信息，方便问题排查
 *
 * MDC 字段说明：
 * - traceId   : 请求级别唯一标识，贯穿整个请求生命周期
 * - agentName : 当前执行的 Agent 名称
 * - round     : 当前执行轮次
 * - isRetry   : 是否为重试操作
 * - eventType : 事件类型（DISPATCH, INVOKE, FINISH, RETRY 等）
 */
public class AgentMDC {

    private static final String TRACE_ID = "traceId";
    private static final String AGENT_NAME = "agentName";
    private static final String ROUND = "round";
    private static final String IS_RETRY = "isRetry";
    private static final String EVENT_TYPE = "eventType";

    private AgentMDC() {
    }

    // ==================== 设置方法 ====================

    /**
     * 生成并设置 traceId（如果当前不存在）
     */
    public static String setTraceIdIfAbsent() {
        String traceId = MDC.get(TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            MDC.put(TRACE_ID, traceId);
        }
        return traceId;
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void setAgentName(String agentName) {
        MDC.put(AGENT_NAME, agentName);
    }

    public static void setRound(int round) {
        MDC.put(ROUND, String.valueOf(round));
    }

    public static void setIsRetry(boolean isRetry) {
        MDC.put(IS_RETRY, String.valueOf(isRetry));
    }

    public static void setEventType(String eventType) {
        MDC.put(EVENT_TYPE, eventType);
    }

    // ==================== 获取方法 ====================

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static String getAgentName() {
        return MDC.get(AGENT_NAME);
    }

    // ==================== 批量操作 ====================

    /**
     * 一次性设置多个 MDC 字段
     */
    public static void setAll(String agentName, int round, boolean isRetry, String eventType) {
        setTraceIdIfAbsent();
        setAgentName(agentName);
        setRound(round);
        setIsRetry(isRetry);
        setEventType(eventType);
    }

    /**
     * 清理当前线程的所有 MDC 信息（防止线程池复用导致的上下文泄漏）
     */
    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(AGENT_NAME);
        MDC.remove(ROUND);
        MDC.remove(IS_RETRY);
        MDC.remove(EVENT_TYPE);
    }

    /**
     * 清理 Agent 相关的 MDC，保留 traceId
     */
    public static void clearAgentContext() {
        MDC.remove(AGENT_NAME);
        MDC.remove(ROUND);
        MDC.remove(IS_RETRY);
        MDC.remove(EVENT_TYPE);
    }
}
