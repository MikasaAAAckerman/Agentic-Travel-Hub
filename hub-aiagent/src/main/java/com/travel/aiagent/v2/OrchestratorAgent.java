package com.travel.aiagent.v2;

import com.travel.aiagent.common.constant.PlanActionEnum;
import com.travel.aiagent.common.core.planner.DeepSeekPlannerService;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.prompt.SystemPrompt;
import com.travel.aiagent.common.memory.ShortTermMemory;
import com.travel.aiagent.v2.agents.BaseTravelAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 子Agent调度器
 */
@Slf4j
@Service
public class OrchestratorAgent {

    @Resource
    DeepSeekPlannerService deepSeekPlannerService;

    /**
     * 根据任务调度器返回的AgentName，动态调用合适的子Agent
     */
    private final Map<String, BaseTravelAgent> subAgentNameAndAgentMap = new HashMap<>();

    private final int maxLoopTimes = 10;

    /**
     * 拼接 “你有如下子Agent可以使用：{AgentnName}: {AgentDescription}; {AgentnName}: {AgentDescription}.... ”
     */
    private String agentNameAndDescription = "";
    @Autowired
    private ShortTermMemory shortTermMemory;

    public OrchestratorAgent(List<BaseTravelAgent> travelAgents) {
        for (BaseTravelAgent subAgent : travelAgents) {
            subAgentNameAndAgentMap.put(subAgent.name(), subAgent);
            agentNameAndDescription = agentNameAndDescription + subAgent.name() + ": " + subAgent.description() + "; ";
        }
    }

    /**
     * 执行器
     *
     * @param userInput  用户输入
     * @param chatMemory 会话上下文
     * @param progress   进度条
     * @return
     */
    public String execute(String userInput, String chatMemory, Consumer<String> progress) {

        if (StringUtils.isEmpty(chatMemory)) {
            chatMemory = "用户说：" + userInput + "。\n ";
        }

        int currentLoopTimes = 1;

        while (currentLoopTimes < maxLoopTimes) {

            // 子Agent调度大师的 system prompt
            String systemPrompt = SystemPrompt.buildOrchestratorSystemPrompt(agentNameAndDescription);

            // 调用Planner，决定下一步走向
            PlanDetailVO planDetailVO = deepSeekPlannerService.doOrchestratorAgentPlan(userInput, chatMemory, systemPrompt);

            // 记录当前任务规划与执行结果的详细信息，放入chatMemory中
            String currentLog = "第" + currentLoopTimes + "步的规划是:" + planDetailVO.getPlanDetail();
            currentLog = currentLog + "需要调用的SubAgent为：" + planDetailVO.getSubAgentName();
            currentLog = currentLog + "\n ";

            // 判断状态，确认下一个流转节点
            if (planDetailVO.getAction().equals(PlanActionEnum.FINISH.getType()) || planDetailVO.getAction().equals(PlanActionEnum.CLARIFY.getType())) {

                // 如果认为任务整体全部完成了或者需要用户补全信息，那么就返回当前结论
                currentLog = currentLog + " 任务执行结束，结论为 " + planDetailVO.getConclusion();
                currentLog = currentLog + " \n ";
                log.info(currentLog);
                return planDetailVO.getConclusion();

            } else if (planDetailVO.getAction().equals(PlanActionEnum.SUB_AGENT_CALL.getType())) {

                // 如果当前任务需要派发给子Agent执行，那么就先获取到实际需要调度的子Agent
                BaseTravelAgent subAgent = subAgentNameAndAgentMap.get(planDetailVO.getSubAgentName());

                if (subAgent != null) {

                    // 让子Agent调用工具执行计划
                    String conclusion = subAgent.execute(planDetailVO.getPlanDetail(), chatMemory, progress);
                    currentLog = planDetailVO.getSubAgentName() + "规划完成，结论是:" + conclusion;

                } else {
                    currentLog = currentLog + "没有这个subAgent，请重新规划方案";
                    currentLog = currentLog + "\n";
                }
                log.info(currentLog);
                chatMemory = chatMemory + currentLog;

            } else {

                // 进入了未知状态
                return "任务规划出现问题，请重试";

            }
            currentLoopTimes++;
        }

        return "任务拆分与子Agent超过最大次数，请重试";
    }


}
