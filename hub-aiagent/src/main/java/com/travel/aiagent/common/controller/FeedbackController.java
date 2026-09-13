package com.travel.aiagent.common.controller;

import com.travel.aiagent.common.service.FeedbackService;
import com.travel.common.domain.FeedbackRequest;
import com.travel.common.domain.FeedbackStats;
import com.travel.common.domain.LlmFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 反馈控制器
 *
 * 提供用户反馈的 REST API
 */
@RestController
@RequestMapping("/api/feedback")
@Slf4j
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 提交反馈
     *
     * POST /api/feedback
     */
    @PostMapping
    public ResponseEntity<LlmFeedback> submitFeedback(@RequestBody FeedbackRequest request) {
        log.info("[Feedback] 收到反馈请求 | traceId={} | rating={}", request.getTraceId(), request.getRating());

        try {
            LlmFeedback feedback = feedbackService.submitFeedback(request);
            return ResponseEntity.ok(feedback);
        } catch (IllegalArgumentException e) {
            log.warn("[Feedback] 参数错误 | error={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.warn("[Feedback] 业务错误 | error={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取反馈统计
     *
     * GET /api/feedback/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<FeedbackStats> getFeedbackStats() {
        FeedbackStats stats = feedbackService.getFeedbackStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取点踩反馈列表（用于分析）
     *
     * GET /api/feedback/dislikes?limit=10
     */
    @GetMapping("/dislikes")
    public ResponseEntity<List<LlmFeedback>> getDislikeFeedbacks(
            @RequestParam(defaultValue = "10") int limit) {
        List<LlmFeedback> feedbacks = feedbackService.getDislikeFeedbacks(limit);
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * 获取点踩原因 Top N
     *
     * GET /api/feedback/dislike-reasons?limit=5
     */
    @GetMapping("/dislike-reasons")
    public ResponseEntity<List<Map<String, Object>>> getTopDislikeReasons(
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> reasons = feedbackService.getTopDislikeReasons(limit);
        return ResponseEntity.ok(reasons);
    }

    /**
     * 按 Agent 名称查询反馈
     *
     * GET /api/feedback/agent/{agentName}
     */
    @GetMapping("/agent/{agentName}")
    public ResponseEntity<List<LlmFeedback>> getFeedbacksByAgent(@PathVariable String agentName) {
        List<LlmFeedback> feedbacks = feedbackService.getFeedbacksByAgent(agentName);
        return ResponseEntity.ok(feedbacks);
    }
}
