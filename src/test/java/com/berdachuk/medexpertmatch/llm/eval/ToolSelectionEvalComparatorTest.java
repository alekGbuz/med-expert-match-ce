package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSelectionEvalComparatorTest {

    @Test
    @DisplayName("computes accuracy delta and per-scenario changes between reports")
    void comparesBeforeAndAfter() {
        ToolSelectionLiveEvalReport before = report("baseline", 0.80, List.of(
                caseResult("analyze_with_case_id_en", true),
                caseResult("match_with_case_id_en", false)));
        ToolSelectionLiveEvalReport after = report("finetuned", 1.0, List.of(
                caseResult("analyze_with_case_id_en", true),
                caseResult("match_with_case_id_en", true)));

        ToolSelectionEvalComparator.Comparison comparison =
                ToolSelectionEvalComparator.compare(before, after);

        assertEquals(0.80, comparison.beforeAccuracy(), 0.001);
        assertEquals(1.0, comparison.afterAccuracy(), 0.001);
        assertEquals(0.20, comparison.accuracyDelta(), 0.001);
        assertEquals(1, comparison.improvedCaseCount());
        assertEquals(0, comparison.regressedCaseCount());
        assertTrue(comparison.markdownSummary().contains("finetuned"));
    }

    private static ToolSelectionLiveEvalReport report(
            String label, double accuracy, List<ToolSelectionLiveEvalReport.CaseResult> results) {
        return new ToolSelectionLiveEvalReport(
                label,
                "functiongemma:270m",
                Instant.parse("2026-06-07T10:00:00Z"),
                results.size(),
                (int) Math.round(accuracy * results.size()),
                accuracy,
                results);
    }

    private static ToolSelectionLiveEvalReport.CaseResult caseResult(String scenario, boolean passed) {
        return new ToolSelectionLiveEvalReport.CaseResult(
                scenario,
                "en",
                "detail case",
                "analyze_case",
                passed ? "analyze_case" : "analyze_case_text",
                passed,
                null);
    }
}
