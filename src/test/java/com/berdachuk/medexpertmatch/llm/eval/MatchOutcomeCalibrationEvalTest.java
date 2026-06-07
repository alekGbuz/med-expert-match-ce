package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchOutcomeCalibrationEvalTest {

    @Test
    @DisplayName("Match outcome calibration JSONL regression set passes")
    void evalJsonlRegressionSet() {
        EvalFamilyResult result = MatchOutcomeCalibrationEvalRunner.run();
        assertEquals("match_outcome_calibration", result.family());
        assertTrue(result.total() > 0);
        assertEquals(result.total(), result.passed());
        assertTrue(result.allPassed());
    }
}
