package com.travel.hubtools.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 全局大模型与外部接口容错配置 (Resilience4j)
 * 保护系统免受 LLM 接口限流、超时和雪崩的伤害喵！
 */
@Configuration
public class ResilienceConfig {

    /**
     * 针对大模型 API 的专属熔断与超时配置
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                // 1. 严格限时器：大模型如果超过 60 秒还不吐字，直接掐死，防止线程被永久挂起喵！
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(60))
                        .build())
                // 2. 熔断器：如果失败率超过 50%，或者慢调用比例过高，直接打开开关，保护系统喵！
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        // 统计窗口大小：最近 10 次调用
                        .slidingWindowSize(10)
                        // 失败率阈值：50%
                        .failureRateThreshold(50.0f)
                        // 慢调用阈值：超过 45 秒算慢调用
                        .slowCallDurationThreshold(Duration.ofSeconds(45))
                        // 慢调用比例阈值：50%
                        .slowCallRateThreshold(50.0f)
                        // 熔断器打开后，等待 30 秒再尝试半开（放行少量请求测试大模型是否恢复）
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        // 允许在半开状态下的调用次数
                        .permittedNumberOfCallsInHalfOpenState(3)
                        // 最小评估请求数：至少有 5 次请求才会开始计算失败率
                        .minimumNumberOfCalls(5)
                        .build())
                .build());
    }

    /**
     * 针对限流 (HTTP 429) 和网络抖动的指数退避重试策略喵！
     */
    @Bean
    public RetryConfig llmRetryConfig() {
        return RetryConfig.custom()
                // 最多重试 3 次
                .maxAttempts(3)
                // 指数退避：第一次失败等 2 秒，第二次等 4 秒，第三次等 8 秒... 防止把 API 打挂喵！
                .waitDuration(Duration.ofSeconds(2))
                // 核心：遇到哪些异常才重试？如果是入参写错了（400），重试一百万次也没用！必须精准捕获！
                .retryExceptions(
                        java.net.SocketTimeoutException.class,
                        java.io.IOException.class,
                        // 这里可以替换成你使用的特定 HttpClient 的异常，或者 Spring AI 的 API 异常
                        org.springframework.web.client.HttpServerErrorException.class,
                        org.springframework.web.client.ResourceAccessException.class
                )
                // 忽略客户端自身导致的异常（如 400 Bad Request, 401 Unauthorized）
                .ignoreExceptions(
                        org.springframework.web.client.HttpClientErrorException.class
                )
                .build();
    }
}