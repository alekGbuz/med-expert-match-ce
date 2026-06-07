package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;

import java.util.List;

public record DoctorMatchCheckpointPayload(
        String caseId,
        String sessionId,
        int maxResults,
        List<DoctorMatch> matches,
        String caseAnalysisJson,
        int bundleSectionCount,
        String policyAction,
        String policyReason,
        String policyUserMessage) {

    public DoctorMatchCheckpointPayload(
            String caseId,
            String sessionId,
            int maxResults,
            List<DoctorMatch> matches,
            String caseAnalysisJson,
            int bundleSectionCount) {
        this(caseId, sessionId, maxResults, matches, caseAnalysisJson, bundleSectionCount, null, null, null);
    }
}
