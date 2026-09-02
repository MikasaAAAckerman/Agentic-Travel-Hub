package com.travel.aiagent.common.core.planner;

import com.alibaba.fastjson.JSON;
import com.travel.aiagent.common.constant.AgentEventType;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.prompt.SystemPrompt;
import com.travel.aiagent.common.service.LlmCallLogService;
import com.travel.aiagent.common.utils.AgentMDC;
import com.travel.common.constant.BizException;
import com.travel.common.constant.ServiceResponseTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 共享 Planner 服务 —— 被 v0（DualCoreReactEngine）和 v1（GraphReactEngine）共用。
 * 模型通过 plannerClient 角色注入，具体绑定哪个模型由 yml 的 agent-model.planner 决定。
 */
@Slf4j
@Service
public class PlannerService {

    @Resource
    private ChatClient plannerClient;

    @Resource
    private LlmCallLogService llmCallLogService;

    /**
     * v0/v2 使用：传入用户输入和历史上下文
     */
    public PlanDetailVO doTravelPlan(String userInput, String historyContext) {
        String userMessage = """
                这是用户当前的需求：%s，
                你之前的规划结果是：%s，
                请根据上述需求进行继续规划。
                """
                .formatted(userInput, historyContext);

        AgentMDC.setEventType(AgentEventType.PLANNER_INPUT.getType());
        AgentMDC.setPlannerInput(userMessage);
        log.info("[Planner] DeepSeek 开始任务规划 | userMessage = {}", JSON.toJSONString(userMessage));

        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        PlanDetailVO result = null;

        try {
            result = plannerClient.prompt()
                    .system(SystemPrompt.TRAVEL_PLANNER_SYSTEM_PROMPT)
                    .user(userMessage)
                    .call().entity(PlanDetailVO.class);

            AgentMDC.setEventType(AgentEventType.PLANNER_OUTPUT.getType());
            AgentMDC.setPlannerAction(result.getAction());
            AgentMDC.setPlannerOutput(JSON.toJSONString(result));
            log.info("[Planner] 任务规划完成 | result = {} ", JSON.toJSONString(result));
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            log.error("[Planner] 任务规划失败", e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            llmCallLogService.recordPlannerCall(
                    "Planner",
                    "v0",
                    SystemPrompt.TRAVEL_PLANNER_SYSTEM_PROMPT,
                    userMessage,
                    historyContext,
                    JSON.toJSONString(result),
                    result,
                    duration,
                    success,
                    errorMessage
            );
        }

        AgentMDC.clearContentContext();
        return result;
    }

    /**
     * 子 Agent 专用：不带 SUB_AGENT_CALL 的 Planner
     */
    public PlanDetailVO doSubAgentPlan(String userInput, String historyContext, String subAgentName) {
        String userMessage = """
                这是用户当前的需求：%s，
                你之前的规划结果是：%s，
                请根据上述需求进行继续规划。
                """
                .formatted(userInput, historyContext);

        AgentMDC.setSubAgentName(subAgentName);
        AgentMDC.setEventType(AgentEventType.PLANNER_INPUT.getType());
        AgentMDC.setPlannerInput(userMessage);
        log.info("[Planner-Sub] {} 开始任务规划 | userMessage = {}", subAgentName, JSON.toJSONString(userMessage));

        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        PlanDetailVO result = null;

        try {
            result = plannerClient.prompt()
                    .system(SystemPrompt.TRAVEL_SUB_AGENT_PLANNER_SYSTEM_PROMPT)
                    .user(userMessage)
                    .call().entity(PlanDetailVO.class);

            AgentMDC.setEventType(AgentEventType.PLANNER_OUTPUT.getType());
            AgentMDC.setPlannerAction(result.getAction());
            AgentMDC.setPlannerOutput(JSON.toJSONString(result));
            log.info("[Planner-Sub] {} 任务规划完成 | result = {} ", subAgentName, JSON.toJSONString(result));
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            log.error("[Planner-Sub] {} 任务规划失败", subAgentName, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            llmCallLogService.recordPlannerCall(
                    subAgentName,
                    "v3",
                    SystemPrompt.TRAVEL_SUB_AGENT_PLANNER_SYSTEM_PROMPT,
                    userMessage,
                    historyContext,
                    JSON.toJSONString(result),
                    result,
                    duration,
                    success,
                    errorMessage
            );
        }

        AgentMDC.clearContentContext();
        return result;
    }

    public PlanDetailVO doOrchestratorAgentPlan(String userInput, String historyContext, String systemPrompt) {
        if (StringUtils.isEmpty(systemPrompt)) {
            throw new BizException(ServiceResponseTypeEnum.BAD_REQUEST);
        }
        String userMessage = """
                这是用户当前的需求：%s，
                你之前的规划结果是：%s，
                请根据上述需求进行继续规划下一步任务。
                """
                .formatted(userInput, historyContext);

        AgentMDC.setEventType(AgentEventType.PLANNER_INPUT.getType());
        AgentMDC.setPlannerInput(userMessage);
        log.info("[Planner] 开始任务规划 | userMessage = {}", JSON.toJSONString(userMessage));

        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        PlanDetailVO result = null;

        try {
            result = plannerClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call().entity(PlanDetailVO.class);

            AgentMDC.setEventType(AgentEventType.PLANNER_OUTPUT.getType());
            AgentMDC.setPlannerAction(result.getAction());
            AgentMDC.setSubAgentName(result.getSubAgentName());
            AgentMDC.setPlannerOutput(JSON.toJSONString(result));
            log.info("[Planner] 任务规划完成 | result = {} ", JSON.toJSONString(result));
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            log.error("[Planner] 任务规划失败", e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            llmCallLogService.recordPlannerCall(
                    "OrchestratorGraphAgent",
                    "v3",
                    systemPrompt,
                    userMessage,
                    historyContext,
                    JSON.toJSONString(result),
                    result,
                    duration,
                    success,
                    errorMessage
            );
        }

        AgentMDC.clearContentContext();
        return result;
    }

}
