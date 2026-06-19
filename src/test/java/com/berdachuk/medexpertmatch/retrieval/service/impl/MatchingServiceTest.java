package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOptions;
import com.berdachuk.medexpertmatch.retrieval.domain.RoutingOptions;
import com.berdachuk.medexpertmatch.retrieval.repository.ConsultationMatchRepository;
import com.berdachuk.medexpertmatch.retrieval.service.RerankingService;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private MedicalCaseRepository medicalCaseRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private SemanticGraphRetrievalService semanticGraphRetrievalService;
    @Mock
    private ConsultationMatchRepository consultationMatchRepository;
    @Mock
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    @Mock
    private RerankingService rerankingService;

    @InjectMocks
    private MatchingServiceImpl matchingService;

    @Test
    @DisplayName("throws IllegalArgumentException when caseId is null")
    void nullCaseIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> matchingService.matchDoctorsToCase(null, MatchOptions.defaultOptions()));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when caseId is blank")
    void blankCaseIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> matchingService.matchDoctorsToCase("  ", MatchOptions.defaultOptions()));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when case not found")
    void caseNotFoundThrows() {
        when(medicalCaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> matchingService.matchDoctorsToCase("nonexistent", MatchOptions.defaultOptions()));
    }

    @Test
    @DisplayName("throws IllegalStateException when case has insufficient medical data")
    void insufficientDataThrows() {
        MedicalCase emptyCase = new MedicalCase(
                "case-empty", null, null, null, null,
                List.of(), List.of(), null, null, null, null, null, null, null);
        when(medicalCaseRepository.findById("case-empty")).thenReturn(Optional.of(emptyCase));

        assertThrows(IllegalStateException.class,
                () -> matchingService.matchDoctorsToCase("case-empty", MatchOptions.defaultOptions()));
    }

    @Test
    @DisplayName("matchFacilitiesForCase throws when caseId is null")
    void matchFacilitiesNullCaseIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> matchingService.matchFacilitiesForCase(null, RoutingOptions.defaultOptions()));
    }

    @Test
    @DisplayName("matchFacilitiesForCase throws when case not found")
    void matchFacilitiesCaseNotFoundThrows() {
        when(medicalCaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> matchingService.matchFacilitiesForCase("nonexistent", RoutingOptions.defaultOptions()));
    }
}
