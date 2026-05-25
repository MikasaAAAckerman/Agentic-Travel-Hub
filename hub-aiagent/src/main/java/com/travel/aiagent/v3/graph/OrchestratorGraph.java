package com.travel.aiagent.v3.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.travel.aiagent.v3.agents.BaseTravelGraphAgent;
import com.travel.aiagent.v3.edge.OrchestratorGraphEdge;
import com.travel.aiagent.v3.node.OrchestratorGraphNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class OrchestratorGraph {

    @Resource
    private Map<String, BaseTravelGraphAgent> subGraphAgentsMap;

    @Resource
    private OrchestratorGraphEdge orchestratorGraphEdge;

    @Resource
    private OrchestratorGraphNode orchestratorGraphNode;

    @Value("${max-loop-times.orchestrator-to-subagent}")
    private int maxLoopTimes;

    public CompiledGraph buildGraph() {
        log.info(" Graph 开始 buildGraph ");
        try {
            // 先给Graph加上默认Node和Edge
            StateGraph graph = new StateGraph()
                    // 加入planner边对应的节点
                    .addNode("planner", orchestratorGraphNode.plannerNode())
                    // 加入finish边对应的节点
                    .addNode("finish", orchestratorGraphNode.finishNode())
                    .addNode("clarify", orchestratorGraphNode.clarifyNode())
                    .addNode("overMaxLoopTimes",orchestratorGraphNode.overMaxLoopTimes())
                    // 加入从planner节点出来后，以每个subAgentName定义边
                    .addConditionalEdges("planner", orchestratorGraphEdge.afterPlanner(), orchestratorGraphEdge.routingMap())
                    .addEdge("finish", StateGraph.END)
                    .addEdge("clarify", StateGraph.END)
                    .addEdge("overMaxLoopTimes", StateGraph.END)
                    .addEdge(StateGraph.START, "planner");

            // 再给Graph加上动态subAgent节点和Edge
            for (Map.Entry<String, BaseTravelGraphAgent> entry : subGraphAgentsMap.entrySet()) {
                String name = entry.getKey();
                BaseTravelGraphAgent subAgent = entry.getValue();
                // 加入每个subAgentName边对应的节点
                graph.addNode(name, orchestratorGraphNode.subAgentNode(subAgent));
                // subAgent完成后，走planner条件边去到调度Agent，让调度Agent继续规划
                graph.addEdge(name, "planner");
            }

            // 完成最终创建
            log.info(" Graph 结束 buildGraph ");
            return graph.compile(CompileConfig.builder().recursionLimit(maxLoopTimes * 3).build());

        } catch (GraphStateException e) {
            throw new RuntimeException("图编译失败", e);
        }
    }

}
