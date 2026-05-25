package com.travel.aiagent.v1.edges;

import com.travel.aiagent.common.constant.NodeEnum;
import com.travel.aiagent.v1.state.GraphStateVO;
import org.springframework.stereotype.Component;

@Component
public class GraphEdge {

    /**
     * 通过状态中的计划类型找到对应的node节点
     *
     * @param graphStateVO
     * @return
     */
    public String chooseNextNode(GraphStateVO graphStateVO) {
        // 通过Planner的计划行为找到对应的节点枚举值
        NodeEnum node = NodeEnum.getByPlanType(graphStateVO.getCurrentPlanDetailVO().getAction());
        if (node != null) {
            // 如果找到了对应节点，那么返回对应node的枚举值
            return node.getNodeName();
        } else {
            // 如果没有找到对应的节点，说明Planner出现了幻觉，强制重新执行计划
            return NodeEnum.PLANNER_NODE.getNodeName();
        }
    }

}