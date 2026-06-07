package com.berdachuk.medexpertmatch.retrieval.domain;

import java.time.Instant;

/**
 * Calibrated historical affinity for a doctor derived from match outcomes (M63).
 */
public record DoctorOutcomeAffinity(
        String doctorId,
        double affinityScore,
        int sampleCount,
        Instant calibratedAt
) {
}
