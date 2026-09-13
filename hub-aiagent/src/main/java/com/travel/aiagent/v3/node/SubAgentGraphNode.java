package com.travel.aiagent.v3.node;

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.travel.aiagent.common.constant.AgentEventType;
import com.travel.aiagent.common.constant.GraphStateKey;
import com.travel.aiagent.common.constant.PlanActionEnum;
import com.travel.aiagent.common.core.planner.PlannerService;
import com.travel.aiagent.common.core.worker.WorkerService;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.WorkDetailVO;
import com.travel.aiagent.common.memory.ShortTermMemory;
import com.travel.aiagent.common.memory.SubAgentReActContextVO;
import com.travel.aiagent.common.memory.TurnVO;
import com.travel.aiagent.common.utils.AgentMDC;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 子 Agent 内部 Graph 的节点。
 *
 * <p>每个领域 Agent（RouteAgent / FoodAgent ...）内部都跑这张小图：
 * <pre>
 *   START → planner → worker → planner (loop) → finish → END
 * </pre>
 */
@Service
@Slf4j
public class SubAgentGraphNode {

    @Resource
    private PlannerService plannerService;

    @Resource
    private WorkerService workerService;

    @Resource
    private ShortTermMemory shortTermMemory;

    /**
     * 子Agent视角中，将当前任务再次进行任务规划的node
     */
    public AsyncNodeAction plannerNode() {
        NodeAction action = state -> {

            // 获取调度者Agent派发的任务规划（包含用户原始需求+子任务，始终不变）
            String orchestratorPlan = state.value(GraphStateKey.ORCHESTRATOR_AGENT_PLAN_DETAIL.getKey(), "");
            Integer loopTimes = state.value(GraphStateKey.LOOP_TIMES.getKey(), 0);
            String subAgentName = state.value(GraphStateKey.SUB_AGENT_NAME.getKey(), "");

            // traceId 透传：从上游 GraphState 恢复，保证子 Agent 日志与主链路关联
            String traceId = state.value(GraphStateKey.TRACE_ID.getKey(), "");
            if (StringUtils.isNotBlank(traceId)) {
                AgentMDC.setTraceId(traceId);
            }

            AgentMDC.setSubAgentName(subAgentName);
            AgentMDC.setRound(loopTimes);
            AgentMDC.setEventType(AgentEventType.ORCHESTRATOR_ROUND.getType());
            log.info("[V3-Sub] {} planner 节点 | 第{}轮", subAgentName, loopTimes);

            // 读取子 Agent 的 ReAct 记忆（orchestratorPlan 固定 + turns 累积）
            SubAgentReActContextVO reactContext = state.value(GraphStateKey.SUB_AGENT_REACT_MEMORY.getKey(), (SubAgentReActContextVO) null);
            if (reactContext == null) {
                // 防御：正常情况下 BaseTravelGraphAgent 已初始化，这里兜底
                reactContext = SubAgentReActContextVO.builder()
                        .orchestratorPlan(orchestratorPlan)
                        .turns(new ArrayList<>())
                        .build();
            }

            // 传给 LLM 的是结构化记忆：orchestrator 任务 + 前几轮自我 plan/结论，一轮内不丢记忆
            PlanDetailVO plan = plannerService.doSubAgentPlanReAct(reactContext, subAgentName);

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put(GraphStateKey.ACTION.getKey(), plan.getAction());
            resultMap.put(GraphStateKey.SUB_AGENT_PLAN_DETAIL.getKey(), plan.getPlanDetail());
            resultMap.put(GraphStateKey.ORCHESTRATOR_AGENT_CONCLUSION.getKey(), plan.getConclusion());
            resultMap.put(GraphStateKey.LOOP_TIMES.getKey(), loopTimes + 1);

            AgentMDC.setEventType(AgentEventType.PLANNER_OUTPUT.getType());
            AgentMDC.setPlannerAction(plan.getAction());
            log.info("[V3-Sub] {} planner → action={} | planDetail={}", subAgentName, plan.getAction(), plan.getPlanDetail());

            AgentMDC.clearContentContext();
            return resultMap;
        };
        return AsyncNodeAction.node_async(action);
    }

