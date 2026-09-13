package com.travel.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 反馈统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStats {

    /**
     * 总反馈数
     */
    private Long totalFeedback;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 点踩数
     */
    private Long dislikeCount;

    /**
     * 赞踩比
     */
    private Double likeDislikeRatio;

    /**
     * 点踩率
     */
    private Double dislikeRate;

    /**
     * 按 Agent 分组的统计
     */
    private Map<String, AgentFeedbackStats> agentStats;

    /**
     * Agent 反馈统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentFeedbackStats {
        private String agentName;
        private Long likeCount;
        private Long dislikeCount;
        private Double likeDislikeRatio;
    }
}
