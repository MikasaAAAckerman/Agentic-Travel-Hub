package com.travel.aiagent.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天记录摘要（数据库对象）—— 覆盖区间为半开区间 {@code [beginId, endId)}。
 *
 * <p>摘要本身<b>不会再被压缩</b>（v5.2 只做一层），所以 {@code beginId/endId}
 * 永远指向 {@code user_chat_history} 的 id。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryCompactDO {

    private Long id;

    private String sessionId;

    /** 摘要正文 */
    private String content;

    /** 覆盖区间下界（含） */
    private Long beginId;

    /** 覆盖区间上界（不含） */
    private Long endId;

    /** 覆盖了多少轮 */
    private Integer coveredRounds;

    private LocalDateTime createdAt;
}
