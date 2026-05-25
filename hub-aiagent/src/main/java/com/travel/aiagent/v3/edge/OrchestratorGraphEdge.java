package com.travel.aiagent.v3.edge;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.travel.aiagent.common.constant.GraphStateKey;
import com.travel.aiagent.common.constant.PlanActionEnum;
import com.travel.aiagent.v3.agents.BaseTravelGraphAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 以GraphState中的Action定义边
 */
@Service
@Slf4j
public class OrchestratorGraphEdge {

    @Resource
    private Map<String, BaseTravelGraphAgent> subGraphAgentsMap;

    @Value("${max-loop-times.orchestrator-to-subagent}")
    private int maxLoopTimes;

    /**
     * 调度者Agent Graph 条件边规则设计：
     * <p>
     * 调度者planner认为任务完成 -> finish
     * 调度者planner认为任务完成需要user-in-loop流程 -> clarify
     * 调度者Planner任务需要子Agent帮忙执行当前任务 -> subAgentName
     * 调度者Planner出现幻觉，没有给出或者给出了不存在的Action -> planner
     * 调度者Planner还剩最后一次call子Agent，提前进入总结流程 -> overMaxLoopTimes
     */
    public AsyncEdgeAction afterPlanner() {
        EdgeAction edgeAction = state -> {

            int currentLoopTimes = state.value(GraphStateKey.LOOP_TIMES.getKey(), 0);
            log.info(" currentLoopTimes In OrchestratorEdge {} " , currentLoopTimes );
            if (currentLoopTimes >= maxLoopTimes - 1) {
                return "overMaxLoopTimes";
            }

            state.data().put(GraphStateKey.LOOP_TIMES.getKey(), currentLoopTimes+1);

            // 获得GraphState的Action
            String actionStr = state.value(GraphStateKey.ACTION.getKey(), "");

            // 幻觉：没有给出Action
            if (StringUtils.isEmpty(actionStr)) {
                // 如果是无条件态，说明刚刚初始化，那么就去到planner节点
                return "planner";
            }

            if (actionStr.equals(PlanActionEnum.FINISH.getType())) {
                return "finish";
            }
            if (actionStr.equals(PlanActionEnum.CLARIFY.getType())) {
                return "clarify";
            }

            if (PlanActionEnum.SUB_AGENT_CALL.getType().equals(actionStr)) {
                String subAgentName = state.value(GraphStateKey.SUB_AGENT_NAME.getKey(), "");
                // 如果子 Agent 名在注册表中，路由到它；不存在则代表Planner派发任务时产生幻觉，扔回Planner重新规划
                return subGraphAgentsMap.containsKey(subAgentName) ? subAgentName : "planner";
            }

            // 没有匹配到任何一个if，说明LLM出现幻觉，ACTION在乱来，那么回到Planner重新思考
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
        map.put("finish", "finish");
        map.put("clarify", "clarify");
        map.put("overMaxLoopTimes", "overMaxLoopTimes");
        for (String name : subGraphAgentsMap.keySet()) {
            map.put(name, name);
        }
        return map;
    }


}
