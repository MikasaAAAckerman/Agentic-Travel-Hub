package com.travel.aiagent.v3.agents;

import com.travel.aiagent.v3.graph.SubAgentGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RouteGraphAgent extends BaseTravelGraphAgent {

    public RouteGraphAgent(SubAgentGraph subAgentGraph) {
        super(subAgentGraph);
    }

    @Override
    public String name() { return "RouteAgent"; }

    @Override
    public String description() {
        return "出行路线专家：驾车/公交/步行路线规划、实时路况、网约车估价、过路费计算";
    }
}
