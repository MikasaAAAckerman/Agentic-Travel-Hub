package com.travel.aiagent.common.advisor;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientAttributes;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * 直接照搬ChatModelCallAdvisor
 */
@Slf4j
public final class MyChatModelCallAdvisor implements CallAdvisor, StreamAdvisor {

    private final ChatModel chatModel;

    public MyChatModelCallAdvisor(ChatModel chatModel) {
        log.info("[Advisor] 初始化 ChatModelCallAdvisor | model={}", chatModel.getClass().getSimpleName());
        this.chatModel = chatModel;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.debug("[Advisor] 进入 adviseCall 拦截器");
        ChatClientRequest formattedChatClientRequest = augmentWithFormatInstructions(chatClientRequest);
        log.debug("[Advisor] 增强后的请求 | promptLength={}", formattedChatClientRequest.prompt().getInstructions().size());
        
        ChatResponse chatResponse = this.chatModel.call(formattedChatClientRequest.prompt());
        log.debug("[Advisor] ChatModel 调用完成 | responseLength={}", 
                chatResponse != null && chatResponse.getResult() != null ? chatResponse.getResult().getOutput().getText().length() : 0);
        
        ChatClientResponse result = ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(Map.copyOf(formattedChatClientRequest.context()))
                .build();
        log.debug("[Advisor] adviseCall 执行完成");
        return result;
    }

    private static ChatClientRequest augmentWithFormatInstructions(ChatClientRequest chatClientRequest) {

        String outputFormat = (String) chatClientRequest.context().get(ChatClientAttributes.OUTPUT_FORMAT.getKey());

        String outputSchema = (String) chatClientRequest.context()
                .get(ChatClientAttributes.STRUCTURED_OUTPUT_SCHEMA.getKey());

        if (!StringUtils.hasText(outputFormat) && !StringUtils.hasText(outputSchema)) {
            return chatClientRequest;
        }

        if (chatClientRequest.context().containsKey(ChatClientAttributes.STRUCTURED_OUTPUT_NATIVE.getKey())
                && StringUtils.hasText(outputSchema) && chatClientRequest.prompt()
                .getOptions() instanceof StructuredOutputChatOptions structuredOutputChatOptions) {

            structuredOutputChatOptions.setOutputSchema(outputSchema);

            return chatClientRequest;
        }

        Prompt augmentedPrompt = chatClientRequest.prompt()
                .augmentUserMessage(userMessage -> userMessage.mutate()
                        .text(userMessage.getText() + System.lineSeparator() + outputFormat)
                        .build());

        return ChatClientRequest.builder()
                .prompt(augmentedPrompt)
                .context(Map.copyOf(chatClientRequest.context()))
                .build();
    }

    @Override
    public String getName() {
        return "call";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        log.debug("[Advisor] 进入 adviseStream 流式拦截器");
        log.debug("[Advisor] 流式请求 | contextSize={}", chatClientRequest.context().size());
        
        return this.chatModel.stream(chatClientRequest.prompt())
                .map(chatResponse -> ChatClientResponse.builder()
                        .chatResponse(chatResponse)
                        .context(Map.copyOf(chatClientRequest.context()))
                        .build())
                .publishOn(Schedulers.boundedElastic()); // TODO add option to disable
    }

    public static final class Builder {

        private ChatModel chatModel;

        private Builder() {
        }

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public MyChatModelCallAdvisor build() {
            return new MyChatModelCallAdvisor(this.chatModel);
        }

    }

}
