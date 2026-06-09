package com.berdachuk.medexpertmatch.llm.harness;

import java.util.List;

public record RoutingCheckpointPayload(
        String caseId,
        String sessionId,
        int maxResults,
        List<com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch> matches,
        String caseAnalysisJson,
        int bundleSectionCount) {
}
