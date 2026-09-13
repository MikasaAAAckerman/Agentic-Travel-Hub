package com.travel.aiagent.common.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 子 Agent 内部一轮「自我 ReAct」（v3 结构化记忆的原子单元）。
 *
 * <p>场景：orchestrator 下发 plan 给子 Agent，子 Agent 内部做多轮 planner→worker→planner 循环。
 * 每一轮 = 一个自我 plan + worker 结论。挂在 {@link SubAgentReActContextVO} 之下累积，
 * 保证子 Agent 在一轮执行内不丢失「前几轮干了什么」的记忆。
 *
 * <p>序列化给 LLM 的形态：
 * <pre>
 * { "traceId": "tr-xxx", "agent": "RouteGraphAgent", "round": 1,
 *   "plan": "制定一下9月17号的路程", "conclusion": "9月17日：白云山→越秀公园…" }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnVO {

    /** 请求级 traceId（透传，方便按 traceId 追踪整个对话中 subAgent 做了什么） */
    private String traceId;

    /** 哪个 subAgent（如 RouteGraphAgent） */
    private String agent;

    /** 子 Agent 内部第几轮自我规划（从 1 开始） */
    private int round;

    /** 自我 plan（给 worker 的任务） */
    private String plan;

    /** worker 执行结论 */
    private String conclusion;
}
