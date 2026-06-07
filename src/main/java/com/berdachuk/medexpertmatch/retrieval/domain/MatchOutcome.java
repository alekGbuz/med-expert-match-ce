package com.berdachuk.medexpertmatch.retrieval.domain;

import java.time.Instant;

/**
 * Recorded match outcome for doctor-case pairs (anonymized IDs only).
 */
public record MatchOutcome(
        String id,
        String caseId,
        String doctorId,
        MatchOutcomeLabel label,
        Instant recordedAt
) {
}
