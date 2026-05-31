package com.berdachuk.medexpertmatch.llm.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalHarnessBaselineTest {

    @Test
    @DisplayName("baseline pass rate file is present and parseable")
    void baselineFilePresent() throws IOException {
        ClassPathResource resource = new ClassPathResource("evaluation/baseline-pass-rate.txt");
        String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        double baseline = Double.parseDouble(raw);
        assertTrue(baseline >= 0.0 && baseline <= 1.0);
    }
}
