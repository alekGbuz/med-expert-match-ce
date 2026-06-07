package com.berdachuk.medexpertmatch.core.advisor;

import com.berdachuk.medexpertmatch.core.util.LlmDateTimeContext;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

/**
 * Injects current UTC date/time into every LLM request so models always have temporal context.
 */
public class DateTimeContextAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public String getName() {
        return "dateTimeContextAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(augmentRequest(request));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(augmentRequest(request));
    }

    private static ChatClientRequest augmentRequest(ChatClientRequest request) {
        String dateTimeBlock = LlmDateTimeContext.contextBlock();
        return request.mutate()
                .prompt(request.prompt().augmentSystemMessage(systemMessage -> systemMessage.mutate()
                        .text(combine(dateTimeBlock, systemMessage.getText()))
                        .build()))
                .build();
    }

    private static String combine(String dateTimeBlock, String existing) {
        if (existing == null || existing.isBlank()) {
            return dateTimeBlock;
        }
        if (existing.startsWith(dateTimeBlock)) {
            return existing;
        }
        return dateTimeBlock + "\n\n" + existing;
    }
}
