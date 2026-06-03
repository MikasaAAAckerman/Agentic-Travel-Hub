package com.travel.aiagent.v3.agents;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.travel.aiagent.common.constant.GraphStateKey;
import com.travel.aiagent.common.memory.ShortTermMemory;
import com.travel.aiagent.v3.ITravelGraphAgent;
import com.travel.aiagent.v3.graph.SubAgentGraph;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public abstract class BaseTravelGraphAgent implements ITravelGraphAgent {

    private final CompiledGraph subGraph;

    @Resource
    private ShortTermMemory shortTermMemory;

    public BaseTravelGraphAgent(SubAgentGraph subAgentGraph) {
        this.subGraph = subAgentGraph.buildGraph();
    }

    @Override
    public abstract String name();

    @Override
    public abstract String description();

    @Override
    public String execute(String task, String userId, String chatId, Consumer<String> progress) {

        Map<String, Object> init = new HashMap<>();
        init.put(GraphStateKey.USER_ID.getKey(), userId);
        init.put(GraphStateKey.CHAT_ID.getKey(), chatId);
        init.put(GraphStateKey.LOOP_TIMES.getKey(), 0);
        init.put(GraphStateKey.SUB_AGENT_NAME.getKey(), name());
        init.put(GraphStateKey.ORCHESTRATOR_AGENT_PLAN_DETAIL.getKey(), task);

        log.info("[V3-Sub] {} 开始执行上游task {} ", name(), task);
        Optional<OverAllState> result = subGraph.invoke(init);
        String conclusion = result
                .map(s -> s.value(GraphStateKey.ORCHESTRATOR_AGENT_CONCLUSION.getKey(), ""))
                .orElse("");

        if (conclusion.isBlank()) {
            log.info("出现预料之外的场景，记录一下案发现场");
            log.info(" subAgentName -> {}", name());
            log.info(" all data -> {} ", JSON.toJSONString(result));
            conclusion = "当前agent无法获取到相关信息，请尝试重新调用，或跳过此步骤吧";
        }

        return conclusion;
    }
}
