package com.travel.aiagent.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatMemory配置类
 */
@Slf4j
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory() {
        log.info("[Config] 初始化 ChatMemory ");
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        log.debug("[Config] ChatMemory 配置完成");
        return chatMemory;
    }

}
