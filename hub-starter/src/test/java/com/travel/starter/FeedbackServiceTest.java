package com.travel.starter;

import com.travel.aiagent.common.service.FeedbackService;
import com.travel.common.domain.FeedbackRequest;
import com.travel.common.domain.FeedbackStats;
import com.travel.common.domain.LlmFeedback;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * 🧪 反馈服务测试
 *
 * 演示如何使用反馈服务收集用户反馈
 *
 * 运行前确保：
 * 1. PG 容器已启动
 * 2. 已执行 sql/init_feedback.sql 建表
 * 3. application-dev.yml 配置正确
 */
@Slf4j
@SpringBootTest
public class FeedbackServiceTest {

    @Autowired
    private FeedbackService feedbackService;

    /**
     * 测试1：提交点赞反馈
     */
    @Test
    void testSubmitLikeFeedback() {
        FeedbackRequest request = FeedbackRequest.builder()
                .traceId("trace-001")
                .userId("user-001")
                .agentName("Orchestrator")
                .rating(1)  // 点赞
                .build();

        LlmFeedback feedback = feedbackService.submitFeedback(request);

        log.info("========================================");
        log.info("✅ 点赞反馈提交成功");
        log.info("========================================");
        log.info("traceId：{}", feedback.getTraceId());
        log.info("userId：{}", feedback.getUserId());
        log.info("agentName：{}", feedback.getAgentName());
        log.info("rating：{}", feedback.getRating());
        log.info("createdAt：{}", feedback.getCreatedAt());
        log.info("========================================");
    }

    /**
     * 测试2：提交点踩反馈（带原因）
     */
    @Test
    void testSubmitDislikeFeedback() {
        FeedbackRequest request = FeedbackRequest.builder()
                .traceId("trace-002")
                .userId("user-001")
                .agentName("Planner")
                .rating(-1)  // 点踩
                .comment("回复太笼统，没有具体信息")
                .build();

        LlmFeedback feedback = feedbackService.submitFeedback(request);

        log.info("========================================");
        log.info("❌ 点踩反馈提交成功");
        log.info("========================================");
        log.info("traceId：{}", feedback.getTraceId());
        log.info("userId：{}", feedback.getUserId());
        log.info("agentName：{}", feedback.getAgentName());
        log.info("rating：{}", feedback.getRating());
        log.info("comment：{}", feedback.getComment());
        log.info("createdAt：{}", feedback.getCreatedAt());
        log.info("========================================");
    }

    /**
     * 测试3：批量提交反馈（模拟真实场景）
     */
    @Test
    void testBatchSubmitFeedback() {
        log.info("🚀 开始批量提交反馈...");

        // 模拟 Orchestrator 的反馈
        submitFeedback("trace-010", "user-001", "Orchestrator", 1, null);
        submitFeedback("trace-011", "user-002", "Orchestrator", 1, null);
        submitFeedback("trace-012", "user-003", "Orchestrator", -1, "回复不够详细");
        submitFeedback("trace-013", "user-004", "Orchestrator", 1, null);
        submitFeedback("trace-014", "user-005", "Orchestrator", -1, "理解错误");

        // 模拟 Planner 的反馈
        submitFeedback("trace-020", "user-001", "Planner", -1, "规划不合理");
        submitFeedback("trace-021", "user-002", "Planner", -1, "步骤太多");
        submitFeedback("trace-022", "user-003", "Planner", 1, null);
        submitFeedback("trace-023", "user-004", "Planner", -1, "工具选择错误");
        submitFeedback("trace-024", "user-005", "Planner", 1, null);

        // 模拟 Worker 的反馈
        submitFeedback("trace-030", "user-001", "Worker", 1, null);
        submitFeedback("trace-031", "user-002", "Worker", 1, null);
        submitFeedback("trace-032", "user-003", "Worker", 1, null);
        submitFeedback("trace-033", "user-004", "Worker", -1, "执行失败");
        submitFeedback("trace-034", "user-005", "Worker", 1, null);

        log.info("✅ 批量提交完成！");
    }

    /**
     * 测试4：获取反馈统计
     */
    @Test
    void testGetFeedbackStats() {
        FeedbackStats stats = feedbackService.getFeedbackStats();

        log.info("========================================");
        log.info("📊 反馈统计");
        log.info("========================================");
        log.info("总反馈数：{}", stats.getTotalFeedback());
        log.info("点赞数：{}", stats.getLikeCount());
        log.info("点踩数：{}", stats.getDislikeCount());
        log.info("赞踩比：{:.2f}", stats.getLikeDislikeRatio());
        log.info("点踩率：{:.2f}%", stats.getDislikeRate() * 100);
        log.info("----------------------------------------");

        // 按 Agent 分组统计
        log.info("各 Agent 统计：");
        for (Map.Entry<String, FeedbackStats.AgentFeedbackStats> entry : stats.getAgentStats().entrySet()) {
            FeedbackStats.AgentFeedbackStats agentStats = entry.getValue();
            log.info("  {}：👍 {} | 👎 {} | 赞踩比 {:.2f}",
                    agentStats.getAgentName(),
                    agentStats.getLikeCount(),
                    agentStats.getDislikeCount(),
                    agentStats.getLikeDislikeRatio());
        }

        log.info("========================================");
    }

    /**
     * 测试5：获取点踩原因 Top N
     */
    @Test
    void testGetTopDislikeReasons() {
        List<Map<String, Object>> reasons = feedbackService.getTopDislikeReasons(5);

        log.info("========================================");
        log.info("📊 点踩原因 Top 5");
        log.info("========================================");

        for (int i = 0; i < reasons.size(); i++) {
            Map<String, Object> reason = reasons.get(i);
            log.info("  [{}] {} ({}次)", i + 1, reason.get("comment"), reason.get("count"));
        }

        log.info("========================================");
    }

    /**
     * 测试6：按 Agent 查询反馈
     */
    @Test
    void testGetFeedbacksByAgent() {
        String agentName = "Planner";
        List<LlmFeedback> feedbacks = feedbackService.getFeedbacksByAgent(agentName);

        log.info("========================================");
        log.info("📋 {} 的反馈列表", agentName);
        log.info("========================================");

        for (LlmFeedback feedback : feedbacks) {
            String emoji = feedback.getRating() == 1 ? "👍" : "👎";
            log.info("  {} traceId={} | userId={} | comment={}",
                    emoji, feedback.getTraceId(), feedback.getUserId(),
                    feedback.getComment() != null ? feedback.getComment() : "无");
        }

        log.info("========================================");
    }

    /**
     * 辅助方法：提交反馈
     */
    private void submitFeedback(String traceId, String userId, String agentName, int rating, String comment) {
        try {
            FeedbackRequest request = FeedbackRequest.builder()
                    .traceId(traceId)
                    .userId(userId)
                    .agentName(agentName)
                    .rating(rating)
                    .comment(comment)
                    .build();

            feedbackService.submitFeedback(request);
            String emoji = rating == 1 ? "👍" : "👎";
            log.info("  {} {} | {} | {}", emoji, traceId, agentName, comment != null ? comment : "");
        } catch (Exception e) {
            log.warn("  ⚠️ 提交失败：{} | {}", traceId, e.getMessage());
        }
    }
}
