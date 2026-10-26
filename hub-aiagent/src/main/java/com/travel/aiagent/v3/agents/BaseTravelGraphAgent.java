package com.travel.aiagent.v3.agents;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.travel.aiagent.common.constant.GraphStateKey;
import com.travel.aiagent.common.domain.SubAgentOutput;
import com.travel.aiagent.common.domain.constraint.ConstraintSet;
import com.travel.aiagent.common.domain.constraint.ConstraintVerdictBuffer;
import com.travel.aiagent.common.domain.constraint.Receipt;
import com.travel.aiagent.common.memory.ShortTermMemory;
import com.travel.aiagent.common.memory.SubAgentReActContextVO;
import com.travel.aiagent.v3.ITravelGraphAgent;
import com.travel.aiagent.v3.graph.SubAgentGraph;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 子 Agent 基类。
 *
 * <p>约束治理在这里只做两件事：把<b>只读快照</b>塞进 GraphState（全量，不筛选），
 * 以及把上一轮的回执捞出来交给 Orchestrator。
 * 它<b>不解释约束内容</b>，也不知道约束是什么领域。
 */
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

    /**
     * 本领域的静态执行规则 —— 纯 Prompt 文本，<b>不是约束数据</b>。
     *
     * <p>它由代码声明，不参与对账，也不进真源：它说的是"我自己该怎么干活"，
     * 而不是"用户要求了什么"。各垂直 Agent 按需 override。
     */
    protected String domainInstruction() {
        return "";
    }

    @Override
    public SubAgentOutput execute(String task, ConstraintSet constraints,
                                  String userId, String chatId, String traceId, Consumer<String> progress) {

        ConstraintSet snapshot = constraints == null ? ConstraintSet.empty() : constraints;
        String instruction = domainInstruction() == null ? "" : domainInstruction().trim();

        Map<String, Object> init = new HashMap<>();
        init.put(GraphStateKey.USER_ID.getKey(), userId);
        init.put(GraphStateKey.CHAT_ID.getKey(), chatId);
        init.put(GraphStateKey.LOOP_TIMES.getKey(), 0);
        init.put(GraphStateKey.SUB_AGENT_NAME.getKey(), name());
        init.put(GraphStateKey.ORCHESTRATOR_AGENT_PLAN_DETAIL.getKey(), task);
        init.put(GraphStateKey.TRACE_ID.getKey(), traceId);
        // 初始化子 Agent 的 ReAct 记忆：orchestratorPlan 固定 + turns 空列表
        init.put(GraphStateKey.SUB_AGENT_REACT_MEMORY.getKey(), SubAgentReActContextVO.builder()
                .orchestratorPlan(task)
                .turns(new ArrayList<>())
                .build());
        // 下行：全量只读约束快照 + 本领域静态规则
        init.put(GraphStateKey.GLOBAL_CONSTRAINTS.getKey(), snapshot);
        init.put(GraphStateKey.DOMAIN_INSTRUCTION.getKey(), instruction);
        init.put(GraphStateKey.AUDIT_ERR.getKey(), "");
        init.put(GraphStateKey.CONSTRAINT_VERDICTS.getKey(), new ConstraintVerdictBuffer());

        log.info("[V3-Sub] {} 开始执行上游task {} | 约束 revision={} digest={} 条数={}",
                name(), task, snapshot.revision(), snapshot.digest(), snapshot.items().size());

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

        // 回执跨子图边界以 JSON 字符串传递（框架会隐式序列化对象，嵌套泛型会丢类型）
        String receiptJson = result
                .map(s -> s.value(GraphStateKey.CONSTRAINT_RECEIPT.getKey(), ""))
                .orElse("");
        Receipt receipt = Receipt.parse(receiptJson);

        return new SubAgentOutput(conclusion, receipt);
    }
}
