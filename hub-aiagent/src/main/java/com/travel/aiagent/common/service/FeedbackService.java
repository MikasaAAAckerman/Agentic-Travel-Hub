package com.travel.aiagent.common.service;

import com.travel.aiagent.common.metrics.FeedbackMetrics;
import com.travel.aiagent.common.repository.FeedbackRepository;
import com.travel.common.domain.FeedbackRequest;
import com.travel.common.domain.FeedbackStats;
import com.travel.common.domain.LlmFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 反馈服务
 *
 * 处理用户反馈的业务逻辑
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMetrics feedbackMetrics;

    /**
     * 提交反馈
     *
     * @param request 反馈请求
     * @return 反馈记录
     */
    public LlmFeedback submitFeedback(FeedbackRequest request) {
        // 参数校验
        if (request.getTraceId() == null || request.getTraceId().isEmpty()) {
            throw new IllegalArgumentException("traceId 不能为空");
        }
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (request.getAgentName() == null || request.getAgentName().isEmpty()) {
            throw new IllegalArgumentException("agentName 不能为空");
        }
        if (request.getRating() == null || (request.getRating() != 1 && request.getRating() != -1)) {
            throw new IllegalArgumentException("rating 必须是 1 或 -1");
        }

        // 检查是否已经反馈过
        LlmFeedback existing = feedbackRepository.findByTraceId(request.getTraceId());
        if (existing != null) {
            log.warn("[Feedback] 重复提交反馈 | traceId={}", request.getTraceId());
            throw new RuntimeException("该请求已经反馈过，不能重复提交");
        }

        // 构建反馈记录
        LlmFeedback feedback = LlmFeedback.builder()
                .traceId(request.getTraceId())
                .userId(request.getUserId())
                .agentName(request.getAgentName())
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        // 插入数据库
        feedbackRepository.insert(feedback);

        // 记录 Prometheus 指标
        feedbackMetrics.recordFeedback(feedback.getAgentName(), feedback.getRating(), feedback.getUserId());

        // 如果是点踩，记录点踩原因
        if (feedback.getRating() == -1 && feedback.getComment() != null) {
            feedbackMetrics.recordDislikeReason(feedback.getAgentName(), feedback.getComment());
        }

        log.info("[Feedback] 反馈提交成功 | traceId={} | userId={} | agentName={} | rating={}",
                feedback.getTraceId(), feedback.getUserId(), feedback.getAgentName(), feedback.getRating());

        return feedback;
    }

    /**
     * 获取反馈统计
     *
     * @return 统计结果
     */
    public FeedbackStats getFeedbackStats() {
        long totalFeedback = feedbackRepository.count();
        long likeCount = feedbackRepository.countLikes();
        long dislikeCount = feedbackRepository.countDislikes();

        // 计算赞踩比
        double likeDislikeRatio = dislikeCount == 0 ? likeCount : (double) likeCount / dislikeCount;

        // 计算点踩率
        double dislikeRate = totalFeedback == 0 ? 0 : (double) dislikeCount / totalFeedback;

        // 按 Agent 分组统计
        Map<String, FeedbackStats.AgentFeedbackStats> agentStats = new HashMap<>();
        List<String> agentNames = feedbackRepository.findAllAgentNames();

        for (String agentName : agentNames) {
            long agentLikes = feedbackRepository.countLikesByAgent(agentName);
            long agentDislikes = feedbackRepository.countDislikesByAgent(agentName);
            double agentRatio = agentDislikes == 0 ? agentLikes : (double) agentLikes / agentDislikes;

            agentStats.put(agentName, FeedbackStats.AgentFeedbackStats.builder()
                    .agentName(agentName)
                    .likeCount(agentLikes)
                    .dislikeCount(agentDislikes)
                    .likeDislikeRatio(agentRatio)
                    .build());
        }

        return FeedbackStats.builder()
                .totalFeedback(totalFeedback)
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .likeDislikeRatio(likeDislikeRatio)
                .dislikeRate(dislikeRate)
                .agentStats(agentStats)
                .build();
    }

    /**
     * 获取点踩的反馈列表（用于分析）
     *
     * @param limit 返回数量
     * @return 点踩反馈列表
     */
    public List<LlmFeedback> getDislikeFeedbacks(int limit) {
        return feedbackRepository.findDislikes(limit);
    }

    /**
     * 获取点踩原因 Top N
     *
     * @param limit 返回数量
     * @return 点踩原因列表
     */
    public List<Map<String, Object>> getTopDislikeReasons(int limit) {
        return feedbackRepository.findTopDislikeReasons(limit);
    }

    /**
     * 按 Agent 名称查询反馈
     *
     * @param agentName Agent 名称
     * @return 反馈列表
     */
    public List<LlmFeedback> getFeedbacksByAgent(String agentName) {
        return feedbackRepository.findByAgentName(agentName);
    }
}
