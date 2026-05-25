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

    ;

    private String type;

    PlanActionEnum(String type) {
        this.type = type;
    }

}
