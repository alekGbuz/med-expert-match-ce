package com.berdachuk.medexpertmatch.core.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LlmResponseSanitizer {

    private static final String STRATEGIZING_MARKER = "strategizing complete";

    private static final Pattern CONTENT_SECTION_PATTERN = Pattern.compile(
            "(?i)(?:^|[\\n.]\\s*)(Case Summary|Clinical Presentation|Matched Doctors|"
                    + "Matching Rationale(?: Explanation)?|Evidence Summary|Recommendations)\\s*:?(?=\\s*(?:\\n|$))",
            Pattern.MULTILINE);

    private static final Pattern NUMBERED_SECTION_PATTERN = Pattern.compile(
            "(?i)\\d+\\.\\s*(Case Summary|Clinical Presentation|Matched Doctors|"
                    + "Matching Rationale(?: Explanation)?|Evidence Summary|Recommendations)\\b");

    private static final int MIN_REASONING_CHARS = 40;

    private LlmResponseSanitizer() {
    }

    public record ReasoningSplit(String reasoning, String content) {
    }

    public static String extractJson(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return llmOutput;
        }
        String result = llmOutput.trim();
        if (result.contains("```json")) {
            int start = result.indexOf("```json") + 7;
            int end = result.lastIndexOf("```");
            if (end > start) {
                result = result.substring(start, end).trim();
            }
        } else if (result.contains("```")) {
            int start = result.indexOf("```") + 3;
            int end = result.lastIndexOf("```");
            if (end > start) {
                result = result.substring(start, end).trim();
            }
        }
        int lastBrace = result.lastIndexOf('}');
        int lastBracket = result.lastIndexOf(']');
        int lastClose = Math.max(lastBrace, lastBracket);
        if (lastClose > 0 && lastClose < result.length() - 1) {
            String after = result.substring(lastClose + 1).trim();
            if (!after.isEmpty() && !after.startsWith(",") && !after.startsWith("}")) {
                result = result.substring(0, lastClose + 1);
            }
        }
        return result;
    }

    public static String stripLlmReasoning(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        ReasoningSplit split = splitReasoningFromResponse(response);
        String content = split.content() != null && !split.content().isBlank() ? split.content() : response;
        return stripCodeFences(stripLeadingReasoningHeaders(normalizeModelTokens(content)));
    }

    /**
     * Splits MedGemma chain-of-thought from the user-facing clinical content.
     */
    public static ReasoningSplit splitReasoningFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return new ReasoningSplit("", response);
        }
        if (response.contains("class=\"llm-thinking\"")) {
            return new ReasoningSplit("", response);
        }

        String cleaned = normalizeModelTokens(response);
        int contentStart = findBestContentStart(cleaned);
        if (contentStart > MIN_REASONING_CHARS) {
            String reasoning = cleaned.substring(0, contentStart).trim();
            String content = cleaned.substring(contentStart).trim();
            return new ReasoningSplit(trimReasoningLabel(reasoning), content);
        }

        if (hasPlanningMarkers(cleaned.toLowerCase(Locale.ROOT)) && contentStart > 0) {
            String reasoning = cleaned.substring(0, contentStart).trim();
            String content = cleaned.substring(contentStart).trim();
            return new ReasoningSplit(trimReasoningLabel(reasoning), content);
        }

        return new ReasoningSplit("", cleaned);
    }

    /**
     * Wraps detected model reasoning in a collapsible HTML block ahead of the clinical answer.
     */
    public static String formatForChatDisplay(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        if (response.contains("class=\"llm-thinking\"")) {
            return response;
        }

        ReasoningSplit split = splitReasoningFromResponse(response);
        String content = stripLlmReasoning(
                split.content() != null && !split.content().isBlank() ? split.content() : response);

        if (split.reasoning() == null || split.reasoning().length() < MIN_REASONING_CHARS) {
            return content;
        }

        return buildCollapsibleReasoning(split.reasoning())
                + "<div class=\"llm-answer-label\">Response</div>\n"
                + "<div class=\"llm-answer\">\n\n"
                + content
                + "\n\n</div>";
    }

    private static String buildCollapsibleReasoning(String reasoning) {
        return "<details class=\"llm-thinking\"><summary>Model reasoning (click to expand)</summary>"
                + "<div class=\"llm-thinking-body\">"
                + escapeHtml(reasoning.trim())
                + "</div></details>\n";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String normalizeModelTokens(String response) {
        return response.trim().replaceAll("<unused\\d+>", "").trim();
    }

    private static String trimReasoningLabel(String reasoning) {
        if (reasoning == null || reasoning.isBlank()) {
            return "";
        }
        String trimmed = reasoning.trim();
        while (trimmed.toLowerCase().startsWith("thought")) {
            int newlineIdx = trimmed.indexOf('\n');
            if (newlineIdx > 0) {
                trimmed = trimmed.substring(newlineIdx + 1).trim();
            } else {
                return "";
            }
        }
        return trimmed;
    }

    private static int findBestContentStart(String response) {
        int afterStrategizing = findContentStartAfterStrategizing(response);
        if (afterStrategizing >= 0) {
            return afterStrategizing;
        }

        String lower = response.toLowerCase(Locale.ROOT);
        if (hasPlanningMarkers(lower)) {
            int latest = findLatestValidSectionStart(response);
            if (latest >= 0) {
                return latest;
            }
        }

        int numbered = findFirstNumberedSectionStart(response);
        if (numbered >= 0) {
            return numbered;
        }

        int fallback = findFallbackContentStart(response);
        if (fallback >= 0) {
            return fallback;
        }

        return findFirstValidSectionStart(response);
    }

    private static int findFallbackContentStart(String response) {
        if (!hasPlanningMarkers(response.toLowerCase(Locale.ROOT))) {
            return -1;
        }
        String lower = response.toLowerCase(Locale.ROOT);
        String[] markers = {"case summary", "matching rationale explanation", "matching rationale", "clinical presentation"};
        int best = -1;
        for (String marker : markers) {
            int idx = lower.lastIndexOf(marker);
            if (idx > MIN_REASONING_CHARS && idx > best) {
                best = idx;
            }
        }
        return best;
    }

    private static int findContentStartAfterStrategizing(String response) {
        String lower = response.toLowerCase(Locale.ROOT);
        int markerIdx = lower.lastIndexOf(STRATEGIZING_MARKER);
        if (markerIdx < 0) {
            return -1;
        }
        int searchFrom = markerIdx + STRATEGIZING_MARKER.length();
        String tail = response.substring(searchFrom);

        int relNumbered = findFirstNumberedSectionStart(tail);
        if (relNumbered >= 0) {
            return searchFrom + relNumbered;
        }

        int relSection = findFirstValidSectionStart(tail);
        if (relSection >= 0) {
            return searchFrom + relSection;
        }
        return -1;
    }

    private static int findFirstNumberedSectionStart(String response) {
        Matcher matcher = NUMBERED_SECTION_PATTERN.matcher(response);
        while (matcher.find()) {
            if (isValidSectionMatch(response, matcher.start())) {
                return matcher.start();
            }
        }
        return -1;
    }

    private static int findFirstValidSectionStart(String response) {
        Matcher matcher = CONTENT_SECTION_PATTERN.matcher(response);
        while (matcher.find()) {
            int start = matcher.start(1);
            if (isValidSectionMatch(response, start)) {
                return start;
            }
        }
        return -1;
    }

    private static int findLatestValidSectionStart(String response) {
        int best = -1;
        Matcher numbered = NUMBERED_SECTION_PATTERN.matcher(response);
        while (numbered.find()) {
            if (isValidSectionMatch(response, numbered.start())) {
                best = numbered.start();
            }
        }
        Matcher section = CONTENT_SECTION_PATTERN.matcher(response);
        while (section.find()) {
            int start = section.start(1);
            if (isValidSectionMatch(response, start) && start > best) {
                best = start;
            }
        }
        return best;
    }

    private static boolean isValidSectionMatch(String response, int sectionStart) {
        int lineStart = sectionStart;
        while (lineStart > 0 && response.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        int lineEnd = response.indexOf('\n', sectionStart);
        if (lineEnd < 0) {
            lineEnd = response.length();
        }
        String line = response.substring(lineStart, lineEnd).trim();
        String lowerLine = line.toLowerCase(Locale.ROOT);
        if (line.contains("?") && (lowerLine.contains("yes") || lowerLine.contains("no"))) {
            return false;
        }
        if (lowerLine.contains("brief overview")
                || lowerLine.contains("only analysis")
                || lowerLine.contains("invent demographics")
                || lowerLine.contains("will monitor")) {
            return false;
        }
        return true;
    }

    private static boolean hasPlanningMarkers(String lower) {
        return looksLikeMedGemmaPlanningPrefix(lower)
                || lower.contains("the user wants")
                || lower.contains("explain matching rationale:")
                || lower.contains("presents matched doctors:")
                || lower.contains("provide recommendations:")
                || lower.contains("key learnings");
    }

    private static String stripLeadingReasoningHeaders(String cleaned) {
        String[] reasoningHeaders = {
                "Understand the Goal:", "Analyze the", "Step 1:", "Step 2:", "Step 3:",
                "Thought:", "Thinking:", "Reasoning:", "Analysis:", "Let me think",
                "Let's analyze", "First, I'll", "I need to", "The task is", "Key Information"
        };

        for (String header : reasoningHeaders) {
            if (cleaned.toLowerCase().startsWith(header.toLowerCase())) {
                int doubleNewlineIdx = cleaned.indexOf("\n\n");
                if (doubleNewlineIdx > 0 && doubleNewlineIdx < cleaned.length() - 2) {
                    cleaned = cleaned.substring(doubleNewlineIdx + 2).trim();
                    break;
                }
            }
        }

        while (cleaned.toLowerCase().startsWith("thought")) {
            int newlineIdx = cleaned.indexOf('\n');
            if (newlineIdx > 0) {
                cleaned = cleaned.substring(newlineIdx + 1).trim();
            } else {
                break;
            }
        }
        return cleaned;
    }

    private static String stripCodeFences(String cleaned) {
        if (cleaned.contains("```json")) {
            int jsonStart = cleaned.indexOf("```json");
            int jsonEnd = cleaned.lastIndexOf("```");
            if (jsonStart >= 0 && jsonEnd > jsonStart + 7) {
                return cleaned.substring(jsonStart + 7, jsonEnd).trim();
            }
        }
        if (cleaned.contains("```")) {
            int codeStart = cleaned.indexOf("```");
            int codeEnd = cleaned.lastIndexOf("```");
            if (codeStart >= 0 && codeEnd > codeStart + 3) {
                String content = cleaned.substring(codeStart + 3, codeEnd).trim();
                int firstNewline = content.indexOf('\n');
                if (firstNewline > 0 && firstNewline < 20) {
                    content = content.substring(firstNewline + 1).trim();
                }
                return content;
            }
        }
        return cleaned;
    }

    private static String stripMedGemmaPlanningPrefix(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        if (!hasPlanningMarkers(response.toLowerCase(Locale.ROOT))) {
            return response;
        }
        int contentStart = findBestContentStart(response);
        if (contentStart > 0) {
            return response.substring(contentStart).trim();
        }
        return response;
    }

    private static boolean looksLikeMedGemmaPlanningPrefix(String lower) {
        return lower.contains("mental sandbox")
                || lower.contains("constraint checklist")
                || lower.contains("confidence score")
                || lower.contains("strategizing complete")
                || lower.startsWith("thought")
                || lower.startsWith("recommendations: yes")
                || lower.contains("summarize the case:")
                || lower.contains("self-correction:")
                || lower.startsWith("the user wants");
    }

    public static String toHumanReadable(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        String cleaned = response.trim();

        cleaned = stripJsonPrefix(cleaned);
        cleaned = stripFinalResponsePrefix(cleaned);
        cleaned = cleanJsonOnlyContent(cleaned);

        return cleaned;
    }

    private static String stripJsonPrefix(String response) {
        String[] prefixes = {"final response:", "final answer:", "response:", "answer:",
                "here is the", "the result is:"};
        String lower = response.toLowerCase().trim();
        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                String after = response.substring(prefix.length()).trim();
                if (!after.isEmpty()) {
                    return after;
                }
            }
        }
        return response;
    }

    private static String stripFinalResponsePrefix(String response) {
        String cleaned = response.trim();
        String lower = cleaned.toLowerCase();

        String[] headers = {"final response:", "final answer:", "response:", "answer:",
                "final response", "final answer"};
        for (String header : headers) {
            if (lower.startsWith(header)) {
                String after = cleaned.substring(header.length()).trim();
                if (!after.isEmpty()) {
                    cleaned = after;
                    lower = cleaned.toLowerCase();
                }
            }
        }
        return cleaned;
    }

    private static String cleanJsonOnlyContent(String response) {
        String trimmed = response.trim();

        if (isJsonOnly(trimmed)) {
            return "[Data received; unable to display formatted response]";
        }

        if (trimmed.startsWith("[") && looksLikeJsonArray(trimmed)) {
            int closeIdx = findClosingBracket(trimmed);
            if (closeIdx > 0 && isJsonOnly(trimmed.substring(0, closeIdx + 1))) {
                String remainder = trimmed.substring(closeIdx + 1).trim();
                if (remainder.isEmpty()) {
                    return "[Data received; unable to display formatted response]";
                }
                return remainder;
            }
        }

        if (trimmed.startsWith("{") && looksLikeJsonObject(trimmed)) {
            int closeIdx = findClosingBrace(trimmed);
            if (closeIdx > 0 && isJsonOnly(trimmed.substring(0, closeIdx + 1))) {
                String remainder = trimmed.substring(closeIdx + 1).trim();
                if (remainder.isEmpty()) {
                    return "[Data received; unable to display formatted response]";
                }
                return remainder;
            }
        }

        return response;
    }

    private static boolean isJsonOnly(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("[")) {
            int closeIdx = findClosingBracket(trimmed);
            return closeIdx == trimmed.length() - 1;
        }
        if (trimmed.startsWith("{")) {
            int closeIdx = findClosingBrace(trimmed);
            return closeIdx == trimmed.length() - 1;
        }
        return false;
    }

    private static boolean looksLikeJsonArray(String text) {
        return text.startsWith("[") && (text.contains("\"") || text.contains("{"));
    }

    private static boolean looksLikeJsonObject(String text) {
        return text.startsWith("{") && text.contains("\"");
    }

    private static int findClosingBracket(String text) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '[') depth++;
                if (c == ']') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static int findClosingBrace(String text) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') depth++;
                if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }
}
