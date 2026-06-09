package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import com.berdachuk.medexpertmatch.retrieval.service.MatchOutcomeCalibrationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Deterministic eval for outcome-calibrated historical scoring (M63).
 */
public final class MatchOutcomeCalibrationEvalRunner {

    private static final String DATASET = "/eval/match-outcome-calibration-cases.jsonl";

    private MatchOutcomeCalibrationEvalRunner() {
    }

    public static EvalFamilyResult run() {
        ObjectMapper objectMapper = new ObjectMapper();
        int passed = 0;
        int total = 0;
        try (InputStream stream = resourceStream(DATASET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                total++;
                JsonNode node = objectMapper.readTree(line);
                double experienceWeight = node.get("experienceWeight").asDouble();
                double outcomeWeight = node.get("outcomeWeight").asDouble();
                Map<String, Double> doctors = readDoctors(node.get("doctors"), experienceWeight, outcomeWeight);

                String top = doctors.entrySet().stream()
                        .max(Comparator.comparingDouble(Map.Entry::getValue))
                        .map(Map.Entry::getKey)
                        .orElseThrow();

                if (top.equals(node.get("expectedTop").asText())) {
                    passed++;
                }
            }
            return new EvalFamilyResult("match_outcome_calibration", "RETRIEVAL", passed, total, 0, true);
        } catch (Exception e) {
            throw new IllegalStateException("Match outcome calibration eval failed", e);
        }
    }

    static double blendedHistorical(
            double experienceScore,
            MatchOutcomeLabel label,
            double experienceWeight,
            double outcomeWeight) {
        double outcomeSignal = MatchOutcomeCalibrationService.labelScore(label);
        return experienceScore * experienceWeight + outcomeSignal * outcomeWeight;
    }

    private static Map<String, Double> readDoctors(
            JsonNode doctorsNode,
            double experienceWeight,
            double outcomeWeight) {
        Map<String, Double> doctors = new LinkedHashMap<>();
        for (Iterator<JsonNode> it = doctorsNode.elements(); it.hasNext(); ) {
            JsonNode doctor = it.next();
            double score = blendedHistorical(
                    doctor.get("experience").asDouble(),
                    MatchOutcomeLabel.valueOf(doctor.get("label").asText()),
                    experienceWeight,
                    outcomeWeight);
            doctors.put(doctor.get("id").asText(), score);
        }
        return doctors;
    }

    private static InputStream resourceStream(String path) {
        return Objects.requireNonNull(MatchOutcomeCalibrationEvalRunner.class.getResourceAsStream(path), path);
    }
}
