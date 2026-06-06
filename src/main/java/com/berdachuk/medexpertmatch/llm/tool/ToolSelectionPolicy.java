package com.berdachuk.medexpertmatch.llm.tool;

import com.berdachuk.medexpertmatch.llm.chat.GoalIntentPatterns;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic policy for which Auto-orchestrator tool FunctionGemma should select.
 * Used for M58 baseline eval and server-side tool-name guarding.
 */
public final class ToolSelectionPolicy {

    private ToolSelectionPolicy() {
    }

    public record ToolChoice(String toolName, Map<String, String> args) {
        public ToolChoice {
            Objects.requireNonNull(toolName, "toolName");
            args = args == null || args.isEmpty() ? Map.of() : Map.copyOf(args);
        }
    }

    public static Optional<ToolChoice> resolve(
            GoalType goalType,
            boolean caseIdInHints,
            String caseId,
            String userMessage) {
        if (goalType == null || goalType == GoalType.GENERAL_QUESTION || goalType == GoalType.GENERATE_RECOMMENDATIONS) {
            return Optional.empty();
        }

        String normalizedCaseId = normalizeCaseId(caseId);
        boolean hasCaseId = caseIdInHints && normalizedCaseId != null;
        String message = userMessage != null ? userMessage : "";

        if (hasCaseId) {
            return Optional.of(resolveWithCaseId(goalType, normalizedCaseId, message));
        }
        return resolveWithoutCaseId(goalType, message);
    }

    /**
     * Remaps a model-selected tool to the case-ID variant when session context has a case ID.
     */
    public static String correctToolName(String requestedTool, GoalType goalType, String sessionCaseId) {
        if (requestedTool == null || requestedTool.isBlank() || goalType == null) {
            return requestedTool;
        }
        String caseId = normalizeCaseId(sessionCaseId);
        if (caseId == null) {
            return requestedTool;
        }
        if ("analyze_case_text".equals(requestedTool) && goalType == GoalType.ANALYZE_CASE) {
            return "analyze_case";
        }
        if ("match_doctors_from_text".equals(requestedTool) && goalType == GoalType.MATCH_DOCTORS) {
            return "match_doctors_to_case";
        }
        return requestedTool;
    }

    public static boolean matchesExpected(ToolChoice expected, ToolChoice actual) {
        if (expected == null || actual == null) {
            return false;
        }
        if (!expected.toolName().equals(actual.toolName())) {
            return false;
        }
        for (Map.Entry<String, String> entry : expected.args().entrySet()) {
            if (!entry.getValue().equals(actual.args().get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static ToolChoice resolveWithCaseId(GoalType goalType, String caseId, String message) {
        Map<String, String> caseArgs = Map.of("caseId", caseId);
        return switch (goalType) {
            case MATCH_DOCTORS -> new ToolChoice("match_doctors_to_case", caseArgs);
            case ANALYZE_CASE -> new ToolChoice("analyze_case", caseArgs);
            case ROUTE_CASE -> new ToolChoice("match_facilities_for_case", caseArgs);
            case SEARCH_EVIDENCE -> evidenceTool(message);
            case TRIAGE_INTAKE -> new ToolChoice("analyze_case", caseArgs);
            default -> throw new IllegalStateException("Unexpected goal: " + goalType);
        };
    }

    private static Optional<ToolChoice> resolveWithoutCaseId(GoalType goalType, String message) {
        return switch (goalType) {
            case MATCH_DOCTORS -> Optional.of(new ToolChoice("match_doctors_from_text", Map.of()));
            case ANALYZE_CASE -> Optional.of(new ToolChoice("analyze_case_text", Map.of()));
            case ROUTE_CASE -> Optional.empty();
            case SEARCH_EVIDENCE -> Optional.of(evidenceTool(message));
            case TRIAGE_INTAKE -> Optional.empty();
            default -> Optional.empty();
        };
    }

    private static ToolChoice evidenceTool(String message) {
        if (GoalIntentPatterns.looksLikePubmedIntent(message)) {
            return new ToolChoice("query_pubmed", Map.of());
        }
        return new ToolChoice("search_clinical_guidelines", Map.of());
    }

    private static String normalizeCaseId(String caseId) {
        if (caseId == null || caseId.isBlank()) {
            return null;
        }
        return caseId.trim().toLowerCase();
    }

}
