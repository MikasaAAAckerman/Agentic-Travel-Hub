package com.travel.aiagent.v2.agents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 天气穿搭领域 Agent。
 */
@Slf4j
@Component("weatherAgent")
public class WeatherAgent extends BaseTravelAgent {

    @Override
    public String name() { return "WeatherAgent"; }

    @Override
    public String description() { return "天气穿搭专家：实时天气查询、紫外线防晒、降水雨具、湿度面料推荐、风寒指数、昼夜温差叠穿"; }


}
