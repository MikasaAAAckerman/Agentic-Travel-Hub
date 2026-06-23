package com.travel.aiagent.common.aspect;

import com.travel.common.domain.LlmFeedback;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 反馈追踪切面
 *
 * 在反馈提交时，在 Jaeger Span 上标注 feedback 结果
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class FeedbackTracingAspect {

    private final Tracer tracer;

    /**
     * 拦截反馈提交，记录到 Jaeger Span
     */
    @Around("execution(* com.travel.aiagent.common.service.FeedbackService.submitFeedback(..))")
    public Object traceFeedback(ProceedingJoinPoint joinPoint) throws Throwable {
        Span currentSpan = tracer.currentSpan();

        try {
            // 执行原方法
            Object result = joinPoint.proceed();

            // 如果是 LlmFeedback 对象，记录到 Span
            if (result instanceof LlmFeedback feedback && currentSpan != null) {
                currentSpan.tag("feedback.trace_id", feedback.getTraceId());
                currentSpan.tag("feedback.user_id", feedback.getUserId());
                currentSpan.tag("feedback.agent_name", feedback.getAgentName());
                currentSpan.tag("feedback.rating", String.valueOf(feedback.getRating()));
                currentSpan.tag("feedback.rating_text", feedback.getRating() == 1 ? "like" : "dislike");

                if (feedback.getComment() != null && !feedback.getComment().isEmpty()) {
                    currentSpan.tag("feedback.comment", feedback.getComment());
                }

                log.debug("[Tracing] 反馈已记录到 Span | traceId={} | rating={}",
                        feedback.getTraceId(), feedback.getRating());
            }

            return result;
        } catch (Exception e) {
            // 记录异常到 Span
            if (currentSpan != null) {
                currentSpan.tag("feedback.error", e.getMessage());
            }
            throw e;
        }
    }
}
