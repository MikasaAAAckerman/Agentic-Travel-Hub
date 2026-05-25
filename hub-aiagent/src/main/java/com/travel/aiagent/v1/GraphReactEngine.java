package com.travel.aiagent.v1;

import com.travel.aiagent.common.constant.NodeEnum;
import com.travel.aiagent.v1.edges.GraphEdge;
import com.travel.aiagent.v1.nodes.GraphNodes;
import com.travel.aiagent.v1.state.GraphStateVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@Slf4j
public class GraphReactEngine {

    @Resource
    private GraphNodes nodes;
    @Resource
    private GraphEdge graphEdge;

    private final int maxLoopCount = 15;

    public String execute(String prompt, String history, Consumer<String> progress) {

        // 初始化全图流转的公文包
        GraphStateVO graphStateVO = new GraphStateVO();
        graphStateVO.setFinished(false);
        graphStateVO.setLoopCount(0);
        graphStateVO.setChatMemory(history);
        graphStateVO.setUserInput(prompt);

        // 初始化流转节点
        String nodeName = NodeEnum.PLANNER_NODE.getNodeName();

        while (!graphStateVO.isFinished() && graphStateVO.getLoopCount() < maxLoopCount) {

            // 对每条边定义Node动作
            if (nodeName.equals(NodeEnum.PLANNER_NODE.getNodeName())) {
                // Edge：规划步骤 -> Node：规划步骤
                nodes.plannerNode(graphStateVO);
                progress.accept(" 正在思考下一步行动 \n ");
            } else if (nodeName.equals(NodeEnum.WORKER_NODE.getNodeName())) {
                // Edge：执行步骤 -> Node：执行步骤
                nodes.workerNode(graphStateVO);
                progress.accept(" 正在派遣工具执行任务 \n ");
            } else if (nodeName.equals(NodeEnum.MEMORY_PRUNE_NODE.getNodeName())) {
                // Edge：记忆压缩 -> Node：记忆压缩
                nodes.memoryPruneNode(graphStateVO);
                progress.accept(" 正在输出结果并压缩记忆 \n ");
            } else if (nodeName.equals(NodeEnum.CLARIFY_NODE.getNodeName())) {
                return "";
            }
            nodeName = graphEdge.chooseNextNode(graphStateVO);
        }

        return graphStateVO.getCurrentPlanDetailVO().getConclusion();

    }
}