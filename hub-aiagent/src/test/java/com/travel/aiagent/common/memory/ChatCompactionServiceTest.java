package com.travel.aiagent.common.memory;

import com.travel.aiagent.common.domain.ChatHistoryDO;
import com.travel.aiagent.common.repository.ChatHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ChatCompactionService：只压最老前缀、保留最近几轮")
class ChatCompactionServiceTest {

    private ChatHistoryRepository repository;
    private StubService service;

    /** 覆写 callModel，避免 mock ChatClient 的流式 API */
    static class StubService extends ChatCompactionService {
        String reply = "用户要避开海鲜；已确认 3 家餐厅。";

        StubService(ChatHistoryRepository repository) {
            super(repository, mock(ChatClient.class));
        }

        @Override
        protected String callModel(String prompt) {
            return reply;
        }
    }

    @BeforeEach
    void setUp() {
        repository = mock(ChatHistoryRepository.class);
        service = new StubService(repository);
    }

    private static List<ChatHistoryDO> rounds(int n) {
        List<ChatHistoryDO> list = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            list.add(ChatHistoryDO.builder()
                    .id((long) i)
                    .userInput("u" + i)
                    .finalReply("a" + i)
                    .build());
        }
        return list;
    }

    @Test
    @DisplayName("低于阈值 -> 什么都不做")
    void belowThresholdDoesNothing() {
        when(repository.sumVisibleTokens(anyString())).thenReturn(ChatCompactionService.THRESHOLD_TOKENS);

        service.compactIfNeeded("s1");

        verify(repository, never()).saveCompaction(anyString(), anyList(), anyString());
        verify(repository, never()).countVisible(anyString());
    }

    @Test
    @DisplayName("可压轮次不足（要保留最近 4 轮）-> 跳过，不压")
    void keepsRecentRounds() {
        when(repository.sumVisibleTokens(anyString())).thenReturn(9000);
        // 可见 5 轮，保留 4 轮 -> 只可压 1 轮 < 最少 2 轮
        when(repository.countVisible(anyString())).thenReturn(5);

        service.compactIfNeeded("s1");

        verify(repository, never()).saveCompaction(anyString(), anyList(), anyString());
    }

    @Test
    @DisplayName("超过阈值 -> 压最早的一半（10 轮留 4，可压 6，压 3）")
    void compactsOldestHalf() {
        when(repository.sumVisibleTokens(anyString())).thenReturn(9000);
        when(repository.countVisible(anyString())).thenReturn(10);
        when(repository.findVisibleOldest(eq("s1"), eq(3))).thenReturn(rounds(3));
        when(repository.saveCompaction(anyString(), anyList(), anyString())).thenReturn(42L);

        service.compactIfNeeded("s1");

        verify(repository).findVisibleOldest("s1", 3);
        verify(repository).saveCompaction(eq("s1"), anyList(), eq("用户要避开海鲜；已确认 3 家餐厅。"));
    }

    @Test
    @DisplayName("模型返回空摘要 -> 不写库，保留原文")
    void blankSummaryIsDiscarded() {
        service.reply = "   ";
        when(repository.sumVisibleTokens(anyString())).thenReturn(9000);
        when(repository.countVisible(anyString())).thenReturn(10);
        when(repository.findVisibleOldest(anyString(), anyInt())).thenReturn(rounds(3));

        service.compactIfNeeded("s1");

        verify(repository, never()).saveCompaction(anyString(), anyList(), anyString());
    }

    @Test
    @DisplayName("写库异常 -> 吞掉，不向调用方抛")
    void writeFailureIsSwallowed() {
        when(repository.sumVisibleTokens(anyString())).thenReturn(9000);
        when(repository.countVisible(anyString())).thenReturn(10);
        when(repository.findVisibleOldest(anyString(), anyInt())).thenReturn(rounds(3));
        when(repository.saveCompaction(anyString(), anyList(), anyString()))
                .thenThrow(new IllegalStateException("并发压缩"));

        assertThatCode(() -> service.compactIfNeeded("s1")).doesNotThrowAnyException();
    }
}
