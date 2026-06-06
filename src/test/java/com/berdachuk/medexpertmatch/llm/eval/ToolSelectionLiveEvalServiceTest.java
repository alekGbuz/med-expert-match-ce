package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolSelectionLiveEvalServiceTest {

    @Test
    @DisplayName("scores golden cases against model tool calls")
    void evaluatesGoldenCases() {
        ChatModel chatModel = mock(ChatModel.class);
        AssistantMessage toolMessage = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "id-1", "function", "analyze_case", "{\"caseId\":\"6a23f05200155d711484cf69\"}")))
                .build();
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(toolMessage))));

        ToolSelectionLiveEvalService service = new ToolSelectionLiveEvalService(
                chatModel,
                new ToolSelectionPromptBuilder(),
                new FunctionGemmaToolCallParser());

        ToolSelectionGoldenCase goldenCase = new ToolSelectionGoldenCase(
                "analyze_with_case_id_en",
                "en",
                com.berdachuk.medexpertmatch.llm.chat.GoalType.ANALYZE_CASE,
                true,
                "6a23f05200155d711484cf69",
                "detail the clinical case",
                "analyze_case",
                java.util.Map.of("caseId", "6a23f05200155d711484cf69"));

        ToolSelectionLiveEvalReport report = service.evaluate(
                List.of(goldenCase), "mock-model", "unit-test");

        assertEquals(1, report.totalCases());
        assertEquals(1, report.passedCases());
        assertEquals(1.0, report.accuracy(), 0.001);
        assertTrue(report.caseResults().getFirst().passed());
    }
}
