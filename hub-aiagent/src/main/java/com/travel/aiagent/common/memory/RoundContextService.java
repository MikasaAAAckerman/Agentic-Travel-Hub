package com.travel.aiagent.common.memory;

import com.travel.aiagent.common.domain.ChatHistoryCompactDO;
import com.travel.aiagent.common.domain.ChatHistoryDO;
import com.travel.aiagent.common.repository.ChatHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 组装给 planner 的对话轮次 —— <b>唯一同时认识 {@link RoundVO} 和 {@link ChatHistoryDO} 的地方</b>。
 *
 * <p>为什么要单独一个类，而不是塞进 {@link RoundMemory}：
 * <ul>
 *   <li>{@code RoundMemory} 是本轮缓冲（纯内存，活一个请求）</li>
 *   <li>{@code ChatHistoryRepository} 是持久化（DB，跨请求，会超时会挂）</li>
 *   <li>两者生命周期与失败模式都不同，捆在一起会让"DB 抖一下"连累内存缓冲，
 *       也会让 RoundVO 与 ChatHistoryDO 互相串味</li>
 * </ul>
 *
 * <p>所以这里只做一件事：<b>翻译 + 拼接</b>。不写库、不改内存。
 *
 * <p>拼接规则：
 * <ol>
 *   <li>DB 里最近 N 轮（已完成、未压缩）→ 翻译成 RoundVO，subAgentResults 为空</li>
 *   <li>内存里当前这一轮（finalReply 仍未写）→ 保留 subAgentResults</li>
 *   <li>轮次号统一重排：历史 1..N，当前轮 = N+1</li>
 * </ol>
 *
 * <p>DB 读失败时 fail-open：历史丢掉，但本轮照常规划 —— 记忆丢了比请求失败好。
 */
@Slf4j
@Service
public class RoundContextService {

    /** 最多带多少轮历史给 planner，防止上下文无限膨胀 */
    private static final int MAX_HISTORY_ROUNDS = 20;

    private final ChatHistoryRepository chatHistoryRepository;
    private final RoundMemory roundMemory;

    public RoundContextService(ChatHistoryRepository chatHistoryRepository, RoundMemory roundMemory) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.roundMemory = roundMemory;
    }

    /**
     * 组装 planner 可见的记忆（摘要 + 历史 + 本轮执行结果）。
     *
     * <p>顺序刻意与 {@code PlannerContextVO} 的字段声明保持一致：
     * 越早的历史越靠前，本轮内容最后 —— 这就是模型的时间轴。
     */
    public PlannerMemory loadPlannerMemory(String userId, String chatId) {
        String summary = loadSummary(userId);

        List<RoundVO> history = new ArrayList<>(MAX_HISTORY_ROUNDS);
        loadHistory(userId, history);

        List<SubAgentResultVO> currentResults = currentSubAgentResults(userId, chatId);

        return new PlannerMemory(summary, history, currentResults);
    }

    /**
     * 多段摘要按时间顺序拼接 —— 因为只压最老的前缀，所以顺序天然正确。
     */
    private String loadSummary(String userId) {
        List<ChatHistoryCompactDO> compacts;
        try {
            compacts = chatHistoryRepository.findCompacts(userId);
        } catch (Exception e) {
            log.error("[RoundContext] 读取摘要失败，按无摘要继续 | sessionId={}", userId, e);
            return "";
        }
        if (compacts == null || compacts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatHistoryCompactDO c : compacts) {
            if (c.getContent() == null || c.getContent().isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(c.getContent().trim());
        }
        return sb.toString();
    }

    /**
     * 当前轮：只有"还没写最终回复"时才取它的执行结果。
     */
    private List<SubAgentResultVO> currentSubAgentResults(String userId, String chatId) {
        List<RoundVO> inMemory = roundMemory.getRounds(userId, chatId);
        if (inMemory == null || inMemory.isEmpty()) {
            return List.of();
        }
        RoundVO last = inMemory.get(inMemory.size() - 1);
        if (last.getFinalReply() != null && !last.getFinalReply().isBlank()) {
            return List.of();   // 已完成，DB 里已有这一轮
        }
        return last.getSubAgentResults() == null ? List.of() : last.getSubAgentResults();
    }

    /** planner 可见的记忆快照 */
    public record PlannerMemory(
            String historySummary,
            List<RoundVO> rounds,
            List<SubAgentResultVO> currentSubAgentResults
    ) {
    }

    /** DB 历史：倒序取出后反转成正序；失败只记日志，不影响本轮 */
    private void loadHistory(String userId, List<RoundVO> result) {
        List<ChatHistoryDO> history;
        try {
            history = chatHistoryRepository.findRecentVisible(userId, MAX_HISTORY_ROUNDS);
        } catch (Exception e) {
            log.error("[RoundContext] 读取历史失败，本轮按无历史继续 | sessionId={}", userId, e);
            return;
        }
        if (history == null || history.isEmpty()) {
            return;
        }
        // findRecentVisible 是倒序（最近的在前），反转成正序
        List<ChatHistoryDO> asc = new ArrayList<>(history);
        Collections.reverse(asc);
        for (ChatHistoryDO d : asc) {
            result.add(RoundVO.builder()
                    .round(result.size() + 1)
                    .userInput(d.getUserInput())
                    .finalReply(d.getFinalReply())
                    .subAgentResults(List.of())   // 执行明细在 llm_call_log，不在记忆里
                    .build());
        }
    }

    /**
     * 历史轮不再塞 subAgentResults —— 那是执行明细，在 llm_call_log 里按 chat_history_id 查。
     */
}
