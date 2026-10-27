package com.travel.aiagent.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天记录（数据库对象）—— 一行 = 一轮 = 我说了什么 + 最终回了什么。
 *
 * <p>它<b>不是</b>执行明细：subAgent 的中间输出不在这里，去 {@code llm_call_log}
 * 按 {@code chat_history_id} 查。
 *
 * <p>字段写入时机（每个字段只写一次）：
 * <ul>
 *   <li>{@code sessionId / traceId / userInput / createdAt} —— 请求进入时 INSERT</li>
 *   <li>{@code finalReply / finishReason / inputToken / outputToken / finishedAt} —— 收尾时 UPDATE</li>
 *   <li>{@code compactedBy / compactedAt} —— 压缩时 UPDATE</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryDO {

    /** 主键 */
    private Long id;

    /** 会话ID（现阶段等同用户ID） */
    private String sessionId;

    /** 本轮请求的链路ID，同时作为幂等键 */
    private String traceId;

    /** 用户输入 */
    private String userInput;

    /** 最终回复；失败或中途取消时可能为 null */
    private String finalReply;

    /** 结束原因：FINISH / CLARIFY / OVER_MAX_LOOP_TIMES / CANCELLED / FAILED */
    private String finishReason;

    /** 本轮输入 token 合计 */
    private Integer inputToken;

    /** 本轮输出 token 合计 */
    private Integer outputToken;

    /** 被哪条摘要遮蔽；null 表示可见 */
    private Long compactedBy;

    /** 被遮蔽时间 */
    private LocalDateTime compactedAt;

    /** 一轮开始时间 */
    private LocalDateTime createdAt;

    /** 回复写定时间 */
    private LocalDateTime finishedAt;

    /** 结束原因常量 */
    public static final class FinishReason {
        public static final String FINISH = "FINISH";
        public static final String CLARIFY = "CLARIFY";
        public static final String OVER_MAX_LOOP_TIMES = "OVER_MAX_LOOP_TIMES";
        public static final String CANCELLED = "CANCELLED";
        public static final String FAILED = "FAILED";

        private FinishReason() {
        }
    }
}
