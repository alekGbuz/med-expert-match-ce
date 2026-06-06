package com.berdachuk.medexpertmatch.llm.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts tool name and arguments from FunctionGemma / OpenAI-compatible tool-calling responses.
 */
public class FunctionGemmaToolCallParser {

    private static final Pattern FUNCTION_GEMMA_CALL = Pattern.compile(
            "<start_function_call>call:(?<tool>[a-z0-9_]+)\\{(?<args>.*?)}<end_function_call>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern ESCAPED_ARG = Pattern.compile(
            "(?<key>[a-zA-Z_][a-zA-Z0-9_]*):<escape>(?<value>.*?)<escape>");

    private final ObjectMapper objectMapper;

    public FunctionGemmaToolCallParser() {
        this(new ObjectMapper());
    }

    public FunctionGemmaToolCallParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ParsedToolCall(String toolName, Map<String, String> args) {
    }

    public Optional<ParsedToolCall> parse(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return Optional.empty();
        }
        if (!(response.getResult().getOutput() instanceof AssistantMessage assistantMessage)) {
            return Optional.empty();
        }
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            AssistantMessage.ToolCall first = toolCalls.getFirst();
            return Optional.of(new ParsedToolCall(
                    first.name(),
                    parseArgumentsJson(first.arguments())));
        }
        return parseText(assistantMessage.getText());
    }

    public Optional<ParsedToolCall> parseText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = FUNCTION_GEMMA_CALL.matcher(text);
        if (matcher.find()) {
            return Optional.of(new ParsedToolCall(
                    matcher.group("tool"),
                    parseFunctionGemmaArgs(matcher.group("args"))));
        }
        return Optional.empty();
    }

    public Map<String, String> normalizeArgs(Map<String, ?> raw) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (raw == null) {
            return normalized;
        }
        raw.forEach((key, value) -> normalized.put(key, value == null ? "" : String.valueOf(value)));
        return normalized;
    }

    private Map<String, String> parseArgumentsJson(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(arguments, new TypeReference<>() {
            });
            return normalizeArgs(raw);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Map<String, String> parseFunctionGemmaArgs(String argsBlock) {
        Map<String, String> args = new LinkedHashMap<>();
        if (argsBlock == null || argsBlock.isBlank()) {
            return args;
        }
        Matcher matcher = ESCAPED_ARG.matcher(argsBlock);
        while (matcher.find()) {
            args.put(matcher.group("key"), matcher.group("value"));
        }
        if (!args.isEmpty()) {
            return args;
        }
        String[] pairs = argsBlock.split(",");
        for (String pair : pairs) {
            int colon = pair.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = pair.substring(0, colon).trim();
            String value = pair.substring(colon + 1).trim();
            if (value.startsWith("<escape>") && value.endsWith("<escape>")) {
                value = value.substring("<escape>".length(), value.length() - "<escape>".length());
            }
            args.put(key, value);
        }
        return args;
    }

    public static boolean argsMatch(Map<String, String> expected, Map<String, String> actual) {
        if (expected == null || expected.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String actualValue = actual == null ? null : actual.get(entry.getKey());
            if (actualValue == null) {
                return false;
            }
            if (!entry.getValue().equalsIgnoreCase(actualValue.trim())) {
                return false;
            }
        }
        return true;
    }
}
