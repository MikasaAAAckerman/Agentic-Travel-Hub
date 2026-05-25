package com.travel.aiagent.common.core.planner;

import com.alibaba.fastjson.JSON;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.prompt.SystemPrompt;
import com.travel.common.constant.BizException;
import com.travel.common.constant.ServiceResponseTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 共享 Planner 服务 —— 被 v0（DualCoreReactEngine）和 v1（GraphReactEngine）共用。
 */
@Slf4j
@Service
public class DeepSeekPlannerService {

    @Resource
    private ChatClient deepseekPlannerClient;

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
        log.info("[Planner] DeepSeek 开始任务规划 | userMessage = {}", JSON.toJSONString(userMessage));
        PlanDetailVO result = deepseekPlannerClient.prompt()
                .system(SystemPrompt.TRAVEL_PLANNER_SYSTEM_PROMPT)
                .user(userMessage)
                .call().entity(PlanDetailVO.class);
        log.info("[Planner] 任务规划完成 | result = {} ", JSON.toJSONString(result));
        return result;
    }

    /** 子 Agent 专用：不带 SUB_AGENT_CALL 的 Planner */
    public PlanDetailVO doSubAgentPlan(String userInput, String historyContext) {
        String userMessage = """
                这是用户当前的需求：%s，
                你之前的规划结果是：%s，
                请根据上述需求进行继续规划。
                """
                .formatted(userInput, historyContext);
        log.info("[Planner-Sub] 子Agent 开始任务规划 | userMessage = {}", JSON.toJSONString(userMessage));
        PlanDetailVO result = deepseekPlannerClient.prompt()
                .system(SystemPrompt.TRAVEL_SUB_AGENT_SYSTEM_PROMPT)
                .user(userMessage)
                .call().entity(PlanDetailVO.class);
        log.info("[Planner-Sub] 子Agent 任务规划完成 | result = {} ", JSON.toJSONString(result));
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
        log.info("[Planner] DeepSeek 开始任务规划 | userMessage = {}", JSON.toJSONString(userMessage));
        PlanDetailVO result = deepseekPlannerClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call().entity(PlanDetailVO.class);
        log.info("[Planner] 任务规划完成 | result = {} ", JSON.toJSONString(result));
        return result;
    }

}
