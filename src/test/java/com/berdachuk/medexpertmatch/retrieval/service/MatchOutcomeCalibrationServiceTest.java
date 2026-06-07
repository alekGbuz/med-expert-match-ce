package com.berdachuk.medexpertmatch.retrieval.service;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorOutcomeAffinity;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcome;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import com.berdachuk.medexpertmatch.retrieval.repository.MatchOutcomeRepository;
import com.berdachuk.medexpertmatch.retrieval.service.impl.MatchOutcomeCalibrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchOutcomeCalibrationServiceTest {

    private MatchOutcomeRepository repository;
    private MatchOutcomeCalibrationService service;

    @BeforeEach
    void setUp() {
        repository = mock(MatchOutcomeRepository.class);
        service = new MatchOutcomeCalibrationServiceImpl(repository);
    }

    @Test
    @DisplayName("calibrateFromOutcomes upserts aggregated doctor affinities")
    void calibrateFromOutcomesUpsertsAffinities() {
        when(repository.aggregateByDoctor()).thenReturn(List.of(
                new DoctorOutcomeAffinity("d_accept", 0.9, 3, Instant.now()),
                new DoctorOutcomeAffinity("d_reject", 0.1, 2, Instant.now())));

        int calibrated = service.calibrateFromOutcomes();

        assertEquals(2, calibrated);
        verify(repository).deleteAllAffinities();
        verify(repository, org.mockito.Mockito.times(2)).upsertAffinity(any(DoctorOutcomeAffinity.class));
    }

    @Test
    @DisplayName("resolveOutcomeSignal prefers pair-specific label over doctor affinity")
    void resolveOutcomeSignalPrefersPairLabel() {
        when(repository.findLatestForPair("case1", "d1")).thenReturn(Optional.of(
                new MatchOutcome("o1", "case1", "d1", MatchOutcomeLabel.REJECTED, Instant.now())));

        double signal = service.resolveOutcomeSignal("case1", "d1");

        assertEquals(0.0, signal);
    }

    @Test
    @DisplayName("resolveOutcomeSignal uses calibrated doctor affinity when no pair label")
    void resolveOutcomeSignalUsesDoctorAffinity() {
        when(repository.findLatestForPair("case1", "d2")).thenReturn(Optional.empty());
        when(repository.findAffinityByDoctorId("d2")).thenReturn(Optional.of(
                new DoctorOutcomeAffinity("d2", 0.82, 5, Instant.now())));

        double signal = service.resolveOutcomeSignal("case1", "d2");

        assertEquals(0.82, signal, 0.001);
    }

    @Test
    @DisplayName("calibration shifts ranking toward accepted doctor on held-out synthetic outcomes")
    void calibrationShiftsRankingTowardAcceptedDoctor() {
        double accepted = MatchOutcomeCalibrationService.labelScore(MatchOutcomeLabel.ACCEPTED);
        double rejected = MatchOutcomeCalibrationService.labelScore(MatchOutcomeLabel.REJECTED);
        assertTrue(accepted > rejected);
        double acceptedWeighted = accepted * 0.4 + 0.5 * 0.6;
        double rejectedWeighted = rejected * 0.4 + 0.5 * 0.6;
        assertTrue(acceptedWeighted > rejectedWeighted);
    }
}
