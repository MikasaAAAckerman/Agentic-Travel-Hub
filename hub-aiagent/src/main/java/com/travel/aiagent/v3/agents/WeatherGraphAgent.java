package com.travel.aiagent.v3.agents;

import com.travel.aiagent.v3.graph.SubAgentGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeatherGraphAgent extends BaseTravelGraphAgent {

    public WeatherGraphAgent(SubAgentGraph subAgentGraph) { super(subAgentGraph); }

    @Override
    public String name() { return "WeatherAgent"; }

    @Override
    public String description() { return "天气穿搭专家：实时天气查询、紫外线防晒、降水雨具、湿度面料推荐、风寒指数、昼夜温差叠穿"; }

    @Override
    protected String domainInstruction() {
        return "只采信出行日期范围内的预报；超出预报窗口的日期必须说明是气候估计而非预报。";
    }
}
