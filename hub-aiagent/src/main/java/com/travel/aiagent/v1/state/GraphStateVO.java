package com.travel.aiagent.v1.state;

import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.WorkDetailVO;
import lombok.Data;

/**
 * 贯穿整个图引擎的全局状态对象，用于记录上下文
 */
@Data
public class GraphStateVO {

    /**
     * 本次工具执行结果
     */
    private WorkDetailVO workDetailVO;

    /**
     * Planner做出的最新计划
     */
    private PlanDetailVO currentPlanDetailVO;

    /**
     * 本次任务的上下文短期会话记忆
     */
    private String chatMemory;

    /**
     * 当前 doPlan -> doWork 的循环次数，超过最大次数跳出循环，避免Token消耗
     */
    private int loopCount;

    /**
     * 用户输入文字
     */
    private String userInput;

    /**
     * 计划是否执行结束了
     */
    private boolean isFinished;

}