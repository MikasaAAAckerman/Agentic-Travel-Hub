package com.travel.aiagent.common.config;

import com.travel.aiagent.common.advisor.MyChatModelCallAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * chatmodel.call太麻烦了，直接用chatClient封装类和大模型交互方便点
 * 用于定义出需要用到的chatClient
 *
 * <p>动态模型路由：
 * 底层各 provider（deepseek/qwen/openrouter）在 yml 里声明各自的 models 映射表，
 * 角色（planner/worker/assistant）通过「引用路径」{provider}.models.{modelKey} 动态绑定模型。
 * 切换模型 = 改 agent-model.* 的引用路径一行，代码零改动。
 *
 * <p>重要：因为 spring-ai-starter-model-openai 自动配置带 @ConditionalOnMissingBean，
 * 手写任何 OpenAiChatModel 都会让自动配置退让，所以所有模型都完全手写，不依赖自动配置。
 */
@Slf4j
@Configuration
public class ChatClientConfig {

    private final Environment environment;

    public ChatClientConfig(Environment environment) {
        this.environment = environment;
    }

    // ═══════════════════════════════════════════════════════════
    // 角色层：通过引用路径动态路由
    // 引用格式：{provider}.models.{modelKey}
    // 例：agent-model.planner = qwen.models.qwen3.7-max-2026-06-08
    // ═══════════════════════════════════════════════════════════

    @Bean("plannerClient")
    public ChatClient plannerClient() {
        String ref = environment.getProperty("agent-model.planner");
        ChatModel model = createModel(ref);
        log.info("[Config] Planner 角色绑定 -> {}", ref);
        return ChatClient.builder(model)
                .defaultAdvisors(new MyChatModelCallAdvisor(model))
                .build();
    }

    @Bean("workerClient")
    public ChatClient workerClient() {
        String ref = environment.getProperty("agent-model.worker");
        ChatModel model = createModel(ref);
        log.info("[Config] Worker 角色绑定 -> {}", ref);
        return ChatClient.builder(model)
                .defaultAdvisors(new MyChatModelCallAdvisor(model))
                .build();
    }

    // 辅助功能（闲聊 / 评测 / 意图识别 / 记忆压缩），固定用便宜的模型
    @Bean("assistantClient")
    public ChatClient assistantClient() {
        String ref = environment.getProperty("agent-model.assistant");
        ChatModel model = createModel(ref);
        log.info("[Config] 辅助功能绑定 -> {}", ref);
        return ChatClient.builder(model)
                .defaultAdvisors(new MyChatModelCallAdvisor(model))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 核心：根据引用路径动态构建 ChatModel
    // ═══════════════════════════════════════════════════════════

    /**
     * 根据引用路径动态构建 ChatModel。
     *
     * @param ref 引用路径，格式 {provider}.models.{modelKey}
     *            例：qwen.models.qwen3.7-max-2026-06-08
     *            → provider=qwen, modelKey=qwen3.7-max-2026-06-08
     *            → 从 yml 读 qwen.api-key / qwen.endpoint / qwen.models.qwen3.7-max-2026-06-08
     */
    private ChatModel createModel(String ref) {
        if (ref == null || ref.isBlank()) {
            throw new IllegalStateException("模型引用路径为空，请检查 agent-model.* 配置");
        }

        // 按 ".models." 切分：前半段是 provider，后半段是 modelKey
        int idx = ref.indexOf(".models.");
        if (idx <= 0) {
            throw new IllegalStateException("模型引用路径格式错误，应为 {provider}.models.{modelKey}，实际：" + ref);
        }
        String provider = ref.substring(0, idx);
        String modelKey = ref.substring(idx + ".models.".length());

        String apiKey = environment.getProperty(provider + ".api-key");
        String endpoint = environment.getProperty(provider + ".endpoint");
        String model = environment.getProperty(provider + ".models." + modelKey);

        if (apiKey == null || endpoint == null || model == null) {
            throw new IllegalStateException("模型配置缺失 | provider=" + provider
                    + " | modelKey=" + modelKey
                    + " | apiKey存在=" + (apiKey != null)
                    + " | endpoint存在=" + (endpoint != null)
                    + " | model存在=" + (model != null));
        }

        log.info("[Config] 动态构建模型 | provider={} | modelKey={} | 实际模型名={} | endpoint={}",
                provider, modelKey, model, endpoint);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(endpoint)
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0) // 数值越低越理性
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }


}
