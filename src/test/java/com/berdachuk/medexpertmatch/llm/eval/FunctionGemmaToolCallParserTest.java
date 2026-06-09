package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FunctionGemmaToolCallParserTest {

    private final FunctionGemmaToolCallParser parser = new FunctionGemmaToolCallParser();

    @Test
    @DisplayName("parses structured AssistantMessage tool calls")
    void parsesStructuredToolCalls() {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1", "function", "analyze_case", "{\"caseId\":\"6a23f05200155d711484cf69\"}")))
                .build();
        ChatResponse response = new ChatResponse(List.of(new Generation(message)));

        Optional<FunctionGemmaToolCallParser.ParsedToolCall> parsed = parser.parse(response);
        assertTrue(parsed.isPresent());
        assertEquals("analyze_case", parsed.get().toolName());
        assertEquals("6a23f05200155d711484cf69", parsed.get().args().get("caseId"));
    }

    @Test
    @DisplayName("parses FunctionGemma start_function_call text format")
    void parsesFunctionGemmaTextFormat() {
        String text = """
                <start_function_call>call:match_doctors_to_case{caseId:<escape>6a1db20e86d74aa336e98ff0<escape>}<end_function_call>
                """;
        Optional<FunctionGemmaToolCallParser.ParsedToolCall> parsed = parser.parseText(text);
        assertTrue(parsed.isPresent());
        assertEquals("match_doctors_to_case", parsed.get().toolName());
        assertEquals("6a1db20e86d74aa336e98ff0", parsed.get().args().get("caseId"));
    }

    @Test
    @DisplayName("returns empty when model replies with text only")
    void noToolForTextOnly() {
        Optional<FunctionGemmaToolCallParser.ParsedToolCall> parsed =
                parser.parseText("Please provide the full case description so I can analyze it.");
        assertFalse(parsed.isPresent());
    }

    @Test
    @DisplayName("normalizes tool args map values to strings")
    void normalizesArgs() {
        Map<String, String> args = parser.normalizeArgs(Map.of("caseId", "ABC123", "topK", 5));
        assertEquals("ABC123", args.get("caseId"));
        assertEquals("5", args.get("topK"));
    }
}
