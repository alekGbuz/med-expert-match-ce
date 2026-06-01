package com.berdachuk.medexpertmatch.llm.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvaluationReportParserTest {

    @Test
    @DisplayName("extracts normalized accuracy from eval report JSON")
    void extractsNormalizedAccuracy() {
        EvaluationReportParser parser = new EvaluationReportParser(new com.fasterxml.jackson.databind.ObjectMapper());
        String report = """
                {"normalized_accuracy": 0.875, "total": 24}
                """;
        assertEquals(0.875, parser.extractNormalizedAccuracy(report), 0.001);
    }
}
