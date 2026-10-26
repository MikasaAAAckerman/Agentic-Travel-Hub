package com.travel.aiagent.common.constant;

import lombok.Getter;

/**
 * 用于定义Planner返回的计划类型
 */
@Getter
public enum PlanActionEnum {
    // "TOOL_CALL 或 FINISH 或 CLARIFY",
    TOOL_CALL("TOOL_CALL"),
    FINISH("FINISH"),
    CLARIFY("CLARIFY"),
    SUB_AGENT_CALL("SUB_AGENT_CALL"),

    /**
     * 约束回执校验被拒，本轮回 planner 重来。
     *
     * <p>不用 {@code StateGraph.START} 表达这个语义 —— 那样会依赖"未知 action 一律回 planner"
     * 的兜底分支，哪天兜底改了就会静默失效。
     */
    RETRY_PLAN("RETRY_PLAN"),

    ;

    private String type;

    PlanActionEnum(String type) {
        this.type = type;
    }

}
