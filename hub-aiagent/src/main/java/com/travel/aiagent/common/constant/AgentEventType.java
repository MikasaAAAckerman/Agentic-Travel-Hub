package com.travel.aiagent.common.constant;

import lombok.Getter;

/**
 * Agent 事件类型枚举
 * 用于 MDC eventType 字段，标记当前日志对应的事件
 */
@Getter
public enum AgentEventType {

    // ==================== Orchestrator 事件 ====================
    TASK_DISPATCH("TASK_DISPATCH", "任务派发给子Agent"),
    ORCHESTRATOR_ROUND("ORCHESTRATOR_ROUND", "Orchestrator新一轮规划"),
    ORCHESTRATOR_FINISH("ORCHESTRATOR_FINISH", "Orchestrator任务完成"),

    // ==================== SubAgent 事件 ====================
    AGENT_INVOKE("AGENT_INVOKE", "子Agent开始执行"),
    AGENT_FINISH("AGENT_FINISH", "子Agent执行完成"),
    AGENT_RETRY("AGENT_RETRY", "子Agent重试"),

    // ==================== 工具事件 ====================
    TOOL_CALL("TOOL_CALL", "调用外部工具"),
    TOOL_FINISH("TOOL_FINISH", "工具调用完成"),
    TOOL_ERROR("TOOL_ERROR", "工具调用失败"),

    // ==================== 用户交互 ====================
    CLARIFY("CLARIFY", "需要用户补充信息"),

    // ==================== 系统事件 ====================
    UNKNOWN("UNKNOWN", "未知状态"),
    ERROR("ERROR", "系统异常");

    private final String type;
    private final String description;

    AgentEventType(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }
}
