package com.travel.aiagent.v2.agents;

import com.travel.aiagent.common.constant.PlanActionEnum;
import com.travel.aiagent.common.core.planner.DeepSeekPlannerService;
import com.travel.aiagent.common.core.worker.QwenWorkerService;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.WorkDetailVO;
import com.travel.aiagent.v2.ITravelAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

@Slf4j
public class BaseTravelAgent implements ITravelAgent {

    private final int maxLoopTimes = 10;

    @Resource
    private DeepSeekPlannerService plannerService;

    @Resource
    private QwenWorkerService workerService;

    @Override
    public String name() {
        return "";
    }

    @Override
    public String description() {
        return "";
    }

    @Override
    public String execute(String userInput, String chatMemory, Consumer<String> progress) {

        int currentLoopTimes = 1;

        if (StringUtils.isEmpty(chatMemory)) {
            chatMemory = "用户说：" + userInput + "。\n ";
        }

        while (currentLoopTimes < maxLoopTimes) {

            // 调用Planner，将当前任务细分
            PlanDetailVO planDetailVO = plannerService.doTravelPlan(userInput, chatMemory);

            // 记录当前任务规划与执行结果的详细信息，放入chatMemory中
            String currentLog = "第" + currentLoopTimes + "步的规划是:" + planDetailVO.getPlanDetail();
            currentLog = currentLog + "\n ";

            // 判断状态，确认下一个流转节点
            if (planDetailVO.getAction().equals(PlanActionEnum.FINISH.getType()) || planDetailVO.getAction().equals(PlanActionEnum.CLARIFY.getType())) {

                // 如果认为任务整体全部完成了或者需要用户补全信息，那么就返回当前结论
                currentLog = currentLog + " 任务执行结束，结论为 " + planDetailVO.getConclusion();
                currentLog = currentLog + " \n ";
                log.info(currentLog);
                return planDetailVO.getConclusion();

            } else if (planDetailVO.getAction().equals(PlanActionEnum.TOOL_CALL.getType())) {

                // 如果当前任务需要派发给子Agent执行，那么就先获取到实际需要调度的子Agent
                WorkDetailVO workDetailVO = workerService.doWorkWithRag(planDetailVO);
                if (workDetailVO.isSuccess()) {
                    currentLog = currentLog + "当前步骤TOOL_CALL成功，结果为：" + workDetailVO.getConclusion();
                } else {
                    currentLog = currentLog + "当前步骤TOOL_CALL失败，原因是：" + workDetailVO.getConclusion() + "请重新规划当前任务";
                }
                currentLog = currentLog + "\n ";
                chatMemory = chatMemory + currentLog;

            } else {

                // 进入了未知状态
                return "任务规划出现问题，请重试";

            }

            log.info(currentLog);

            currentLoopTimes++;
        }

        return "任务拆分与执行超过最大次数，请重试";
    }
}
