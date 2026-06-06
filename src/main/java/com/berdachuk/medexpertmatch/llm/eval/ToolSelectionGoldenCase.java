package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;

import java.util.Map;
import java.util.Objects;

/**
 * One row from the FunctionGemma tool-selection golden dataset.
 */
public record ToolSelectionGoldenCase(
        String scenario,
        String locale,
        GoalType goalType,
        boolean caseIdInHints,
        String caseId,
        String userMessage,
        String expectedTool,
        Map<String, String> expectedArgs) {

    public ToolSelectionGoldenCase {
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(goalType, "goalType");
        Objects.requireNonNull(userMessage, "userMessage");
        expectedArgs = expectedArgs == null ? Map.of() : Map.copyOf(expectedArgs);
    }

    public boolean expectsTool() {
        return expectedTool != null && !expectedTool.isBlank();
    }
}
