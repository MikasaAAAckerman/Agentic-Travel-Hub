package com.travel.aiagent.common.core.worker;

import com.alibaba.fastjson2.JSON;
import com.travel.aiagent.common.domain.PlanDetailVO;
import com.travel.aiagent.common.domain.WorkDetailVO;
import com.travel.aiagent.common.domain.prompt.SystemPrompt;
import com.travel.aiagent.common.utils.SpringAIDocumentUtils;
import com.travel.hubtools.tool.common.IAgentTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 共享 Worker 服务 —— 被 v0 / v1 / v2 / v3 共用。
 */
@Service
@Slf4j
public class QwenWorkerService {

    @Resource(name = "parallelToolExecutor") // ⚠️ 必须通过名字指定，别拿错了喵！
    private Executor parallelThreadPool;

    private final ChatClient qwenChatClient;
    private final DocumentRetriever travelDocumentRetriever;

    /**
     * toolName → ToolCallback 映射表
     */
    private final Map<String, ToolCallback> globalToolRegistry = new HashMap<>();

    public QwenWorkerService(
            @Qualifier("qwenChatClient") ChatClient qwenChatClient,
            @Qualifier("travelDocumentRetriever") DocumentRetriever travelDocumentRetriever,
            List<IAgentTool> allAgentTools) {
        log.info("[Worker] 初始化 QwenWorkerService");
        this.qwenChatClient = qwenChatClient;
        this.travelDocumentRetriever = travelDocumentRetriever;
        for (IAgentTool toolBean : allAgentTools) {
            ToolCallback[] callbacks = ToolCallbacks.from(toolBean);
            for (ToolCallback cb : callbacks) {
                globalToolRegistry.put(cb.getToolDefinition().name(), cb);
            }
        }
        log.info("[Worker] 工具注册完成 | totalTools={}", globalToolRegistry.size());
    }

    /**
     * RAG 增强模式：知识库检索 → 注入 Tool → 真实调用
     */
    public WorkDetailVO doWorkWithRag(PlanDetailVO planDetailVO) {
        log.info("[Worker] doWorkWithRag启动 | plan={}", JSON.toJSONString(planDetailVO));
        Query query = new Query(planDetailVO.getPlanDetail());
        List<Document> toolDocumentList = travelDocumentRetriever.retrieve(query);
        List<String> toolBeanList = SpringAIDocumentUtils.getToolBeanList(toolDocumentList, SpringAIDocumentUtils.TOOL_NAME_PATTERN);
        Set<ToolCallback> selectedCallbacks = new HashSet<>();
        for (String toolName : toolBeanList) {
            ToolCallback cb = globalToolRegistry.get(toolName);
            if (cb != null) {
                selectedCallbacks.add(cb);
            } else {
                log.warn("[Worker] 知识库检索到工具但未注册 | toolName={}", toolName);
            }
        }
        log.info("[Worker] RAG检索完成 | matchedTools={}", selectedCallbacks.size());

        String workerPrompt = String.format("""
                        你是负责执行工具调用的 Worker 节点。
                        需要执行的计划是：【%s】，
                        需要调用的工具是：【%s】，
                        请你立刻调用现有工具，执行当前计划并将结果总结为一段简短、包含核心信息的自然语言汇报。
                        你必须通过底层框架真实触发工具调用，绝对不允许捏造工具的执行结果。
                        """,
                planDetailVO.getPlanDetail(),
                JSON.toJSONString(toolBeanList)
        );
        log.info("[Worker] 构建 RAG Worker Prompt -> {} ", workerPrompt);

        ChatResponse chatResponse = qwenChatClient.prompt()
                .system(SystemPrompt.TRAVEL_WORKER_SYSTEM_PROMPT)
                .user(workerPrompt)
                .toolCallbacks(selectedCallbacks.toArray(new ToolCallback[0]))
                .call()
                .chatResponse();
        String finalAnswer = chatResponse.getResult().getOutput().getText();
        WorkDetailVO workDetailVO = new WorkDetailVO(true, finalAnswer);
        log.info("[Worker] RAG任务执行完成 | success={}", JSON.toJSON(workDetailVO));
        return workDetailVO;
    }


    /**
     * 多工具并发执行的doWorkWithRag
     */


