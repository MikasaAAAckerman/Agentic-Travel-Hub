package com.travel.aiagent.v3.edge;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.travel.aiagent.common.constant.GraphStateKey;
import com.travel.aiagent.common.constant.PlanActionEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 子Agent内部Graph的边——planner→worker/finish 路由。
 */
@Slf4j
@Service
public class SubAgentGraphEdge {

    @Value("${max-loop-times.planner-to-worker}")
    private int maxLoopTimes;

    /**
     * 调度者Agent Graph 条件边规则设计：
     * <p>
     * 子Agent的planner认为任务完成或者需要user-in-loop流程 -> finish
     * 子Agent的planner任务需要调用工具帮忙执行当前任务 -> worker
     * 子Agent的planner出现幻觉，没有给出或者给出了不存在的Action -> planner
     * 子Agent的planner还剩最后一次call worker，提前去总结 -> overMaxLoopTimes
     */
    public AsyncEdgeAction afterPlanner() {
        EdgeAction edgeAction = state -> {

            int currentLoopTimes = state.value(GraphStateKey.LOOP_TIMES.getKey(), 0);
            log.info(" currentLoopTimes In subEdge {} " , currentLoopTimes );
            if (currentLoopTimes >= maxLoopTimes - 1) {
                return "overMaxLoopTimes";
            }

            String actionStr = state.value(GraphStateKey.ACTION.getKey(), "");
            if (actionStr == null || actionStr.isEmpty()) return "planner";

            if (PlanActionEnum.FINISH.getType().equals(actionStr)
                    || PlanActionEnum.CLARIFY.getType().equals(actionStr)) {
                return "finish";
            }
            if (PlanActionEnum.TOOL_CALL.getType().equals(actionStr)) {
                return "worker";
            }
            return "planner";
        };
        return AsyncEdgeAction.edge_async(edgeAction);
    }

    /**
     * 合法边，不在routingMap中的边全不允许访问
     */
    public Map<String, String> routingMap() {
        Map<String, String> map = new HashMap<>();
        map.put("planner", "planner");
        map.put("worker", "worker");
        map.put("finish", "finish");
        map.put("overMaxLoopTimes", "overMaxLoopTimes");
        return map;
    }


}
