package com.travel.starter.controller;

import com.travel.aiagent.common.memory.ShortTermMemory;
import com.travel.aiagent.common.router.IntentRecognitionRouter;
import com.travel.aiagent.v0.DualCoreReactEngine;
import com.travel.aiagent.v1.GraphReactEngine;
import com.travel.aiagent.v2.OrchestratorAgent;
import com.travel.aiagent.v3.OrchestratorGraphAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Travel Agent 接口层 —— v0 / v1 / v2 架构迭代。
 *
 * <ul>
 *   <li>{@code /chat/stream/v0} — while-if-else ReAct（双模型分离）</li>
 *   <li>{@code /chat/stream/v1} — 手写状态机 Graph</li>
 *   <li>{@code /chat/stream/v2} — 多 Agent + 编排者 while 循环</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/aiagent")
public class TravelAgentController {

    // ─── 引擎 ───
    @Resource
    private DualCoreReactEngine v0Engine;

    @Resource
    private GraphReactEngine v1Engine;

    @Resource
    private OrchestratorAgent v2Engine;

    @Resource
    private OrchestratorGraphAgent v3Engine;

    // ─── 共享组件 ───
    @Resource
    private ShortTermMemory shortTermMemory;

    @Resource
    private IntentRecognitionRouter intentRecognitionRouter;

    @Resource
    private ChatClient qwenChatClient;

    // ═══════════════════════════════════════
    // v0：while-if-else ReAct
    // ═══════════════════════════════════════

    @GetMapping(value = "/chat/stream/v0", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamV0(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "sessionId", defaultValue = "default_session_123") String sessionId,
            @RequestParam(value = "chatId", defaultValue = "default_chatId_ryqqubghkjnad") String chatId) {

        return Flux.<String>create(sink -> {
            try {
                String intent = intentRecognitionRouter.doIntentRecognition(prompt);
                if (intent.equals("PLAN")) {
                    log.info("[V0] ReAct 引擎启动");
                    sink.next("✨ [V0 · ReAct] 收到任务，红豆开始规划喵！\n\n");
                    shortTermMemory.addUserTalking(sessionId, chatId, prompt);
                    String history = shortTermMemory.getMemoryByUserIdAndChatId(sessionId, chatId);
                    String result = v0Engine.execute(prompt, history, sink::next);
                    shortTermMemory.addAgentTalking(sessionId, chatId, result);
                    sink.next("\n🎉 最终规划完成：\n" + result);
                } else {
                    log.info("[V0] 闲聊模式");
                    sink.next(qwenChatClient.prompt().user(prompt).call().content());
                }
                sink.complete();
            } catch (Exception e) {
                log.error("[V0] 接口异常", e);
                sink.error(new RuntimeException("系统爆炸了！"));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ═══════════════════════════════════════
    // v1：手写状态机 Graph
    // ═══════════════════════════════════════

    @GetMapping(value = "/chat/stream/v1", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamV1(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "sessionId", defaultValue = "default_session_123") String sessionId,
            @RequestParam(value = "chatId", defaultValue = "default_chatId_ryqqubghkjnad") String chatId) {

        return Flux.<String>create(sink -> {
            try {
                String intent = intentRecognitionRouter.doIntentRecognition(prompt);
                if (intent.equals("PLAN")) {
                    log.info("[V1] Graph 引擎启动");
                    sink.next("✨ [V1 · Graph] 收到任务，红豆开始规划喵！\n\n");
                    shortTermMemory.addUserTalking(sessionId, chatId, prompt);
                    String history = shortTermMemory.getMemoryByUserIdAndChatId(sessionId, chatId);
                    String result = v1Engine.execute(prompt, history, sink::next);
                    shortTermMemory.addAgentTalking(sessionId, chatId, result);
                    sink.next("\n🎉 最终规划完成：\n" + result);
                } else {
                    log.info("[V1] 闲聊模式");
                    sink.next(qwenChatClient.prompt().user(prompt).call().content());
                }
                sink.complete();
            } catch (Exception e) {
                log.error("[V1] 接口异常", e);
                sink.error(new RuntimeException("系统爆炸了！"));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ═══════════════════════════════════════
    // v2：多 Agent + 编排者 while 循环
    // ═══════════════════════════════════════

    @GetMapping(value = "/chat/stream/v2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamV2(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "sessionId", defaultValue = "default_session_123") String sessionId,
            @RequestParam(value = "chatId", defaultValue = "default_chatId_ryqqubghkjnad") String chatId) {

        return Flux.<String>create(sink -> {
            try {
                String intent = intentRecognitionRouter.doIntentRecognition(prompt);
                if (intent.equals("PLAN")) {
                    log.info("[V2] 多Agent 引擎启动");
                    sink.next("✨ [V2 · Multi-Agent] 收到任务，红豆调度专家团队中喵！\n\n");
                    shortTermMemory.addUserTalking(sessionId, chatId, prompt);
                    String history = shortTermMemory.getMemoryByUserIdAndChatId(sessionId, chatId);
                    String result = v2Engine.execute(prompt, history, sink::next);
                    shortTermMemory.addAgentTalking(sessionId, chatId, result);
                    sink.next("\n🎉 专家团队规划完成：\n" + result);
                } else {
                    log.info("[V2] 闲聊模式");
                    sink.next(qwenChatClient.prompt().user(prompt).call().content());
                }
                sink.complete();
            } catch (Exception e) {
                log.error("[V2] 接口异常", e);
                sink.error(new RuntimeException("系统爆炸了！"));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ═══════════════════════════════════════
    // v3：多 Agent + Spring AI Alibaba Graph（原生流式）
    // ═══════════════════════════════════════

    @GetMapping(value = "/chat/stream/v3", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamV3(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "sessionId", defaultValue = "default_session_123") String sessionId,
            @RequestParam(value = "chatId", defaultValue = "default_chatId_ryqqubghkjnad") String chatId) {

        String intent = intentRecognitionRouter.doIntentRecognition(prompt);
        if (!"PLAN".equals(intent)) {
            log.info("[V3] 闲聊模式");
            return Flux.just(qwenChatClient.prompt().user(prompt).call().content());
        }

        log.info("[V3] Graph多Agent 流式引擎启动");
        shortTermMemory.addUserTalking(sessionId, chatId, prompt);

        return Flux.just("✨ [V3 · Graph Multi-Agent] 两层Graph嵌套，红豆调度专家团队中喵！\n\n")
                .concatWith(v3Engine.executeStream(prompt, sessionId, chatId))
                .onErrorResume(e -> {
                    log.error("[V3] 接口异常", e);
                    return Flux.just("系统爆炸了！" + e.getMessage());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    // ═══════════════════════════════════════
    // 记忆查看器：实时查看 ShortTermMemory 内容
    // ═══════════════════════════════════════

    @GetMapping(value = "/memory/view", produces = MediaType.TEXT_PLAIN_VALUE)
    public String viewMemory(
            @RequestParam(value = "sessionId", defaultValue = "hongdou_master") String sessionId,
            @RequestParam(value = "chatId", defaultValue = "session_001") String chatId) {
        return shortTermMemory.getMemoryByUserIdAndChatId(sessionId, chatId);
    }
}