    public WorkDetailVO doWorkWithRagParallel(PlanDetailVO planDetailVO) {
        // 通过 RAG 找到 ToolCallBackSet
        log.info("[Worker] doWorkWithRagParallel启动 | plan={}", JSON.toJSONString(planDetailVO));
        if (planDetailVO.getPlanDetail() == null) {
            return new WorkDetailVO(false, "计划为空，无法执行计划，请重新规划当前步骤的计划吧");
        }
        Query query = new Query(planDetailVO.getPlanDetail());

        List<Document> toolDocumentList = travelDocumentRetriever.retrieve(query);
        List<String> toolBeanList = SpringAIDocumentUtils.getToolBeanList(toolDocumentList, SpringAIDocumentUtils.TOOL_NAME_PATTERN);
        Set<ToolCallback> selectedCallbacks = new HashSet<>();
        for (String toolName : toolBeanList) {
            ToolCallback cb = globalToolRegistry.get(toolName);
            if (cb != null) {
                selectedCallbacks.add(cb);
            } else {
                log.warn("[Worker] 知识库检索到工具但未注册 | toolName={}", toolName);
            }
        }

        // 新增chatOption，关闭工具自动执行 https://springdoc.cn/spring-ai/api/tools.html#_%E7%94%A8%E6%88%B7%E6%8E%A7%E5%88%B6%E7%9A%84%E5%B7%A5%E5%85%B7%E6%89%A7%E8%A1%8C
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(new ArrayList<>(selectedCallbacks))
                .internalToolExecutionEnabled(false) // 关闭工具自动执行
                .build();

        // 将配置了工具不许自动执行的option挂载给chatClient
        ChatResponse chatResponse = qwenChatClient.prompt()
                .system(SystemPrompt.TRAVEL_WORKER_SYSTEM_PROMPT) //
                .user("执行计划：" + planDetailVO.getPlanDetail())
                .options(chatOptions)   // 将关闭工具自动执行的Option注入进来
                .call()
                .chatResponse();

        // 拿到消息体，重点关注里面的Tool
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

        log.info(" assistantMessage -> {}", JSON.toJSONString(assistantMessage));

        if (assistantMessage.getToolCalls() != null && !assistantMessage.getToolCalls().isEmpty()) {
            log.info(" 发现 {} 个工具调用，开启并发工具执行 ", assistantMessage.getToolCalls().size());

            List<CompletableFuture<ToolResponseMessage.ToolResponse>> futures = assistantMessage.getToolCalls().stream()
                    .map(toolCall -> CompletableFuture.supplyAsync(() -> {
                        ToolCallback cb = globalToolRegistry.get(toolCall.name());
                        if (cb == null) {
                            log.warn("[Worker-并发] 工具未注册 | name={}", toolCall.name());
                            return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), "工具未注册");
                        }
                        try {
                            String result = cb.call(toolCall.arguments());
                            return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), result);
                        } catch (Exception e) {
                            log.error("[Worker-并发] 工具执行异常 | name={}", toolCall.name(), e);
                            return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), "工具异常：" + e.getMessage());
                        }
                    }, parallelThreadPool))
                    .toList();

            // 4. 全员集合！30s 超时兜底，单个卡死不拖死全图
            List<ToolResponseMessage.ToolResponse> results = futures.stream()
                    .map(f -> {
                        try {
                            return f.get(60, TimeUnit.SECONDS);
                        } catch (java.util.concurrent.TimeoutException e) {
                            log.error("[Worker-并发] 工具执行超时(60s)");
                            return new ToolResponseMessage.ToolResponse("timeout", "unknown", "工具执行超时(60s)");
                        } catch (Exception e) {
                            log.error("[Worker-并发] 工具get异常", e);
                            return new ToolResponseMessage.ToolResponse("error", "unknown", "工具异常：" + e.getMessage());
                        }
                    })
                    .collect(Collectors.toList());

            // 5. 汇总推理（不带 toolCallbacks，告诉 Qwen 只总结别调工具）
            ChatOptions summaryOptions = ToolCallingChatOptions.builder()
                    .internalToolExecutionEnabled(false)
                    .build();
            List<Message> messages = List.of(
                    new UserMessage(planDetailVO.getPlanDetail()),
                    assistantMessage,
                    ToolResponseMessage.builder()
                            .responses(results)
                            .build()
            );
            log.info("[Worker] 开始进行finalResult总结 | message={}", JSON.toJSON(JSON.toJSONString(messages)));
            ChatResponse finalResponse = qwenChatClient.prompt()
                    .system(SystemPrompt.WORKER_SUMMARY_SYSTEM_PROMPT)
                    .messages(messages)
                    .options(summaryOptions)
                    .call()
                    .chatResponse();

            String finalAnswer = finalResponse.getResult().getOutput().getText();
            WorkDetailVO workDetailVO = new WorkDetailVO(true, finalAnswer);
            log.info("[Worker] RAG任务执行完成 | success={}", JSON.toJSON(workDetailVO));
            return workDetailVO;
        }

        return new WorkDetailVO(true, assistantMessage.getText());
    }


}
