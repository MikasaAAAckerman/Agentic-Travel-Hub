package com.travel.aiagent.v3.component;

import com.alibaba.fastjson2.JSON;
import com.travel.aiagent.v3.agents.BaseTravelGraphAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InitComponet {

    @Bean
    public Map<String, BaseTravelGraphAgent> subGraphAgentsMap(List<BaseTravelGraphAgent> travelGraphAgents) {
        log.info("开始初始化subGraphAgentsMap");
        Map<String, BaseTravelGraphAgent> result = new HashMap<>();
        for (BaseTravelGraphAgent subAgent : travelGraphAgents) {
            result.put(subAgent.name(), subAgent);
        }
        log.info("subGraphAgentsMap初始化完成，结果为 -> {}", JSON.toJSONString(result));
        return result;
    }

}
