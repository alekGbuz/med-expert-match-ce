package com.berdachuk.medexpertmatch.llm.harness;

import java.time.Instant;

public record RecommendationChainedEvent(String caseId, String sessionId, Instant chainedAt) {
}
