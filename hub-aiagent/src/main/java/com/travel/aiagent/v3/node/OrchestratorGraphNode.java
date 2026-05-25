package com.travel.aiagent.v3.node;

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.travel.aiagent.common.constant.GraphStateKey;
import com.travel.aiagent.common.core.planner.DeepSeekPlannerService;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.prompt.SystemPrompt;
import com.travel.aiagent.common.memory.ShortTermMemory;
import com.travel.aiagent.v3.agents.BaseTravelGraphAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Graph 模式 Node初始化类
 */
@Service
@Slf4j
public class OrchestratorGraphNode {

    @Resource
    private DeepSeekPlannerService plannerService;

    @Resource
    private Map<String, BaseTravelGraphAgent> subGraphAgentsMap;

    @Resource
    private ShortTermMemory shortTermMemory;

    /**
     * 调度者Agent视角中，将用户输入转为计划的node
     */
    public AsyncNodeAction plannerNode() {
        NodeAction action = state -> {
            String userInput = state.value(GraphStateKey.USER_INPUT.getKey(), "");
            String userId = state.value(GraphStateKey.USER_ID.getKey(), "");
            String chatId = state.value(GraphStateKey.CHAT_ID.getKey(), "");
            String chatMemory = shortTermMemory.getMemoryByUserIdAndChatId(userId, chatId);
            Integer loopTimes = state.value(GraphStateKey.LOOP_TIMES.getKey(), 0);

            log.info("[V3] Orchestrator planner 节点 | 第{}轮调度", loopTimes);

            StringBuilder subAgentDescriptionBuilder = new StringBuilder();
            for (String key : subGraphAgentsMap.keySet()) {
                subAgentDescriptionBuilder.append(key + ": " + subGraphAgentsMap.get(key).description() + ";\n");
            }
            String systemPrompt = SystemPrompt.buildOrchestratorSystemPrompt(subAgentDescriptionBuilder.toString());
            PlanDetailVO planDetailVO = plannerService.doOrchestratorAgentPlan(userInput, chatMemory, systemPrompt);

            // 获取计划后的结果，扔进GraphState中，交给Edge判断进到哪个节点
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put(GraphStateKey.ACTION.getKey(), planDetailVO.getAction());
            resultMap.put(GraphStateKey.SUB_AGENT_NAME.getKey(), planDetailVO.getSubAgentName());
            resultMap.put(GraphStateKey.ORCHESTRATOR_AGENT_PLAN_DETAIL.getKey(), planDetailVO.getPlanDetail());
            resultMap.put(GraphStateKey.ORCHESTRATOR_AGENT_CONCLUSION.getKey(), planDetailVO.getConclusion());
            resultMap.put(GraphStateKey.LOOP_TIMES.getKey(), loopTimes + 1);

            log.info("[V3] Orchestrator planner → ACTION={}, SUB_AGENT_NAME={}", planDetailVO.getAction(), planDetailVO.getSubAgentName());
            return resultMap;
        };
        return AsyncNodeAction.node_async(action);
    }

    /**
     * 调度者Agent中，将分步计划交给subAgent执行的node
     */
    public AsyncNodeAction subAgentNode(BaseTravelGraphAgent baseTravelGraphAgent) {
        NodeAction action = state -> {
            String planDetail = state.value(GraphStateKey.ORCHESTRATOR_AGENT_PLAN_DETAIL.getKey(), "");
            String userId = state.value(GraphStateKey.USER_ID.getKey(), "");
            String chatId = state.value(GraphStateKey.CHAT_ID.getKey(), "");
            String subAgentName = state.value(GraphStateKey.SUB_AGENT_NAME.getKey(), "");

            log.info("[V3] {} 节点 | plan={}", baseTravelGraphAgent.name(), planDetail);

            String result = baseTravelGraphAgent.execute(planDetail, userId, chatId, null);
            shortTermMemory.addAgentTalking(userId, chatId, subAgentName + " 执行完成，结论：" + result);

            log.info("[V3] {} → 完成", baseTravelGraphAgent.name());
            return Map.of();
        };
        return AsyncNodeAction.node_async(action);
    }

    /**
     * 调度者Agent中，需要用户补充信息时走此节点
     */
    public AsyncNodeAction clarifyNode() {
        NodeAction action = state -> {
            log.info("[V3] clarify 节点");
            return Map.of();
        };
        return AsyncNodeAction.node_async(action);
    }

    /**
     * 调度者Agent中，执行完所有的计划后，总结结论返回给前端的node
     */
    public AsyncNodeAction finishNode() {
        NodeAction action = state -> {
            log.info("[V3] finish 节点");
            return Map.of();
        };
        return AsyncNodeAction.node_async(action);
    }

    /**
     * 编排者超过最大调度轮次时收尾：把ShortTermMemory累积的结论拼成最终回复
     */
    public AsyncNodeAction overMaxLoopTimes() {
        NodeAction action = state -> {

            String userId = state.value(GraphStateKey.USER_ID.getKey(), "");
            String chatId = state.value(GraphStateKey.CHAT_ID.getKey(), "");
            log.warn("[V3] Orchestrator 触及最大调度轮次，进入 overMaxLoopTimes 收尾");

            String memory = shortTermMemory.getMemoryByUserIdAndChatId(userId, chatId);
            String conclusion;
            if (memory.isBlank() || "暂无对话记忆，这是你们的初次对话".equals(memory)) {
                conclusion = "抱歉喵，任务规划超过最大轮次，未能完成全部规划，请简化需求后重试~";
            } else {
                conclusion = memory;
            }

            Map<String, Object> delta = new HashMap<>();
            delta.put(GraphStateKey.ORCHESTRATOR_AGENT_CONCLUSION.getKey(), conclusion);
            return delta;
        };
        return AsyncNodeAction.node_async(action);
    }
}
