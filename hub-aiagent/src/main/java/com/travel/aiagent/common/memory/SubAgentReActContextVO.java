package com.travel.aiagent.common.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 子 Agent 一次执行的 ReAct 记忆（v3 专用，存在子 Agent 图的 GraphState 中）。
 *
 * <p>orchestratorPlan 固定不变（orchestrator 下发的一次任务），
 * turns 每轮 append 一条 {@link TurnVO}（子 Agent 内部的多轮自我 ReAct）。
 *
 * <p>序列化给 LLM 的形态：
 * <pre>
 * {
 *   "orchestratorPlan": "帮用户规划一下9月17号到9月23号广州的旅游路线",
 *   "turns": [
 *     { "traceId": "tr-xxx", "agent": "RouteGraphAgent", "round": 1,
 *       "plan": "制定一下9月17号的路程", "conclusion": "9月17日：白云山→越秀公园…" }
 *   ]
 * }
 * </pre>
 *
 * <p>生命周期：一次 {@code BaseTravelGraphAgent.execute()} 内（子图 invoke 一次）。
 * 子图跑完即结束，不跨 orchestrator 派发保留（跨派发记忆属第一套 RoundMemory / 复盘走 llm_call_log）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubAgentReActContextVO {

    /** orchestrator 下发的原始 plan（固定，不变） */
    private String orchestratorPlan;

    /** 子 Agent 内部多轮自我 ReAct（每轮 append，累积） */
    private List<TurnVO> turns;
}
