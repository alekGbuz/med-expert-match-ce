package com.berdachuk.medexpertmatch.llm.eval;

/**
 * Builds Auto-orchestrator user prompts for FunctionGemma live eval (mirrors chat hint layout).
 */
public class ToolSelectionPromptBuilder {

    private static final String CASE_ID_HINT = """
            IMPORTANT — medical case ID for case-ID tools (match_doctors_to_case, analyze_case, etc.): %s
            Use this exact 24-character ID only. Do NOT use ICD-10 codes, labels, or invented IDs.""";

    private static final String NO_CASE_ID_HINT = """
            No medical case ID in this message. Do NOT invent case IDs or pass ICD-10 codes to match_doctors_to_case.
            For specialist matching use match_doctors_from_text with the full anonymized case description.
            For analysis without matching use analyze_case_text with the case description.""";

    public String buildUserPrompt(ToolSelectionGoldenCase goldenCase) {
        StringBuilder prompt = new StringBuilder();
        if (goldenCase.caseIdInHints() && goldenCase.caseId() != null && !goldenCase.caseId().isBlank()) {
            prompt.append(String.format(CASE_ID_HINT, goldenCase.caseId())).append("\n\n");
        } else {
            prompt.append(NO_CASE_ID_HINT).append("\n\n");
        }
        if (goldenCase.goalType() != null) {
            prompt.append("Goal hint: ").append(goldenCase.goalType().name()).append("\n\n");
        }
        prompt.append("User message:\n").append(goldenCase.userMessage());
        return prompt.toString();
    }
}
