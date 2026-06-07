package com.berdachuk.medexpertmatch.retrieval.service;

/**
 * Calibrates doctor historical affinity from recorded match outcomes (M63).
 */
public interface MatchOutcomeCalibrationService {

    /**
     * Recomputes doctor affinity scores from all match outcomes.
     *
     * @return number of doctors calibrated
     */
    int calibrateFromOutcomes();

    /**
     * Resolves outcome signal (0-1) for scoring, preferring pair-specific labels.
     */
    double resolveOutcomeSignal(String caseId, String doctorId);

    /**
     * Maps a label to a normalized score for eval and calibration.
     */
    static double labelScore(com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel label) {
        return switch (label) {
            case ACCEPTED -> 1.0;
            case REJECTED -> 0.0;
            case OVERRIDDEN -> 0.35;
        };
    }
}
