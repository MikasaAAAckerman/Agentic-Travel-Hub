package com.travel.aiagent.v3.agents;

import com.travel.aiagent.v3.graph.SubAgentGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EntertainmentGraphAgent extends BaseTravelGraphAgent {

    public EntertainmentGraphAgent(SubAgentGraph subAgentGraph) { super(subAgentGraph); }

    @Override
    public String name() { return "EntertainmentAgent"; }

    @Override
    public String description() { return "景点娱乐专家：博物馆预约、主题乐园攻略、夜生活酒吧、集市市场、寺庙祈福、户外徒步、演出影讯"; }
}
