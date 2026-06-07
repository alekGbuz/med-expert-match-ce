package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;

/**
 * Policy-driven human-in-the-loop checkpoint rules (M65). {@link DoctorMatchWorkflowState#NEEDS_HUMAN}
 * is the persisted HUMAN_REVIEW state.
 */
public final class HumanAdjudicationSupport {

    private HumanAdjudicationSupport() {}

    public static boolean shouldPauseForAdjudication(
            HarnessProperties properties,
            ConfidencePolicyDecision decision) {
        return properties.humanAdjudicationEnabled()
                && decision != null
                && decision.action() == PolicyAction.ESCALATE;
    }

    public static String pendingReviewMessage(ConfidencePolicyDecision decision) {
        if (decision != null && decision.userMessage() != null && !decision.userMessage().isBlank()) {
            return decision.userMessage();
        }
        return """
                Specialist recommendations are pending clinician review before results can be released. \
                This output is for research and educational purposes only and is not a substitute \
                for professional medical advice, diagnosis, or treatment.""";
    }
}
