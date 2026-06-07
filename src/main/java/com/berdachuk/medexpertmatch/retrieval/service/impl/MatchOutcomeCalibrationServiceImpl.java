package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorOutcomeAffinity;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import com.berdachuk.medexpertmatch.retrieval.repository.MatchOutcomeRepository;
import com.berdachuk.medexpertmatch.retrieval.service.MatchOutcomeCalibrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MatchOutcomeCalibrationServiceImpl implements MatchOutcomeCalibrationService {

    private static final double NEUTRAL_SIGNAL = 0.5;

    private final MatchOutcomeRepository matchOutcomeRepository;

    public MatchOutcomeCalibrationServiceImpl(MatchOutcomeRepository matchOutcomeRepository) {
        this.matchOutcomeRepository = matchOutcomeRepository;
    }

    @Override
    @Transactional
    public int calibrateFromOutcomes() {
        matchOutcomeRepository.deleteAllAffinities();
        var aggregates = matchOutcomeRepository.aggregateByDoctor();
        for (DoctorOutcomeAffinity aggregate : aggregates) {
            double clamped = Math.max(0.0, Math.min(1.0, aggregate.affinityScore()));
            matchOutcomeRepository.upsertAffinity(new DoctorOutcomeAffinity(
                    aggregate.doctorId(),
                    clamped,
                    aggregate.sampleCount(),
                    aggregate.calibratedAt()));
        }
        log.info("Calibrated outcome affinity for {} doctor(s)", aggregates.size());
        return aggregates.size();
    }

    @Override
    @Transactional(readOnly = true)
    public double resolveOutcomeSignal(String caseId, String doctorId) {
        if (doctorId == null || doctorId.isBlank()) {
            return NEUTRAL_SIGNAL;
        }
        if (caseId != null && !caseId.isBlank()) {
            var pairOutcome = matchOutcomeRepository.findLatestForPair(caseId, doctorId);
            if (pairOutcome.isPresent()) {
                return labelScore(pairOutcome.get().label());
            }
        }
        return matchOutcomeRepository.findAffinityByDoctorId(doctorId)
                .map(DoctorOutcomeAffinity::affinityScore)
                .orElse(NEUTRAL_SIGNAL);
    }

    static double labelScore(MatchOutcomeLabel label) {
        return MatchOutcomeCalibrationService.labelScore(label);
    }
}
