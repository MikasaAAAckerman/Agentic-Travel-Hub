package com.travel.aiagent.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 反馈指标采集
 *
 * 使用 Micrometer 采集反馈相关的 Prometheus 指标
 */
@Component
@Slf4j
public class FeedbackMetrics {

    /**
     * 反馈计数器（按 agentName 和 rating 分组）
     */
    private final Counter feedbackCounter;

    /**
     * 点赞计数器
     */
    private final Counter likeCounter;

    /**
     * 点踩计数器
     */
    private final Counter dislikeCounter;

    private final MeterRegistry meterRegistry;

    public FeedbackMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化总计数器
        this.feedbackCounter = Counter.builder("llm_feedback_total")
                .description("LLM 反馈总数")
                .register(meterRegistry);

        this.likeCounter = Counter.builder("llm_feedback_like_total")
                .description("LLM 点赞总数")
                .register(meterRegistry);

        this.dislikeCounter = Counter.builder("llm_feedback_dislike_total")
                .description("LLM 点踩总数")
                .register(meterRegistry);

        log.info("[Metrics] 初始化反馈指标采集");
    }

    /**
     * 记录反馈（带标签）
     *
     * @param agentName Agent 名称
     * @param rating    评分（1=赞, -1=踩）
     * @param userId    用户 ID
     */
    public void recordFeedback(String agentName, int rating, String userId) {
        // 增加总计数器
        feedbackCounter.increment();

        // 增加分类计数器
        if (rating == 1) {
            likeCounter.increment();
        } else {
            dislikeCounter.increment();
        }

        // 带标签的计数器（按 Agent 和 rating 分组）
        Counter.builder("llm_feedback_count")
                .description("LLM 反馈计数（按 Agent 和 rating 分组）")
                .tag("agent_name", agentName)
                .tag("rating", rating == 1 ? "like" : "dislike")
                .register(meterRegistry)
                .increment();

        log.debug("[Metrics] 记录反馈 | agentName={} | rating={} | userId={}", agentName, rating, userId);
    }

    /**
     * 记录点踩原因（带标签）
     *
     * @param agentName Agent 名称
     * @param reason    点踩原因
     */
    public void recordDislikeReason(String agentName, String reason) {
        if (reason != null && !reason.isEmpty()) {
            Counter.builder("llm_feedback_dislike_reason")
                    .description("LLM 点踩原因统计")
                    .tag("agent_name", agentName)
                    .tag("reason", reason)
                    .register(meterRegistry)
                    .increment();

            log.debug("[Metrics] 记录点踩原因 | agentName={} | reason={}", agentName, reason);
        }
    }

    /**
     * 记录反馈处理耗时
     *
     * @param agentName Agent 名称
     * @param duration  耗时（毫秒）
     */
    public void recordFeedbackDuration(String agentName, long duration) {
        meterRegistry.timer("llm_feedback_duration",
                        "agent_name", agentName)
                .record(java.time.Duration.ofMillis(duration));

        log.debug("[Metrics] 记录反馈耗时 | agentName={} | duration={}ms", agentName, duration);
    }
}
