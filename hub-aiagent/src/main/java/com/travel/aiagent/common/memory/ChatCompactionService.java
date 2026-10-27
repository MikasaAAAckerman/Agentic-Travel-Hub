package com.travel.aiagent.common.memory;

import com.travel.aiagent.common.domain.ChatHistoryDO;
import com.travel.aiagent.common.repository.ChatHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天记录压缩 —— 把最早的一段原文压成一条摘要。
 *
 * <p><b>触发时机：只在本轮 {@code FINISH} 之后。</b>其他收尾一律不压：
 * <ul>
 *   <li>{@code CLARIFY} —— 用户还要补充信息，这轮没完，压了就把上下文截断了</li>
 *   <li>{@code OVER_MAX_LOOP_TIMES} / {@code CANCELLED} / {@code FAILED} —— 异常收尾，上下文没走完整</li>
 * </ul>
 *
 * <p>核心原则：<b>不能让压缩发生在轮内</b>。一旦本轮执行到一半历史就变了，
 * 模型前一步和后一步看到的上下文不一致 —— 那不是省 token，是自造幻觉。
 *
 * <p>四条规则：
 * <ol>
 *   <li>只压<b>最老的前缀</b>：摘要永远排在最前，顺序天然正确</li>
 *   <li>最近 {@value #KEEP_RECENT_ROUNDS} 轮原文永不压缩：模型对"刚发生什么"最敏感</li>
 *   <li>一次压"可压部分的一半"，至少 {@value #MIN_COMPACT_ROUNDS} 轮</li>
 *   <li>压缩失败一律吞掉：记忆压不了，比对话失败轻得多</li>
 * </ol>
 */
@Slf4j
@Service
public class ChatCompactionService {

    /** 可见历史超过这个 token 才考虑压缩（留足余量，压一次顶好几轮） */
    static final int THRESHOLD_TOKENS = 6000;

    /** 最近这么多轮永远保留原文 */
    static final int KEEP_RECENT_ROUNDS = 4;

    /** 一次至少压这么多轮，否则不划算 */
    static final int MIN_COMPACT_ROUNDS = 2;

    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatClient assistantClient;

    public ChatCompactionService(ChatHistoryRepository chatHistoryRepository,
                                 @Qualifier("assistantClient") ChatClient assistantClient) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.assistantClient = assistantClient;
    }

    /**
     * 按需压缩。任何异常都只记日志，不向调用方抛。
     *
     * @param sessionId 会话ID（现阶段等同用户ID）
     */
    public void compactIfNeeded(String sessionId) {
        try {
            doCompact(sessionId);
        } catch (Exception e) {
            log.warn("[Compaction] 压缩失败，保留原文 | sessionId={}", sessionId, e);
        }
    }

    private void doCompact(String sessionId) {
        int tokens = chatHistoryRepository.sumVisibleTokens(sessionId);
        if (tokens <= THRESHOLD_TOKENS) {
            return;
        }

        int visible = chatHistoryRepository.countVisible(sessionId);
        int compactable = visible - KEEP_RECENT_ROUNDS;
        if (compactable < MIN_COMPACT_ROUNDS) {
            log.info("[Compaction] 可压轮次不足，跳过 | sessionId={} | visible={} | keep={}",
                    sessionId, visible, KEEP_RECENT_ROUNDS);
            return;
        }

        int batch = Math.max(MIN_COMPACT_ROUNDS, compactable / 2);
        List<ChatHistoryDO> covered = chatHistoryRepository.findVisibleOldest(sessionId, batch);
        if (covered.size() < MIN_COMPACT_ROUNDS) {
            return;
        }

        String summary = callModel(buildPrompt(covered));
        if (summary == null || summary.isBlank()) {
            log.warn("[Compaction] 模型返回空摘要，放弃本次压缩 | sessionId={}", sessionId);
            return;
        }

        long compactId = chatHistoryRepository.saveCompaction(sessionId, covered, summary.trim());
        log.info("[Compaction] 压缩完成 | sessionId={} | 覆盖轮数={} | 压缩前可见token={} | compactId={} | 摘要长度={}",
                sessionId, covered.size(), tokens, compactId, summary.trim().length());
    }

    /** 单独抽出便于测试覆盖（不 mock ChatClient 的流式 API） */
    protected String callModel(String prompt) {
        return assistantClient.prompt().user(prompt).call().content();
    }

    private String buildPrompt(List<ChatHistoryDO> rounds) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("请把下面几轮对话压缩成一段摘要，供后续对话继续使用。\n\n")
                .append("必须保留：\n")
                .append("1. 用户明确提出的要求（尽量保留原话）\n")
                .append("2. 用户否决过、纠正过的方案（避免重复犯错）\n")
                .append("3. 已经确认的结论、数据、名单\n\n")
                .append("不要写「用户询问了…」这类空话；不要提及「这是压缩」；不要编造未出现的信息。\n")
                .append("控制在 200 字以内。\n\n")
                .append("轮次：\n");

        for (int i = 0; i < rounds.size(); i++) {
            ChatHistoryDO r = rounds.get(i);
            sb.append("第 ").append(i + 1).append(" 轮\n")
                    .append("用户：").append(nullToEmpty(r.getUserInput())).append('\n')
                    .append("助手：").append(nullToEmpty(r.getFinalReply())).append('\n');
        }
        sb.append("\n摘要：");
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
