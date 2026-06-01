package com.berdachuk.medexpertmatch.llm.harness;

import java.time.Instant;

public record HarnessChainEvent(
        String id,
        String chainRootSessionId,
        String sessionId,
        String caseId,
        HarnessChainStep step,
        Instant createdAt) {
}
