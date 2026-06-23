package com.travel.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM 回复质量反馈实体
 *
 * 用于存储用户的点赞/点踩数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmFeedback {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联请求追踪ID
     */
    private String traceId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 回复的Agent名称
     */
    private String agentName;

    /**
     * 评分：1=赞, -1=踩
     */
    private Integer rating;

    /**
     * 用户补充原因（点踩时可选）
     */
    private String comment;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
