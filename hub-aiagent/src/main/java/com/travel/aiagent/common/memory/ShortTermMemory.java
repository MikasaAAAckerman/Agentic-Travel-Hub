package com.travel.aiagent.common.memory;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 短期记忆体，负责储存、读取和语义压缩短期记忆。
 */
@Slf4j
@Service
public class ShortTermMemory {

    @Resource
    private ChatMemory chatMemory;

    @Resource
    @Qualifier("qwenChatClient")
    private ChatClient qwenChatClient;

    /** 当对话消息数超过此阈值时触发压缩 */
    private static final int COMPRESS_THRESHOLD = 10;

    public String getMemoryByUserIdAndChatId(String userId, String chatId) {
        log.info(" 开始获取用户对话记忆");
        String conversationId = this.getConversationId(userId, chatId);
        List<Message> messageList = chatMemory.get(conversationId);
        if (CollectionUtils.isEmpty(messageList)) {
            return "暂无对话记忆，这是你们的初次对话";
        }

        // 先压缩再返回，确保 Planner 拿到的上下文是紧凑的
        this.coverMemoryIfNeeded(conversationId, messageList, userId, chatId);
        messageList = chatMemory.get(conversationId);

        StringBuilder result = new StringBuilder();
        for (Message message : messageList) {
            if (message.getMessageType().getValue().equals(MessageType.USER.getValue())) {
                result.append(" 用户说：'").append(message.getText()).append("。'  ");
            } else {
                result.append(" Agent说：'").append(message.getText()).append("。'  ");
            }
        }
        log.info(" userId -> {} , chatId ->{} 获取用户对话记忆结果 ->  {} ", userId, chatId, result.toString());
        return result.toString();
    }

    public Boolean addUserTalking(String userId, String chatId, String message) {
        String conversationId = this.getConversationId(userId, chatId);
        return this.safeAdd(conversationId, new UserMessage(message));
    }

    public Boolean addAgentTalking(String userId, String chatId, String message) {
        String conversationId = this.getConversationId(userId, chatId);
        return this.safeAdd(conversationId, new AssistantMessage(message));
    }

    /**
     * 语义压缩：当消息数超过阈值时，用 LLM 把旧对话压缩成摘要，覆盖回 ChatMemory。
     */
    public Boolean coverMemoryByUserIdAndChatId(String userId, String chatId) {
        String conversationId = this.getConversationId(userId, chatId);
        List<Message> messageList = chatMemory.get(conversationId);
        return this.coverMemoryIfNeeded(conversationId, messageList, userId, chatId);
    }

    private String getConversationId(String userId, String chatId) {
        return userId + "-" + chatId;
    }

    private Boolean safeAdd(String conversationId, Message message) {
        try {
            chatMemory.add(conversationId, message);
            return true;
        } catch (Exception e) {
            log.error("添加消息失败", e);
            return false;
        }
    }

    private boolean coverMemoryIfNeeded(String conversationId, List<Message> messages,
                                        String userId, String chatId) {
        if (messages.size() < COMPRESS_THRESHOLD) {
            return false;
        }

        log.info(" 触发记忆压缩 | 压缩前消息数={}", messages.size());

        try {
            // 1. 把旧消息拼成文本
            StringBuilder raw = new StringBuilder();
            for (Message m : messages) {
                String role = m.getMessageType().getValue().equals(MessageType.USER.getValue()) ? "用户" : "Agent";
                raw.append(role).append("：").append(m.getText()).append("\n");
            }

            // 2. 让 LLM 压缩成结构化摘要
            String compressed = qwenChatClient.prompt()
                    .user("""
                            请把以下对话历史压缩成一段不超过200字的摘要，但是如有必要，可以扩展到500字
                            保留关键信息：目的地、日期、人数、预算、偏好、已确定的行程细节。
                            丢失无关的闲聊和语气词。只输出摘要文本，不要加任何前缀。

                            对话历史：
                            %s
                            """.formatted(raw.toString()))
                    .call()
                    .content();

            log.info(" 压缩完成 | 压缩后长度={}", compressed.length());

            // 3. 清空旧记忆
            chatMemory.clear(conversationId);

            // 4. 写回压缩摘要（用 AssistantMessage 不会被 evict）
            chatMemory.add(conversationId,
                    new AssistantMessage("[对话历史摘要] " + compressed));

            log.info(" 记忆压缩完成 | 压缩前{}条 → 压缩后1条摘要", messages.size());
            return true;

        } catch (Exception e) {
            log.error("记忆压缩失败，保留原始记忆", e);
            return false;
        }
    }
}
