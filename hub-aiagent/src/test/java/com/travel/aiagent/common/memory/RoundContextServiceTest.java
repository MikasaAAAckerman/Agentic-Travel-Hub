package com.travel.aiagent.common.memory;

import com.travel.aiagent.common.domain.ChatHistoryCompactDO;
import com.travel.aiagent.common.domain.ChatHistoryDO;
import com.travel.aiagent.common.repository.ChatHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RoundContextService：摘要 + 历史 + 本轮结果的组装")
class RoundContextServiceTest {

    private ChatHistoryRepository repository;
    private RoundMemory roundMemory;
    private RoundContextService service;

    @BeforeEach
    void setUp() {
        repository = mock(ChatHistoryRepository.class);
        roundMemory = mock(RoundMemory.class);
        service = new RoundContextService(repository, roundMemory);
    }

    private static ChatHistoryDO history(String input, String reply) {
        return ChatHistoryDO.builder().userInput(input).finalReply(reply).build();
    }

    private static ChatHistoryCompactDO compact(String content) {
        return ChatHistoryCompactDO.builder().content(content).build();
    }

    private static RoundVO currentRound(String input) {
        return RoundVO.builder().round(99).userInput(input).finalReply("").build();
    }

    @Test
    @DisplayName("历史反转成正序、轮次重排，本轮结果单独取出")
    void assemblesSummaryHistoryAndCurrentResults() {
        when(repository.findCompacts(anyString())).thenReturn(List.of(compact("早期摘要")));
        // findRecentVisible 返回倒序
        when(repository.findRecentVisible(anyString(), anyInt()))
                .thenReturn(List.of(history("u2", "a2"), history("u1", "a1")));

        SubAgentResultVO result = SubAgentResultVO.builder().agent("FoodAgent").conclusion("三家餐厅").build();
        RoundVO inFlight = currentRound("u3");
        inFlight.setSubAgentResults(List.of(result));
        when(roundMemory.getRounds(anyString(), anyString())).thenReturn(List.of(inFlight));

        RoundContextService.PlannerMemory memory = service.loadPlannerMemory("s1", "c1");

        assertThat(memory.historySummary()).isEqualTo("早期摘要");
        assertThat(memory.rounds()).hasSize(2);
        assertThat(memory.rounds()).extracting(RoundVO::getRound).containsExactly(1, 2);
        assertThat(memory.rounds()).extracting(RoundVO::getUserInput).containsExactly("u1", "u2");
        // 历史轮没有执行明细（明细在 llm_call_log）
        assertThat(memory.rounds().get(0).getSubAgentResults()).isEmpty();
        // 本轮结果单独承载
        assertThat(memory.currentSubAgentResults()).hasSize(1);
        assertThat(memory.currentSubAgentResults().get(0).getAgent()).isEqualTo("FoodAgent");
    }

    @Test
    @DisplayName("多段摘要按时间顺序拼接")
    void multipleSummariesAreJoined() {
        when(repository.findCompacts(anyString())).thenReturn(List.of(compact("第一段"), compact("第二段")));
        when(repository.findRecentVisible(anyString(), anyInt())).thenReturn(List.of());
        when(roundMemory.getRounds(anyString(), anyString())).thenReturn(List.of());

        assertThat(service.loadPlannerMemory("s1", "c1").historySummary())
                .isEqualTo("第一段\n\n第二段");
    }

    @Test
    @DisplayName("当前轮已完成 -> 不重复带出执行结果")
    void finishedRoundHasNoCurrentResults() {
        when(repository.findCompacts(anyString())).thenReturn(List.of());
        when(repository.findRecentVisible(anyString(), anyInt())).thenReturn(List.of(history("u1", "a1")));
        when(roundMemory.getRounds(anyString(), anyString()))
                .thenReturn(List.of(RoundVO.builder().round(1).userInput("u1").finalReply("a1").build()));

        RoundContextService.PlannerMemory memory = service.loadPlannerMemory("s1", "c1");

        assertThat(memory.rounds()).hasSize(1);
        assertThat(memory.currentSubAgentResults()).isEmpty();
    }

    @Test
    @DisplayName("DB 读失败时 fail-open：历史与摘要都空，本轮结果照常返回")
    void databaseFailureFailsOpen() {
        when(repository.findCompacts(anyString())).thenThrow(new RuntimeException("connection refused"));
        when(repository.findRecentVisible(anyString(), anyInt())).thenThrow(new RuntimeException("connection refused"));
        when(roundMemory.getRounds(anyString(), anyString())).thenReturn(List.of(currentRound("u1")));

        RoundContextService.PlannerMemory memory = service.loadPlannerMemory("s1", "c1");

        assertThat(memory.historySummary()).isEmpty();
        assertThat(memory.rounds()).isEmpty();
        assertThat(memory.currentSubAgentResults()).isEmpty();
    }

    @Test
    @DisplayName("什么都没有 -> 全空（初次对话）")
    void emptyWhenNothingExists() {
        when(repository.findCompacts(anyString())).thenReturn(List.of());
        when(repository.findRecentVisible(anyString(), anyInt())).thenReturn(List.of());
        when(roundMemory.getRounds(anyString(), anyString())).thenReturn(List.of());

        RoundContextService.PlannerMemory memory = service.loadPlannerMemory("s1", "c1");

        assertThat(memory.historySummary()).isEmpty();
        assertThat(memory.rounds()).isEmpty();
        assertThat(memory.currentSubAgentResults()).isEmpty();
    }
}
