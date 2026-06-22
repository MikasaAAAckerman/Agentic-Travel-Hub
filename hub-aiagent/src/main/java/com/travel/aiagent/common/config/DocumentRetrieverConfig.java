package com.travel.aiagent.common.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DashScope API 配置
 * 提供 DashScopeApi Bean，供百炼召回通道和其他模块使用
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class DocumentRetrieverConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    @Bean
    public DashScopeApi dashscopeApi() {
        log.info("[Config] 初始化 DashScopeApi");
        return DashScopeApi.builder().apiKey(dashscopeApiKey).build();
    }

}
