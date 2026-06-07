package com.berdachuk.medexpertmatch.retrieval.rest;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcome;
import com.berdachuk.medexpertmatch.retrieval.domain.dto.MatchOutcomeRecordRequest;
import com.berdachuk.medexpertmatch.retrieval.service.MatchOutcomeCalibrationService;
import com.berdachuk.medexpertmatch.retrieval.service.MatchOutcomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@Tag(name = "Match Outcomes", description = "Anonymized match outcome ingestion for historical calibration (M63)")
@RestController
@RequestMapping("/api/v1")
public class MatchOutcomeRestController {

    private final MatchOutcomeService matchOutcomeService;
    private final MatchOutcomeCalibrationService calibrationService;
    private final AdminAccessGuard adminAccessGuard;

    public MatchOutcomeRestController(
            MatchOutcomeService matchOutcomeService,
            MatchOutcomeCalibrationService calibrationService,
            AdminAccessGuard adminAccessGuard) {
        this.matchOutcomeService = matchOutcomeService;
        this.calibrationService = calibrationService;
        this.adminAccessGuard = adminAccessGuard;
    }

    @Operation(summary = "Record a match outcome label for a case-doctor pair")
    @PostMapping("/match-outcomes")
    public ResponseEntity<Map<String, Object>> recordOutcome(@RequestBody MatchOutcomeRecordRequest request) {
        log.info("POST /api/v1/match-outcomes label={}", request.label());
        MatchOutcome outcome = matchOutcomeService.recordOutcome(
                request.caseId(), request.doctorId(), request.label());
        return ResponseEntity.ok(Map.of(
                "id", outcome.id(),
                "caseId", outcome.caseId(),
                "doctorId", outcome.doctorId(),
                "label", outcome.label().name(),
                "recordedAt", outcome.recordedAt().toString()));
    }

    @Operation(summary = "Recalibrate doctor affinity scores from recorded outcomes (admin)")
    @PostMapping("/admin/match-outcomes/calibrate")
    public ResponseEntity<Map<String, Object>> calibrateOutcomes() {
        adminAccessGuard.requireAdmin();
        int calibrated = calibrationService.calibrateFromOutcomes();
        return ResponseEntity.ok(Map.of(
                "status", "calibrated",
                "doctorCount", calibrated));
    }
}