    /**
     * 子 Agent视角中，执行Planner派发任务的worker节点
     */
    public AsyncNodeAction workerNode() {
        NodeAction action = state -> {
            String planDetail = state.value(GraphStateKey.SUB_AGENT_PLAN_DETAIL.getKey(), "");
            String subAgentName = state.value(GraphStateKey.SUB_AGENT_NAME.getKey(), "");

            AgentMDC.setSubAgentName(subAgentName);
            AgentMDC.setEventType(AgentEventType.WORKER_INPUT.getType());
            log.info("[V3-Sub] {} 进入 worker 节点 | plan={}", subAgentName, planDetail);

            PlanDetailVO planVO = new PlanDetailVO();
            planVO.setPlanDetail(planDetail);
            planVO.setAction(PlanActionEnum.TOOL_CALL.getType());
            WorkDetailVO workerDetail = workerService.doWorkWithRagParallel(planVO);

            Map<String, Object> resultMap = new HashMap<>();

            String workerMemory = state.value(GraphStateKey.WORKER_CONCLUSION.getKey(), "");
            String conclusion = workerDetail.getConclusion();
            resultMap.put(GraphStateKey.WORKER_CONCLUSION.getKey(), workerMemory + conclusion);

            // 结构化 ReAct 记忆：append 一条 TurnVO（供 planner 下轮读取，一轮内不丢记忆）
            SubAgentReActContextVO reactContext = state.value(GraphStateKey.SUB_AGENT_REACT_MEMORY.getKey(), (SubAgentReActContextVO) null);
            if (reactContext != null) {
                String traceId = state.value(GraphStateKey.TRACE_ID.getKey(), "");
                List<TurnVO> turns = reactContext.getTurns();
                TurnVO turn = TurnVO.builder()
                        .traceId(traceId)
                        .agent(subAgentName)
                        .round(turns.size() + 1)
                        .plan(planDetail)
                        .conclusion(conclusion)
                        .build();
                turns.add(turn);
                resultMap.put(GraphStateKey.SUB_AGENT_REACT_MEMORY.getKey(), reactContext);
            }

            AgentMDC.setEventType(AgentEventType.WORKER_OUTPUT.getType());
            AgentMDC.setWorkerConclusion(conclusion);
            log.info("[V3-Sub] {} worker 完成 | success={} | conclusion={}", subAgentName, workerDetail.isSuccess(), conclusion);

            AgentMDC.clearContentContext();
            return resultMap;
        };
        return AsyncNodeAction.node_async(action);
    }

    /**
     * 子 Agent视角中，当前规划执行完成的节点
     */
    public AsyncNodeAction finishNode() {
        NodeAction action = state -> {
            log.info("[V3-Sub] finish 节点");
            return Map.of();
        };
        return AsyncNodeAction.node_async(action);
    }

    /**
     * 子Agent超过最大plan→worker循环时优雅收尾：把worker累积结果塞进conclusion
     */
    public AsyncNodeAction overMaxLoopTimesNode() {
        NodeAction action = state -> {
            String subAgentName = state.value(GraphStateKey.SUB_AGENT_NAME.getKey(), "");
            String workerConclusion = state.value(GraphStateKey.WORKER_CONCLUSION.getKey(), "");
            log.warn("[V3-Sub] {} 触及最大循环，进入 overMaxLoopTimes 收尾", subAgentName);

            Map<String, Object> delta = new HashMap<>();
            if (workerConclusion.isBlank()) {
                delta.put(GraphStateKey.ORCHESTRATOR_AGENT_CONCLUSION.getKey(),
                        "任务超过最大执行轮次，未能获取有效结果，建议简化需求后重试");
            } else {
                delta.put(GraphStateKey.ORCHESTRATOR_AGENT_CONCLUSION.getKey(), workerConclusion);
            }
            return delta;
        };
        return AsyncNodeAction.node_async(action);
    }
}
