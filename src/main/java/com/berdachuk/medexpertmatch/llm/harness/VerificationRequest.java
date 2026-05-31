package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;

import java.util.List;

public record VerificationRequest(
        HarnessWorkflowType workflowType,
        String caseId,
        List<DoctorMatch> doctorMatches,
        int minMatches) {
}
