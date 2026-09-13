package com.travel.aiagent.common.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第一套：多轮对话级短期记忆（v3 专用）。
 *
 * <p>记录「每一轮用户输入 + orchestrator 最终回复」，供多轮对话使用：
 * <pre>
 *   startRound(userId, chatId, userInput)   ← 请求开始（记用户说了什么）
 *   appendTurn(...)                          ← （第二套执行轨迹，本类不涉及）
 *   finishRound(userId, chatId, finalReply)  ← 请求收尾（记最终回了什么）
 * </pre>
 *
 * <p>finalReply 覆盖三种结束方式：finish（正常总结）/ clarify（补充信息）/ overMaxLoopTimes（强制总结）。
 *
 * <p>⚠️ 假定同一 chatId 的请求不并发——并发会让「当前轮」产生歧义（单用户场景成立）。
 *
 * <p>与 {@link ShortTermMemory}（v0/v1/v2 平铺 String）对比：本类是 v3 的结构化升级，
 * 用「一次用户请求 = 一轮」的方式组织，debug 时一眼看清每轮对话。
 */
@Slf4j
@Service
public class RoundMemory {

    /** conversationId -> 按序排列的对话轮次 */
    private final Map<String, List<RoundVO>> memory = new ConcurrentHashMap<>();

    /**
     * 开一轮：记录用户这一轮说了什么。
     *
     * @param userInput 用户输入
     */
    public synchronized void startRound(String userId, String chatId, String userInput) {
        String conversationId = getConversationId(userId, chatId);
        List<RoundVO> rounds = memory.computeIfAbsent(conversationId, k -> new ArrayList<>());
        RoundVO round = RoundVO.builder()
                .round(rounds.size() + 1)
                .userInput(userInput)
                .finalReply("")
                .build();
        rounds.add(round);
        log.info("[RoundMemory] 开轮 {} | conversationId={} | userInput={}", round.getRound(), conversationId, userInput);
    }

    /**
     * 收轮：给当前这一轮写最终回复（覆盖 finish / clarify / overMaxLoopTimes 三种结束）。
     *
     * @param finalReply 最终回复给用户的话
     */
    public synchronized void finishRound(String userId, String chatId, String finalReply) {
        String conversationId = getConversationId(userId, chatId);
        List<RoundVO> rounds = memory.get(conversationId);
        if (rounds == null || rounds.isEmpty()) {
            log.warn("[RoundMemory] 收轮失败：conversationId={} 无进行中的轮次", conversationId);
            return;
        }
        RoundVO last = rounds.get(rounds.size() - 1);
        last.setFinalReply(finalReply);
        log.info("[RoundMemory] 收轮 {} | conversationId={} | finalReplyLen={}", last.getRound(), conversationId, finalReply.length());
    }

    /**
     * 获取该会话全部对话轮次（按 round 升序，返回不可变副本）。
     */
    public synchronized List<RoundVO> getRounds(String userId, String chatId) {
        List<RoundVO> rounds = memory.get(getConversationId(userId, chatId));
        if (rounds == null || rounds.isEmpty()) {
            return List.of();
        }
        return List.copyOf(rounds);
    }

    /**
     * 调试视图：一轮一块，像 SillyTavern 那样逐轮排开。
     */
    public synchronized String viewRounds(String userId, String chatId) {
        List<RoundVO> rounds = getRounds(userId, chatId);
        if (rounds.isEmpty()) {
            return "暂无对话记忆，这是你们的初次对话";
        }
        StringBuilder sb = new StringBuilder();
        for (RoundVO r : rounds) {
            sb.append("════ 第 ").append(r.getRound()).append(" 轮 ════\n")
              .append("👤 用户：").append(r.getUserInput()).append("\n")
              .append("🤖 Agent：").append(r.getFinalReply()).append("\n\n");
        }
        return sb.toString();
    }

    private String getConversationId(String userId, String chatId) {
        return userId + "-" + chatId;
    }
}
