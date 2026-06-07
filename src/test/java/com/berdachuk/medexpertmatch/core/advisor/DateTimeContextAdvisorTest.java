package com.berdachuk.medexpertmatch.core.advisor;

import com.berdachuk.medexpertmatch.core.util.LlmDateTimeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeContextAdvisorTest {

    private final DateTimeContextAdvisor advisor = new DateTimeContextAdvisor();

    @AfterEach
    void resetClock() {
        LlmDateTimeContext.resetClock();
    }

    @Test
    @DisplayName("user-only prompt gains system message with UTC datetime")
    void injectsSystemMessageForUserOnlyPrompt() {
        LlmDateTimeContext.setClock(Clock.fixed(Instant.parse("2026-06-08T14:30:00Z"), ZoneOffset.UTC));
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(List.of(new UserMessage("find cardiologists"))))
                .build();

        SystemMessage systemMessage = extractSystemMessageAfterAdvisor(request);

        assertNotNull(systemMessage);
        assertTrue(systemMessage.getText().contains("2026-06-08T14:30:00Z"));
    }

    @Test
    @DisplayName("existing system prompt is prefixed with UTC datetime")
    void prependsDateTimeToExistingSystemPrompt() {
        LlmDateTimeContext.setClock(Clock.fixed(Instant.parse("2026-06-08T14:30:00Z"), ZoneOffset.UTC));
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(List.of(
                        new SystemMessage("You are a medical assistant."),
                        new UserMessage("analyze case"))))
                .build();

        SystemMessage systemMessage = extractSystemMessageAfterAdvisor(request);

        assertNotNull(systemMessage);
        assertTrue(systemMessage.getText().startsWith("Current date and time (UTC): 2026-06-08T14:30:00Z"));
        assertTrue(systemMessage.getText().contains("You are a medical assistant."));
    }

    @Test
    @DisplayName("does not duplicate datetime when already present")
    void skipsDuplicateDateTimePrefix() {
        LlmDateTimeContext.setClock(Clock.fixed(Instant.parse("2026-06-08T14:30:00Z"), ZoneOffset.UTC));
        String existing = LlmDateTimeContext.contextBlock() + "\n\nExisting instructions.";
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(List.of(new SystemMessage(existing), new UserMessage("hi"))))
                .build();

        SystemMessage systemMessage = extractSystemMessageAfterAdvisor(request);

        assertNotNull(systemMessage);
        assertTrue(systemMessage.getText().startsWith(LlmDateTimeContext.contextBlock()));
        assertTrue(systemMessage.getText().endsWith("Existing instructions."));
        assertTrue(systemMessage.getText().indexOf(LlmDateTimeContext.contextBlock()) == systemMessage.getText().lastIndexOf(LlmDateTimeContext.contextBlock()));
    }

    private SystemMessage extractSystemMessageAfterAdvisor(ChatClientRequest request) {
        AtomicReference<ChatClientRequest> captured = new AtomicReference<>();
        CallAdvisorChain chain = new CallAdvisorChain() {
            @Override
            public ChatClientResponse nextCall(ChatClientRequest augmented) {
                captured.set(augmented);
                return new ChatClientResponse(null, Map.of());
            }

            @Override
            public List<CallAdvisor> getCallAdvisors() {
                return Collections.emptyList();
            }

            @Override
            public CallAdvisorChain copy(CallAdvisor advisor) {
                return this;
            }
        };
        advisor.adviseCall(request, chain);
        return captured.get().prompt().getSystemMessage();
    }
}
