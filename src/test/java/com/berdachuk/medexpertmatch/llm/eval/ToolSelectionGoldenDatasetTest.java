package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSelectionGoldenDatasetTest {

    @Test
    @DisplayName("loads curated golden dataset from classpath")
    void loadsGoldenDataset() throws Exception {
        ToolSelectionGoldenDataset dataset = new ToolSelectionGoldenDataset();
        var cases = dataset.loadClasspathGolden();
        ToolSelectionGoldenDataset.requireMinimumSize(cases, 20);
        assertTrue(cases.stream().anyMatch(c -> "analyze_with_case_id_ru".equals(c.scenario())));
        assertTrue(cases.stream().anyMatch(c -> !c.expectsTool()));
        long withCaseId = cases.stream().filter(ToolSelectionGoldenCase::caseIdInHints).count();
        assertTrue(withCaseId >= 10);
        assertEquals(24, cases.size());
    }
}
