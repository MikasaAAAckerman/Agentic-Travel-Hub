package com.travel.aiagent.v3;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.travel.aiagent.common.constant.GraphStateKey;
import com.travel.aiagent.common.constant.PlanActionEnum;
import com.travel.aiagent.common.domain.ChatHistoryDO;
import com.travel.aiagent.common.memory.ChatCompactionService;
import com.travel.aiagent.common.memory.RoundMemory;
import com.travel.aiagent.common.metrics.AgentMetrics;
import com.travel.aiagent.common.repository.ChatHistoryRepository;
import com.travel.aiagent.common.repository.LlmCallLogRepository;
import com.travel.aiagent.common.session.AgentSessionRegistry;
import com.travel.aiagent.common.utils.AgentMDC;
import com.travel.aiagent.v3.graph.OrchestratorGraph;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class OrchestratorGraphAgent {

    private final CompiledGraph compiledGraph;

    @Resource
    private RoundMemory roundMemory;

    @Resource
    private AgentSessionRegistry agentSessionRegistry;

    @Resource
    private AgentMetrics agentMetrics;

    @Resource
    private ChatHistoryRepository chatHistoryRepository;

    @Resource
    private LlmCallLogRepository llmCallLogRepository;

    @Resource
    private ChatCompactionService chatCompactionService;

    public OrchestratorGraphAgent(OrchestratorGraph graph) {
        this.compiledGraph = graph.buildGraph();
    }

    /**
     * 流式模式 —— 利用 Spring AI Alibaba Graph 原生 stream()，
     * 每个 Node 执行完自动推送进度事件。
     */
    public Flux<String> executeStream(String userInput, String userId, String chatId, String traceId) {
        // 第一套对话级记忆：开一轮，记录用户输入
        roundMemory.startRound(userId, chatId, userInput);

        // 聊天记录：用户输入一存在就先落一行（此时 trace_id 已可用于幂等），
        // 拿回的 id 会挂到本轮每一次 llm_call_log 上，供页面重放执行明细
        Long chatHistoryId = chatHistoryRepository.openRound(userId, traceId, userInput);

        Map<String, Object> init = new HashMap<>();
        init.put(GraphStateKey.USER_INPUT.getKey(), userInput);
        init.put(GraphStateKey.USER_ID.getKey(), userId);
        init.put(GraphStateKey.CHAT_ID.getKey(), chatId);
        init.put(GraphStateKey.LOOP_TIMES.getKey(), 0);
        init.put(GraphStateKey.TRACE_ID.getKey(), traceId);

        log.info("OrchestratorGraphAgent 流式开始 | input={}", userInput);

        // 用 AtomicReference 拿最后一帧的结论
        AtomicReference<String> finalConclusion = new AtomicReference<>("抱歉，规划未完成~");
        // clarify（需要用户补充信息）时，规划尚未完成，不展示"最终规划完成"
        AtomicBoolean isClarify = new AtomicBoolean(false);
        AtomicBoolean isOverMax = new AtomicBoolean(false);
        AtomicBoolean isFailed = new AtomicBoolean(false);

        // Flux.defer：在【订阅线程】上设置 MDC，保证后续节点与日志同线程可见
        return Flux.defer(() -> {
                    AgentMDC.setTraceId(traceId);
                    AgentMDC.setSessionId(userId);
                    if (chatHistoryId != null) {
                        AgentMDC.setChatHistoryId(String.valueOf(chatHistoryId));
                    }
                    return compiledGraph.stream(init)
                            .map(output -> formatProgress(output, finalConclusion, isClarify, isOverMax))
                            .filter(s -> !s.isEmpty())
                            .concatWith(Flux.defer(() -> {
                                if (isClarify.get()) {
                                    return Flux.empty();
                                }
                                return Flux.just("\n🎉 最终规划完成：\n" + finalConclusion.get());
                            }));
                })
                .doOnError(e -> {
                    isFailed.set(true);
                    log.error("[V3] 图执行异常 | traceId={}", traceId, e);
                })
                // 收尾：无论 finish / clarify / overMaxLoopTimes / CANCEL（用户停止）哪种结束，都写最终回复
                .doFinally(sig -> {
                    // 用户停止（stop 接口 dispose / 检查点退出）：不写半截结论，标记「已停止」
                    boolean cancelled = sig == SignalType.CANCEL || agentSessionRegistry.isCancelled(traceId);
                    // 兜底清理会话注册（正常/异常/取消都走到，幂等）
                    agentSessionRegistry.remove(traceId);

                    String reason;
                    String reply;
                    if (cancelled) {
                        reason = ChatHistoryDO.FinishReason.CANCELLED;
                        reply = "（用户已停止本次规划）";
                        agentMetrics.recordAgentStop();
                        log.info("[V3] 会话被用户停止，收尾 | traceId={}", traceId);
                    } else if (isFailed.get()) {
                        reason = ChatHistoryDO.FinishReason.FAILED;
                        reply = null;
                    } else if (isClarify.get()) {
                        reason = ChatHistoryDO.FinishReason.CLARIFY;
                        reply = finalConclusion.get();
                    } else if (isOverMax.get()) {
                        reason = ChatHistoryDO.FinishReason.OVER_MAX_LOOP_TIMES;
                        reply = finalConclusion.get();
                    } else {
                        reason = ChatHistoryDO.FinishReason.FINISH;
                        reply = finalConclusion.get();
                    }

                    roundMemory.finishRound(userId, chatId, reply == null ? "（本轮执行失败）" : reply);
                    // 收轮：token 从本轮所有 llm_call_log 汇总；守卫 final_reply IS NULL，重复收尾不会覆盖
                    LlmCallLogRepository.TokenTotals totals = sumTokensSafely(chatHistoryId);
                    chatHistoryRepository.closeRound(chatHistoryId, reply, reason,
                            totals.promptTokens(), totals.completionTokens());

                    // 只有正常完成才压缩：CLARIFY 用户还要补充信息、异常收尾上下文没走完整，
                    // 这两种情况压缩会截断本轮的输入依据
                    if (ChatHistoryDO.FinishReason.FINISH.equals(reason)) {
                        chatCompactionService.compactIfNeeded(userId);
                    }
                    AgentMDC.clearContentContext();
                });
    }

    /** 汇总失败不该拖垮收尾，记日志按 0 处理 */
    private LlmCallLogRepository.TokenTotals sumTokensSafely(Long chatHistoryId) {
        try {
            return llmCallLogRepository.sumTokensByChatHistoryId(chatHistoryId);
        } catch (Exception e) {
            log.warn("[V3] 汇总本轮 token 失败，按 0 处理 | chatHistoryId={}", chatHistoryId, e);
            return new LlmCallLogRepository.TokenTotals(0, 0);
        }
    }

    /** 把 NodeOutput 转成人话进度 */
    private String formatProgress(NodeOutput output, AtomicReference<String> conclusionHolder,
                                  AtomicBoolean clarifyFlag, AtomicBoolean overMaxFlag) {
        String node = output.node();
        OverAllState state = output.state();
        if (state == null) return "";

        // 走走 clarify 节点即说明需要用户补充信息，标记后结尾不再展示"最终规划完成"
        if ("clarify".equals(node)) {
            clarifyFlag.set(true);
        }
        // 轮次耗尽收尾：结束原因要与"正常完成"区分开，否则排查时看不出这条回复是兜底的
        if ("overMaxLoopTimes".equals(node)) {
            overMaxFlag.set(true);
        }

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
            case "finish" -> "🎉 正在为您汇总结果 ...";
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
