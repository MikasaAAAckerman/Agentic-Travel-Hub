package com.travel.aiagent.common.config;

import com.travel.aiagent.common.advisor.MyChatModelCallAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * chatmodel.call太麻烦了，直接用chatClient封装类和大模型交互方便点
 * 用于定义出需要用到的chatClient
 *
 * <p>重要说明：因为 spring-ai-starter-model-openai 的自动配置带 @ConditionalOnMissingBean，
 * 一旦手写了任何 OpenAiChatModel/OpenAiApi，自动配置就会退让。所以这里两个模型
 * （DeepSeek + 千问）都改为完全手写，不依赖自动配置。
 */
@Slf4j
@Configuration
public class ChatClientConfig {

    // ═══════════════════════════════════════════════════════════
    // DeepSeek：手写 OpenAiApi + OpenAiChatModel（读 spring.ai.openai 配置）
    // ═══════════════════════════════════════════════════════════

    @Bean("deepseekOpenAiApi")
    public OpenAiApi deepseekOpenAiApi(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl) {
        log.info("[Config] 初始化 DeepSeek API | baseUrl={}", baseUrl);
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Bean("deepseekChatModel")
    public OpenAiChatModel deepseekChatModel(
            @Qualifier("deepseekOpenAiApi") OpenAiApi deepseekOpenAiApi,
            @Value("${spring.ai.openai.chat.options.model}") String model) {
        log.info("[Config] 初始化 DeepSeek ChatModel | model={}", model);
        return OpenAiChatModel.builder()
                .openAiApi(deepseekOpenAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.0)
                        .build())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 千问：手写 OpenAiApi + OpenAiChatModel（走 OpenAI 兼容模式）
    // 原因：qwen3.7-max 是「OpenAI 兼容模式」新模型，DashScope 原生协议调不通（报 url error）
    // ═══════════════════════════════════════════════════════════

    @Bean("qwenOpenAiApi")
    public OpenAiApi qwenOpenAiApi(
            @Value("${qwen-compatible.api-key}") String apiKey,
            @Value("${qwen-compatible.base-url}") String baseUrl) {
        log.info("[Config] 初始化千问 OpenAI 兼容 API | baseUrl={}", baseUrl);
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Bean("qwenChatModel")
    public OpenAiChatModel qwenChatModel(
            @Qualifier("qwenOpenAiApi") OpenAiApi qwenOpenAiApi,
            @Value("${qwen-compatible.model}") String model) {
        log.info("[Config] 初始化千问 ChatModel | model={}", model);
        return OpenAiChatModel.builder()
                .openAiApi(qwenOpenAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.0)
                        .build())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // ChatClient 定义
    // ═══════════════════════════════════════════════════════════

    // 用来和通义千问通信的ChatClient
    @Bean("qwenChatClient")
    public ChatClient qwenChatClient(@Qualifier("qwenChatModel") OpenAiChatModel qwenChatModel) {
        log.info("[Config] 初始化 Qwen ChatClient（OpenAI 兼容模式）");
        return ChatClient.builder(qwenChatModel)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .temperature(0.0) // 数值越低越理性
                                .build()
                )
                .defaultAdvisors(new MyChatModelCallAdvisor(qwenChatModel))
                .build();
    }

    // 用来和deepseek通信的ChatClient
    @Bean("deepseekChatClient")
    public ChatClient deepseekChatClient(
            @Qualifier("deepseekChatModel") OpenAiChatModel deepSeekChatModel) {
        log.info("[Config] 初始化 DeepSeek ChatClient");
        return ChatClient.builder(deepSeekChatModel)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .temperature(0.0) // 数值越低越理性
                                .build()
                )
                .defaultAdvisors(new MyChatModelCallAdvisor(deepSeekChatModel))
                .build();
    }

    // 用来和deepseek通信的Planner Client
    @Bean("deepseekPlannerClient")
    public ChatClient deepseekPlannerClient(
            @Qualifier("deepseekChatModel") OpenAiChatModel deepSeekChatModel) {
        log.info("[Config] 初始化 DeepSeek Planner Client");
        return ChatClient.builder(deepSeekChatModel)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .temperature(0.0) // 数值越低越理性
                                .build()
                )
                .defaultAdvisors(new MyChatModelCallAdvisor(deepSeekChatModel))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 以下为旧代码（DashScope 原生协议），因 qwen3.7-max 不支持原生协议，已废弃但保留备查
    // ═══════════════════════════════════════════════════════════

//    @Resource
//    private DashScopeApi dashScopeApi;
//
//    // 旧：用来和通义千问通信的ChatClient（走 DashScope 原生协议）
//    @Bean("qwenChatClient")
//    public ChatClient qwenChatClientOld(DashScopeChatModel dashScopeChatModel) {
//        log.info("[Config] 初始化 Qwen ChatClient（旧·原生协议）");
//        return ChatClient.builder(dashScopeChatModel)
//                .defaultOptions(
//                        DashScopeChatOptions.builder()
//                                .temperature(0.0)
//                                .build()
//                )
//                .defaultAdvisors(new MyChatModelCallAdvisor(dashScopeChatModel))
//                .build();
//    }

}
