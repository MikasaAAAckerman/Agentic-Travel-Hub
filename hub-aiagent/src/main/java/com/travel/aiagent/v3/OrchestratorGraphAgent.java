package com.travel.aiagent.v3;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.travel.aiagent.common.constant.GraphStateKey;
import com.travel.aiagent.common.constant.PlanActionEnum;
import com.travel.aiagent.v3.graph.OrchestratorGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class OrchestratorGraphAgent {

    private final CompiledGraph compiledGraph;

    public OrchestratorGraphAgent(OrchestratorGraph graph) {
        this.compiledGraph = graph.buildGraph();
    }

    /**
     * 流式模式 —— 利用 Spring AI Alibaba Graph 原生 stream()，
     * 每个 Node 执行完自动推送进度事件。
     */
    public Flux<String> executeStream(String userInput, String userId, String chatId) {
        Map<String, Object> init = new HashMap<>();
        init.put(GraphStateKey.USER_INPUT.getKey(), userInput);
        init.put(GraphStateKey.USER_ID.getKey(), userId);
        init.put(GraphStateKey.CHAT_ID.getKey(), chatId);
        init.put(GraphStateKey.LOOP_TIMES.getKey(), 0);

        log.info("OrchestratorGraphAgent 流式开始 | input={}", userInput);

        // 用 AtomicReference 拿最后一帧的结论
        AtomicReference<String> finalConclusion = new AtomicReference<>("抱歉，规划未完成~");

        return compiledGraph.stream(init)
                .map(output -> formatProgress(output, finalConclusion))
                .filter(s -> !s.isEmpty())
                .concatWith(Flux.defer(() ->
                        Flux.just("\n🎉 最终规划完成：\n" + finalConclusion.get())));
    }

    /** 把 NodeOutput 转成人话进度 */
    private String formatProgress(NodeOutput output, AtomicReference<String> conclusionHolder) {
        String node = output.node();
        OverAllState state = output.state();
        if (state == null) return "";

        String action = state.value(GraphStateKey.ACTION.getKey(), "");
        String subAgentName = state.value(GraphStateKey.SUB_AGENT_NAME.getKey(), "");
        String planDetail = state.value(GraphStateKey.ORCHESTRATOR_AGENT_PLAN_DETAIL.getKey(), "");
        String conclusion = state.value(GraphStateKey.ORCHESTRATOR_AGENT_CONCLUSION.getKey(), "");
        Integer loopTimes = state.value(GraphStateKey.LOOP_TIMES.getKey(), 0);

        // 缓存 conclusion 给最后拼结果用
        if (!conclusion.isEmpty()) {
            conclusionHolder.set(conclusion);
        }

        return switch (node) {
            case "planner" -> {
                if (PlanActionEnum.FINISH.getType().equals(action)) {
                    yield "🧠 调度者规划完成！\n";
                }
                if (PlanActionEnum.CLARIFY.getType().equals(action)) {
                    yield "";  // CLARIFY 时 conclusion 已存，最后 concatWith 会展示
                }
                if (PlanActionEnum.SUB_AGENT_CALL.getType().equals(action) && !subAgentName.isEmpty()) {
                    yield String.format("🧠 [第%d步] 红豆正在调度 %s 处理：%s\n", loopTimes, subAgentName, planDetail);
                }
                yield String.format("🧠 [第%d步] 调度者正在分析需求...\n", loopTimes);
            }
            case "finish" -> String.format("🎉 已帮您完成所有的任务规划，结论如下：\n%s\n", conclusion);
            case "clarify" -> String.format("🤔 红豆还需要主人确认一下信息喵～\n%s\n", conclusion);
            case "overMaxLoopTimes" -> "⏰ 规划轮次已达上限，正在汇总已有结果...\n";
            default -> {
                if (!subAgentName.isEmpty()) {
                    yield String.format("✅ %s 处理完毕，正在汇总结果...\n", node);
                }
                yield "";
            }
        };
    }
}
