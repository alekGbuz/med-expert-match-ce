package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanAdjudicationSupportTest {

    @Test
    @DisplayName("pauses only when adjudication enabled and policy is ESCALATE")
    void shouldPauseForEscalateWhenEnabled() {
        HarnessProperties enabled = new HarnessProperties(
                true, true, 2, true, 1, 0, false, true, false, false, false, true);
        HarnessProperties disabled = HarnessProperties.defaults();
        ConfidencePolicyDecision escalate = new ConfidencePolicyDecision(
                PolicyAction.ESCALATE, "urgent_low_score", "review");

        assertTrue(HumanAdjudicationSupport.shouldPauseForAdjudication(enabled, escalate));
        assertFalse(HumanAdjudicationSupport.shouldPauseForAdjudication(disabled, escalate));
        assertFalse(HumanAdjudicationSupport.shouldPauseForAdjudication(
                enabled,
                new ConfidencePolicyDecision(PolicyAction.CLARIFY, "low_score", "clarify")));
    }
}
