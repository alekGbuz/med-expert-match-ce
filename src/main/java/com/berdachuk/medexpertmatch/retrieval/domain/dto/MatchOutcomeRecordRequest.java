package com.berdachuk.medexpertmatch.retrieval.domain.dto;

import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;

/**
 * Request body for recording a match outcome (synthetic/anonymized IDs in tests only).
 */
public record MatchOutcomeRecordRequest(
        String caseId,
        String doctorId,
        MatchOutcomeLabel label
) {
}
