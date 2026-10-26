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
    TRACE_ID("traceId", "会话级链路追踪ID，前端生成随请求传入，贯穿LLM调用日志"),
    SUB_AGENT_REACT_MEMORY("subAgentReActMemory", "子Agent本轮执行的ReAct记忆（orchestratorPlan + turns），结构化累积不丢失"),

    // ============ 约束治理（全量下行 / 回执上行 / 内容盲审计）============
    GLOBAL_CONSTRAINTS("globalConstraints", "全局约束只读快照（全量下发，唯一写者是 Orchestrator）"),
    DOMAIN_INSTRUCTION("domainInstruction", "子Agent本领域静态执行规则（纯Prompt文本，不是约束数据）"),
    CONSTRAINT_VERDICTS("constraintVerdicts", "子图内部累积的裁决（仅在 FINISH 轮累积）"),
    CONSTRAINT_RECEIPT("constraintReceipt", "约束执行回执（上行，给程序对账）"),
    AUDIT_ERR("auditErr", "上一轮约束回执被拒的原因（String，注入下一轮Prompt）"),


    ;

    private final String key;
    private final String description;

    GraphStateKey(String key, String description) {
        this.key = key;
        this.description = description;
    }


}
