package com.berdachuk.medexpertmatch.llm.harness;

import java.time.Instant;

/**
 * Immutable audit record for harness human adjudication (M65).
 */
public record HarnessAdjudicationEntry(
        String id,
        String runId,
        String caseId,
        String reviewerId,
        String decision,
        String comment,
        Instant recordedAt) {
}
