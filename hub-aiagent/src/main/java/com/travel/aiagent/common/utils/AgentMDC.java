package com.travel.aiagent.common.utils;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Agent MDC (Mapped Diagnostic Context) 工具类（Layer 1：ELK 结构化日志）
 *
 * 目的：往 SLF4J MDC（ThreadLocal）里塞字段，
 *       logback JSON 编码器通过 %X{key} 自动读取并输出到日志文件，
 *       最终经 Logstash 解析写入 ES，在 Kibana 中可按字段搜索过滤。
 *
 * 数据流：AgentMDC.setXXX() → MDC.put() → logback %X{key} → JSON 日志 → Logstash → ES → Kibana
 *
 * MDC 字段说明：
 * - traceId         : 请求级别唯一标识，贯穿整个请求生命周期，Kibana 里按此字段追踪完整链路
 * - agentName       : 当前执行的 Agent 名称（如 OrchestratorAgent），Kibana 里按此字段过滤
 * - round           : 当前执行轮次
 * - isRetry         : 是否为重试操作
 * - eventType       : 事件类型（AGENT_INVOKE, PLANNER_INPUT, WORKER_OUTPUT 等），Kibana 里按此字段搜索
 * - plannerInput    : Planner 的输入（用户需求），用于查看 LLM 收到了什么
 * - plannerOutput   : Planner 的输出（规划结果），用于查看 LLM 返回了什么
 * - plannerAction   : Planner 选择的动作（CLARIFY/TASK_DISPATCH 等）
 * - workerConclusion: Worker 的结论（工具执行结果），用于查看最终回复
 * - toolNames       : 调用的工具名称，用于追踪工具使用情况
 * - subAgentName    : 子 Agent 名称，用于追踪任务派发
 */
public class AgentMDC {

    private static final String TRACE_ID = "traceId";
    private static final String AGENT_NAME = "agentName";
    private static final String ROUND = "round";
    private static final String IS_RETRY = "isRetry";
    private static final String EVENT_TYPE = "eventType";

    // ==================== 内容追踪字段（供 ELK 搜索） ====================
    private static final String PLANNER_INPUT = "plannerInput";
    private static final String PLANNER_OUTPUT = "plannerOutput";
    private static final String PLANNER_ACTION = "plannerAction";
    private static final String WORKER_CONCLUSION = "workerConclusion";
    private static final String TOOL_NAMES = "toolNames";
    private static final String SUB_AGENT_NAME = "subAgentName";

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

    // ==================== 内容追踪字段 setter ====================

    public static void setPlannerInput(String plannerInput) {
        MDC.put(PLANNER_INPUT, truncate(plannerInput, 500));
    }

    public static void setPlannerOutput(String plannerOutput) {
        MDC.put(PLANNER_OUTPUT, truncate(plannerOutput, 500));
    }

    public static void setPlannerAction(String plannerAction) {
        MDC.put(PLANNER_ACTION, plannerAction);
    }

    public static void setWorkerConclusion(String workerConclusion) {
        MDC.put(WORKER_CONCLUSION, truncate(workerConclusion, 500));
    }

    public static void setToolNames(String toolNames) {
        MDC.put(TOOL_NAMES, toolNames);
    }

    public static void setSubAgentName(String subAgentName) {
        MDC.put(SUB_AGENT_NAME, subAgentName);
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

    /**
     * 清理 Planner/Worker 内容字段（每次调用后清理，防止上下文泄漏）
     */
    public static void clearContentContext() {
        MDC.remove(PLANNER_INPUT);
        MDC.remove(PLANNER_OUTPUT);
        MDC.remove(PLANNER_ACTION);
        MDC.remove(WORKER_CONCLUSION);
        MDC.remove(TOOL_NAMES);
        MDC.remove(SUB_AGENT_NAME);
    }

    /**
     * 截断过长内容，防止 MDC 占用过多内存
     */
    private static String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) + "...(" + value.length() + " chars)" : value;
    }
}
