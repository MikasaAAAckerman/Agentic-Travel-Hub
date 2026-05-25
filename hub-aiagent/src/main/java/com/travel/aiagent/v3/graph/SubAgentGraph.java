package com.travel.aiagent.v3.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.travel.aiagent.v3.edge.SubAgentGraphEdge;
import com.travel.aiagent.v3.node.SubAgentGraphNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SubAgentGraph {

    @Resource
    private SubAgentGraphNode subAgentGraphNodes;

    @Resource
    private SubAgentGraphEdge subAgentGraphEdge;

    @Value("${max-loop-times.planner-to-worker}")
    private int maxLoopTimes;

    public CompiledGraph buildGraph() {
        try {
            return new StateGraph()
                    .addNode("planner", subAgentGraphNodes.plannerNode())
                    .addNode("worker", subAgentGraphNodes.workerNode())
                    .addNode("finish", subAgentGraphNodes.finishNode())
                    .addNode("overMaxLoopTimes",subAgentGraphNodes.overMaxLoopTimesNode())
                    .addConditionalEdges("planner", subAgentGraphEdge.afterPlanner(), subAgentGraphEdge.routingMap())
                    .addEdge("worker", "planner")
                    .addEdge("finish", StateGraph.END)
                    .addEdge("overMaxLoopTimes", StateGraph.END)
                    .addEdge(StateGraph.START, "planner")
                    .compile(CompileConfig.builder().recursionLimit(maxLoopTimes * 3).build());
        } catch (GraphStateException e) {
            throw new RuntimeException("子Agent图编译失败", e);
        }
    }

}
