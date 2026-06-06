package com.berdachuk.medexpertmatch.llm.eval;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Result of a live FunctionGemma tool-selection eval run.
 */
public record ToolSelectionLiveEvalReport(
        String label,
        String modelName,
        Instant evaluatedAt,
        int totalCases,
        int passedCases,
        double accuracy,
        List<CaseResult> caseResults) {

    public record CaseResult(
            String scenario,
            String locale,
            String userMessage,
            String expectedTool,
            String actualTool,
            boolean passed,
            Map<String, String> actualArgs) {
    }

    public Map<String, Double> accuracyByScenario() {
        return caseResults.stream()
                .collect(Collectors.groupingBy(
                        CaseResult::scenario,
                        Collectors.collectingAndThen(Collectors.toList(), results -> {
                            long passed = results.stream().filter(CaseResult::passed).count();
                            return results.isEmpty() ? 0.0 : (double) passed / results.size();
                        })));
    }
}
