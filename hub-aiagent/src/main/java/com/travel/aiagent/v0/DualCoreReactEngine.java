package com.travel.aiagent.v0;

import com.travel.aiagent.common.constant.PlanActionEnum;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.WorkDetailVO;
import com.travel.aiagent.common.core.planner.DeepSeekPlannerService;
import com.travel.aiagent.common.core.worker.QwenWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class DualCoreReactEngine {

    private final DeepSeekPlannerService planner;

    private final QwenWorkerService worker;

    /**
     * 以ReAct模式将输入转为可执行的计划输出
     */
    public String execute(String userInput, String historyContext, Consumer<String> progressListener) {
        // 流程设计：
        // 1、接受前端userInput和之前会话的chatMemory(若有)
        // 2、将userInput与chatMemory丢入deepseekPlanner做任务计划
        // 3、获取每一次的任务规划结果，丢进qwenWorker调用做任务执行
        // 4、将qwenWorker对当前任务规划的执行结果，再次扔进deepseekPlanner，让ds带上前一步的计划结果，继续规划下一步
        // 5、循环执行2->3->4->2，直到planner觉得在 “针对当前用户的需求，我拆分的步骤已经”

        String result = "";
        log.info("[Engine] ReAct 引擎启动 | inputLength={}", userInput.length());
        // 由于需要进行 思考 -> 返回思考并执行工具的结果 -> 带着结果继续思考 -> 继续执行下一步工具获取结果 -> 带着结果思考的loop，为了避免爆token，设定循环轮次计数
        int maxLoopTimes = 15;
        int currentLoopTimes = 1;
        String preWorkResult = historyContext;
        while (currentLoopTimes <= maxLoopTimes) {

            if (progressListener != null) {
                progressListener.accept("🧠 [第" + currentLoopTimes + "步]：DeepSeek 正在拼命推演计划中...\n");
            }
            log.info("[Engine] 执行第 {}/{} 次迭代", currentLoopTimes, maxLoopTimes);
            // 将输入交给deepseek大模型，让它思考下一步计划具体需要做什么
            PlanDetailVO planDetailVO = planner.doTravelPlan(userInput, preWorkResult);
            // 给前端输出一下思考过程
            if (progressListener != null && planDetailVO.getThought() != null) {
                progressListener.accept("💡 思考过程：" + planDetailVO.getThought() + "\n");
            }
            // 判断一下当前计划执行到了哪个步骤
            if (planDetailVO.getAction().equals(PlanActionEnum.FINISH.getType())
                    || planDetailVO.getAction().equals(PlanActionEnum.CLARIFY.getType())) {
                // 计划执行完成，或者需要补全信息进行进一步处理
                log.info("[Engine] 任务规划完成 | action={}", planDetailVO.getAction());
                String conclusion = planDetailVO.getConclusion() != null ? planDetailVO.getConclusion() : "";
                String detail = planDetailVO.getPlanDetail() != null ? planDetailVO.getPlanDetail() : "";
                result = conclusion + "\n\n" + detail;
                return result;
            } else if (planDetailVO.getAction().equals(PlanActionEnum.TOOL_CALL.getType())) {
                // 向前端输出一下准备调用的工具
                if (progressListener != null) {
                    progressListener.accept("🛠️ 准备调用工具：" + planDetailVO.getPlanDetail() + "\n");
                }
                // 完成当前计划需要调用工具返回结果，将结果拿走进一步思考
                log.info("[Engine] 准备开始执行工具调用 | tool={}", planDetailVO.getPlanDetail());
//                WorkDetailVO workDetailVO = worker.doWork(planDetailVO);
                WorkDetailVO workDetailVO = worker.doWorkWithRag(planDetailVO);

                // 向前端输出一下工具执行结果
                if (progressListener != null) {
                    progressListener.accept("✅ 工具执行结果：" + workDetailVO.getConclusion() + "\n\n");
                }

                // 拼接思考内容和思考结果，扔给deepseek继续规划任务，直到finished
                preWorkResult = preWorkResult + """
                        第 %d 次思考：%s
                        执行成功与否：%s
                        结果为：%s。
                        """.formatted(currentLoopTimes,
                        planDetailVO.getThought(),
                        workDetailVO.isSuccess(),
                        workDetailVO.getConclusion());

            } else {
                // 未知状态，直接告诉deepseek思考失败
                log.error("[Engine] DeepSeek 返回未知状态 | action={}", planDetailVO.getAction());
                preWorkResult = preWorkResult + """
                        第 %d 次思考：%s
                        执行成功与否：%s
                        请重新思考。
                        """.formatted(currentLoopTimes,
                        planDetailVO.getThought(),
                        false);

            }

            currentLoopTimes++;
        }
        log.warn("[Engine] 达到最大迭代次数 {} 次，任务未完成", maxLoopTimes);
        return result;
    }

}
