package com.travel.aiagent.common.config;

import com.travel.aiagent.common.advisor.MyChatModelCallAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * chatmodel.call太麻烦了，直接用chatClient封装类和大模型交互方便点
 * 用于定义出需要用到的chatClient
 *
 * <p>双模型对称配置 + 角色路由：
 * 底层定义多个 ChatModel（DeepSeek / 千问 / OpenRouter），
 * 上层用「角色」plannerClient / workerClient 通过 agent-model.* 配置路由到具体模型，
 * 切换模型只需改 yml 一行，Service 层零改动。
 *
 * <p>重要：因为 spring-ai-starter-model-openai 自动配置带 @ConditionalOnMissingBean，
 * 手写任何 OpenAiChatModel 都会让自动配置退让，所以所有模型都完全手写，不依赖自动配置。
 */
@Slf4j
@Configuration
public class ChatClientConfig {

    @Value("${openrouter.api-key}")
    String openRouterApiKey;

    @Value("${openrouter.endpoint}")
    String openRouterApiUrl;

    // ═══════════════════════════════════════════════════════════
    // 模型层：定义各种可用的 ChatModel
    // ═══════════════════════════════════════════════════════════

    @Bean(name = "deepseekChatModel")
    public ChatModel deepseekChatModel(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.endpoint}") String endpoint,
            @Value("${deepseek.model}") String model) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(endpoint)
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0) // 数值越低越理性
                .build();
        log.info("[Config] 初始化 DeepSeek ChatModel | model={} | endpoint={}", model, endpoint);
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean(name = "qwenChatModel")
    public ChatModel qwenChatModel(
            @Value("${qwen.api-key}") String apiKey,
            @Value("${qwen.endpoint}") String endpoint,
            @Value("${qwen.model}") String model) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(endpoint)
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .build();
        log.info("[Config] 初始化千问 ChatModel | model={} | endpoint={}", model, endpoint);
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean(name = "openRouterDeepseekChatModel")
    public ChatModel openRouterDeepseekChatModel(
            @Value("${openrouter.models.deepseek-v4-pro}") String model) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(openRouterApiUrl)
                .apiKey(openRouterApiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .build();
        log.info("[Config] 初始化 OpenRouter DeepSeek Pro ChatModel | model={}", model);
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean(name = "openRouterFlashChatModel")
    public ChatModel openRouterFlashChatModel(
            @Value("${openrouter.models.deepseek-v4-flash}") String model) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(openRouterApiUrl)
                .apiKey(openRouterApiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .build();
        log.info("[Config] 初始化 OpenRouter DeepSeek Flash ChatModel | model={}", model);
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 角色层：plannerClient / workerClient，通过 agent-model.* 路由到具体模型
    // ═══════════════════════════════════════════════════════════

    // ── Planner 角色：deepseek ──
    @Bean("plannerClient")
    @ConditionalOnProperty(name = "agent-model.planner", havingValue = "deepseek", matchIfMissing = true)
    public ChatClient plannerClientDeepseek(@Qualifier("deepseekChatModel") ChatModel model) {
        log.info("[Config] Planner 角色绑定 -> DeepSeek");
        return ChatClient.builder(model)
                .defaultAdvisors(new MyChatModelCallAdvisor(model))
                .build();
    }

    // ── Planner 角色：openrouter ──
    @Bean("plannerClient")
    @ConditionalOnProperty(name = "agent-model.planner", havingValue = "openrouter")
    public ChatClient plannerClientOpenrouter(@Qualifier("openRouterDeepseekChatModel") ChatModel model) {
        log.info("[Config] Planner 角色绑定 -> OpenRouter DeepSeek Pro");
        return ChatClient.builder(model)
                .defaultAdvisors(new MyChatModelCallAdvisor(model))
                .build();
    }

    // ── Worker 角色：qwen ──
    @Bean("workerClient")
    @ConditionalOnProperty(name = "agent-model.worker", havingValue = "qwen", matchIfMissing = true)
    public ChatClient workerClientQwen(@Qualifier("qwenChatModel") ChatModel model) {
        log.info("[Config] Worker 角色绑定 -> 千问");
        return ChatClient.builder(model)
                .defaultAdvisors(new MyChatModelCallAdvisor(model))
                .build();
    }

    // ── Worker 角色：openrouter ──
    @Bean("workerClient")
    @ConditionalOnProperty(name = "agent-model.worker", havingValue = "openrouter")
    public ChatClient workerClientOpenrouter(@Qualifier("openRouterFlashChatModel") ChatModel model) {
        log.info("[Config] Worker 角色绑定 -> OpenRouter DeepSeek Flash");
        return ChatClient.builder(model)
                .defaultAdvisors(new MyChatModelCallAdvisor(model))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 辅助层：固定用千问做杂活（闲聊 / 评测 / 意图识别 / 记忆压缩）
    // 这些不属于 Planner/Worker 角色，不需要参与路由切换，固定用便宜的千问即可
    // ═══════════════════════════════════════════════════════════

    @Bean("qwenChatClient")
    public ChatClient qwenChatClient(@Qualifier("qwenChatModel") ChatModel qwenChatModel) {
        log.info("[Config] 初始化 Qwen ChatClient（辅助功能）");
        return ChatClient.builder(qwenChatModel)
                .defaultAdvisors(new MyChatModelCallAdvisor(qwenChatModel))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 以下为旧代码（直接绑定具体模型的 ChatClient），已废弃但保留备查
    // ═══════════════════════════════════════════════════════════

//    // 旧：用来和deepseek通信的ChatClient
//    @Bean("deepseekChatClient")
//    public ChatClient deepseekChatClient(@Qualifier("deepseekChatModel") ChatModel deepseekChatModel) {
//        log.info("[Config] 初始化 DeepSeek ChatClient");
//        return ChatClient.builder(deepseekChatModel)
//                .defaultAdvisors(new MyChatModelCallAdvisor(deepseekChatModel))
//                .build();
//    }
//
//    // 旧：用来和deepseek通信的Planner Client
//    @Bean("deepseekPlannerClient")
//    public ChatClient deepseekPlannerClient(@Qualifier("deepseekChatModel") ChatModel deepseekChatModel) {
//        log.info("[Config] 初始化 DeepSeek Planner Client");
//        return ChatClient.builder(deepseekChatModel)
//                .defaultAdvisors(new MyChatModelCallAdvisor(deepseekChatModel))
//                .build();
//    }

}
