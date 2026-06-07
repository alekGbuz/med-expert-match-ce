package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcome;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import com.berdachuk.medexpertmatch.retrieval.repository.MatchOutcomeRepository;
import com.berdachuk.medexpertmatch.retrieval.service.MatchOutcomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class MatchOutcomeServiceImpl implements MatchOutcomeService {

    private final MatchOutcomeRepository matchOutcomeRepository;
    private final MedicalCaseRepository medicalCaseRepository;
    private final DoctorRepository doctorRepository;

    public MatchOutcomeServiceImpl(
            MatchOutcomeRepository matchOutcomeRepository,
            MedicalCaseRepository medicalCaseRepository,
            DoctorRepository doctorRepository) {
        this.matchOutcomeRepository = matchOutcomeRepository;
        this.medicalCaseRepository = medicalCaseRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    @Transactional
    public MatchOutcome recordOutcome(String caseId, String doctorId, MatchOutcomeLabel label) {
        if (caseId == null || caseId.isBlank() || doctorId == null || doctorId.isBlank() || label == null) {
            throw new ResponseStatusException(BAD_REQUEST, "caseId, doctorId, and label are required");
        }
        String normalizedCaseId = caseId.trim().toLowerCase();
        String normalizedDoctorId = doctorId.trim();
        if (medicalCaseRepository.findById(normalizedCaseId).isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Medical case not found");
        }
        if (doctorRepository.findById(normalizedDoctorId).isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Doctor not found");
        }

        Instant recordedAt = Instant.now();
        MatchOutcome outcome = new MatchOutcome(
                IdGenerator.generateId(),
                normalizedCaseId,
                normalizedDoctorId,
                label,
                recordedAt);
        matchOutcomeRepository.insert(outcome);
        log.info("Recorded match outcome label={} for caseId={} doctorId={}", label, normalizedCaseId, normalizedDoctorId);
        return outcome;
    }
}
