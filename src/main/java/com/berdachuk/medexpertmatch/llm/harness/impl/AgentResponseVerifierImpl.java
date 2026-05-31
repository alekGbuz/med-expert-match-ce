package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.AgentResponseVerifier;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchVerificationRules;
import com.berdachuk.medexpertmatch.llm.harness.HarnessFailureReason;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowType;
import com.berdachuk.medexpertmatch.llm.harness.VerificationRequest;
import com.berdachuk.medexpertmatch.llm.harness.VerificationResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentResponseVerifierImpl implements AgentResponseVerifier {

    @Override
    public VerificationResult verify(VerificationRequest request) {
        if (request.workflowType() != HarnessWorkflowType.DOCTOR_MATCH) {
            return VerificationResult.pass();
        }
        int minMatches = request.minMatches() > 0
                ? request.minMatches()
                : DoctorMatchVerificationRules.DEFAULT_MIN_MATCHES;
        List<String> violations = DoctorMatchVerificationRules.validateMatches(
                request.doctorMatches(), minMatches);
        if (violations.isEmpty()) {
            return VerificationResult.pass();
        }
        return VerificationResult.fail(violations, HarnessFailureReason.TOOL_OUTPUT_INVALID);
    }
}
