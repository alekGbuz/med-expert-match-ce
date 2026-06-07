package com.berdachuk.medexpertmatch.retrieval.repository;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorOutcomeAffinity;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcome;

import java.util.List;
import java.util.Optional;

/**
 * Repository for match outcome persistence (M63).
 */
public interface MatchOutcomeRepository {

    String insert(MatchOutcome outcome);

    Optional<MatchOutcome> findLatestForPair(String caseId, String doctorId);

    List<DoctorOutcomeAffinity> aggregateByDoctor();

    long count();

    int deleteAll();

    void upsertAffinity(DoctorOutcomeAffinity affinity);

    Optional<DoctorOutcomeAffinity> findAffinityByDoctorId(String doctorId);

    int deleteAllAffinities();
}
