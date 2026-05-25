package com.travel.aiagent.common.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.travel.aiagent.common.advisor.MyChatModelCallAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * chatmodel.call太麻烦了，直接用chatClient封装类和大模型交互方便点
 * 用于定义出需要用到的chatClient
 */
@Slf4j
@Configuration
public class ChatClientConfig {

    @Resource
    private DashScopeApi dashScopeApi;

    // 用来和通义千问通信的ChatClient
    @Bean("qwenChatClient")
    public ChatClient qwenChatClient(DashScopeChatModel dashScopeChatModel) {
        /*
        * https://springdoc.cn/spring-ai/api/chatclient.html
        * 官网写法，通过yml控温，但是考虑到可能会出现多个qwenClient，所以手动builder，可以实现不动yml的情况下造出多个温度的client
        return ChatClient.create(dashScopeChatModel); */
        log.info("[Config] 初始化 Qwen ChatClient | temperature=0.0 (理性模式)");

        return ChatClient.builder(dashScopeChatModel)
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .temperature(0.0) // 数值越低越理性
                                .build()
                )
                .defaultAdvisors(new MyChatModelCallAdvisor(dashScopeChatModel))
                .build();
    }

    // 用来和deepseek通信的ChatClient
    @Bean("deepseekChatClient")
    public ChatClient deepseekChatClient(OpenAiChatModel deepSeekChatModel) {
        /*
        * https://springdoc.cn/spring-ai/api/chatclient.html
        * 官网写法，通过yml控温，但是考虑到可能会出现多个Client，所以手动builder，可以实现不动yml的情况下造出多个温度的client
        return ChatClient.create(dashScopeChatModel); */
        log.info("[Config] 初始化 DeepSeek ChatClient | temperature=0.0 (理性模式)");
        return ChatClient.builder(deepSeekChatModel)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .temperature(0.0) // 数值越低越理性
                                .build()
                )
                .defaultAdvisors(new MyChatModelCallAdvisor(deepSeekChatModel))
                .build();
    }

    // 用来和deepseek通信的ChatClient
    @Bean("deepseekPlannerClient")
    public ChatClient deepseekPlannerClient(OpenAiChatModel openAiChatModel) {
        /*
        * https://springdoc.cn/spring-ai/api/chatclient.html
        * 官网写法，通过yml控温，但是考虑到可能会出现多个Client，所以手动builder，可以实现不动yml的情况下造出多个温度的client
        return ChatClient.create(dashScopeChatModel); */
        log.info("[Config] 初始化 DeepSeek Planner Client | temperature=0.0 (规划专用)");
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .temperature(0.0) // 数值越低越理性
                                .build()
                )
                .defaultAdvisors(new MyChatModelCallAdvisor(openAiChatModel))
                .build();
    }


}
