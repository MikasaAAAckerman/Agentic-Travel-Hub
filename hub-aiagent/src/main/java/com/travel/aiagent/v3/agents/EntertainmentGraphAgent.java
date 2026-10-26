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

    @Override
    protected String domainInstruction() {
        return "需实名预约的场馆必须给出预约提示，不能把未预约的场馆当作可直接前往；"
                + "若用户提出了行动能力方面的要求，避免推荐需要大量步行、爬坡或长时间站立的地点。";
    }
}
