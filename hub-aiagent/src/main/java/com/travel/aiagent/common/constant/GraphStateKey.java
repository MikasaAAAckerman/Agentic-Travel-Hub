package com.travel.aiagent.common.constant;

import lombok.Getter;

/**
 * Graph中，State结果Map的key字段
 */
@Getter
public enum GraphStateKey {

    USER_INPUT("userInput", "用户输入"),
    USER_ID("userId", "用户ID，ShortTermMemory 会话隔离用"),
    CHAT_ID("chatId", "聊天室ID，ShortTermMemory 会话隔离用"),
    ACTION("action", "Planner的下一步行为"),
    ORCHESTRATOR_AGENT_PLAN_DETAIL("orchestratorAgentPlanDetail", "调度者Agent的规划结果"),
    ORCHESTRATOR_AGENT_CONCLUSION("orchestratorAgentConclusion", "调度者Agent的结论"),
    SUB_AGENT_NAME("subAgentName", "子Agent的名字"),
    SUB_AGENT_PLAN_DETAIL("subAgentPlanDetail", "子Agent的规划结果"),
    WORKER_CONCLUSION("workerConclusion", "worker结论"),
    LOOP_TIMES("loopTimes", "Agent调度者与子Agent，子AgentPlanner和worker的对话次数"),


    ;

    private final String key;
    private final String description;

    GraphStateKey(String key, String description) {
        this.key = key;
        this.description = description;
    }


}
