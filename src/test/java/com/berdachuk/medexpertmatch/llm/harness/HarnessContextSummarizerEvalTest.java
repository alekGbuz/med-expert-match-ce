package com.berdachuk.medexpertmatch.llm.harness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class HarnessContextSummarizerEvalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HarnessContextSummarizer summarizer;

    @BeforeEach
    void setUp() {
        summarizer = new HarnessContextSummarizerImpl(objectMapper);
    }

    @Test
    @DisplayName("Context summarizer eval JSONL regression set preserves whitelist and drops noise")
    void evalJsonlRegressionSet() throws Exception {
        int total = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/eval/context-summarizer-cases.jsonl")),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                total++;
                JsonNode node = objectMapper.readTree(line);
                HarnessContextKind kind = HarnessContextKind.valueOf(node.get("kind").asText());
                String raw = node.get("raw").asText();
                String summary = summarizer.summarizeToolResults(raw, kind);

                for (Iterator<JsonNode> it = node.get("preserve").elements(); it.hasNext(); ) {
                    String token = it.next().asText();
                    assertTrue(summary.contains(token),
                            "Line " + total + " missing preserved token '" + token + "': " + summary);
                }
                if (node.has("drop")) {
                    for (Iterator<JsonNode> it = node.get("drop").elements(); it.hasNext(); ) {
                        String token = it.next().asText();
                        assertFalse(summary.contains(token),
                                "Line " + total + " must not contain '" + token + "': " + summary);
                    }
                }
            }
        }
        assertEquals(4, total, "Expected four context-summarizer eval scenarios");
    }
}
