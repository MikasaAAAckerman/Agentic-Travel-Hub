package com.travel.aiagent.v3.agents;

import com.travel.aiagent.v3.graph.SubAgentGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FoodGraphAgent extends BaseTravelGraphAgent {

    public FoodGraphAgent(SubAgentGraph subAgentGraph) { super(subAgentGraph); }

    @Override
    public String name() { return "FoodAgent"; }

    @Override
    public String description() { return "美食探索专家：附近餐厅搜索、地道美食推荐、深夜食堂、宠物友好/亲子餐厅、Brunch咖啡馆"; }
}
