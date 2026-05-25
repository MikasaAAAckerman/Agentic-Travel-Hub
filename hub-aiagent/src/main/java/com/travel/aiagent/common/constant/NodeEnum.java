package com.travel.aiagent.common.constant;

import lombok.Getter;

@Getter
public enum NodeEnum {

    /**
     * 节点：执行当前步骤所需工具
     */
    WORKER_NODE("worker_node", PlanActionEnum.TOOL_CALL.getType()),

    /**
     * 节点：记忆压缩
     */
    MEMORY_PRUNE_NODE("memory_prune_node", PlanActionEnum.FINISH.getType()),


    /**
     * 节点：任务拆解为单步骤
     */
    PLANNER_NODE("planner_node", ""),

    CLARIFY_NODE("clarify_node", PlanActionEnum.CLARIFY.getType()),

    SUB_AGENT_NODE("sub_agent_node", PlanActionEnum.SUB_AGENT_CALL.getType()),

    FINISH_NODE("finish_node", PlanActionEnum.FINISH.getType()),

    ;

    private String nodeName;
    private String type;

    NodeEnum(String nodeName, String type) {
        this.nodeName = nodeName;
        this.type = type;
    }

    public static NodeEnum getByPlanType(String type) {
        for (NodeEnum value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }


}
