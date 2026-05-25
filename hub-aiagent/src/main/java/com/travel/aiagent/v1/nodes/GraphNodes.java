package com.travel.aiagent.v1.nodes;

import com.alibaba.fastjson2.JSON;
import com.travel.aiagent.common.constant.PlanActionEnum;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.core.planner.DeepSeekPlannerService;
import com.travel.aiagent.common.core.worker.QwenWorkerService;
import com.travel.aiagent.common.domain.WorkDetailVO;
import com.travel.aiagent.v1.state.GraphStateVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 定义每个状态边上的节点
 */
@Component
@Slf4j
public class GraphNodes {

    @Resource
    DeepSeekPlannerService plannerService;

    @Resource
    QwenWorkerService workerService;

    /**
     * 计划执行节点
     *
     * @param graphStateVO
     * @return
     */
    public GraphStateVO plannerNode(GraphStateVO graphStateVO) {
        log.info(" 进入计划节点plannerNode ");

        // 开始执行计划
        PlanDetailVO planDetailVO = plannerService.doTravelPlan(graphStateVO.getUserInput(), graphStateVO.getChatMemory());

        // planner根据chatMemory拆解步骤 -> worker完成步骤放进chatMemory -> planner根据chatMemory拆解步骤 算一次loop，所以计数器+1
        graphStateVO.setLoopCount(graphStateVO.getLoopCount() + 1);

        // planner根据userInput + chatMemory做出的最新计划
        graphStateVO.setCurrentPlanDetailVO(planDetailVO);

        // 设置一下计划的执行状态
        graphStateVO.setFinished(planDetailVO.getAction().equals(PlanActionEnum.FINISH.getType()));

        return graphStateVO;
    }

    /**
     * 工具执行节点
     *
     * @param graphStateVO
     * @return
     */
    public GraphStateVO workerNode(GraphStateVO graphStateVO) {
        log.info(" 进入工具执行节点workerNode ");
        String currentChatLog = String.format("第%d步，需要执行的计划是：%s，",
                graphStateVO.getLoopCount(),
                graphStateVO.getCurrentPlanDetailVO().getPlanDetail());
        log.info(" 当前的计划是 -> {}", JSON.toJSONString(graphStateVO.getCurrentPlanDetailVO()));
        // 根据计划执行工具
        WorkDetailVO workDetailVO = workerService.doWorkWithRag(graphStateVO.getCurrentPlanDetailVO());
        currentChatLog = currentChatLog + "执行的结果为：" + workDetailVO.getConclusion() + "。";
        // 历史memory + 当前工具结果生成最新的chatMemory
        String chatMemory = graphStateVO.getChatMemory() + currentChatLog;
        log.info(" 当前计划的计划的结果是 -> {}", currentChatLog);
        graphStateVO.setChatMemory(chatMemory);
        return graphStateVO;
    }

    /**
     * 记忆压缩节点
     *
     * @param graphStateVO
     * @return
     */
    public GraphStateVO memoryPruneNode(GraphStateVO graphStateVO) {
        // todo:先不浪费token在记忆压缩上，到时候再补
        return graphStateVO;
    }

}
