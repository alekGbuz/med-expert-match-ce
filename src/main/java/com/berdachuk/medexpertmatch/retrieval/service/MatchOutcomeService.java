package com.berdachuk.medexpertmatch.retrieval.service;

import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcome;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;

/**
 * Records anonymized match outcomes for the data flywheel (M63).
 */
public interface MatchOutcomeService {

    MatchOutcome recordOutcome(String caseId, String doctorId, MatchOutcomeLabel label);
}
