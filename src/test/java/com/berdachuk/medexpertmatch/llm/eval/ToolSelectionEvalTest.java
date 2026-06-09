package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.tool.ToolSelectionPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ToolSelectionEvalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Tool selection policy eval JSONL regression set passes")
    void evalJsonlRegressionSet() throws Exception {
        int total = 0;
        int passed = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/eval/tool-selection-cases.jsonl")),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                total++;
                JsonNode node = objectMapper.readTree(line);
                GoalType goalType = GoalType.valueOf(node.get("goalType").asText());
                boolean caseIdInHints = node.path("caseIdInHints").asBoolean(false);
                String caseId = node.hasNonNull("caseId") ? node.get("caseId").asText() : null;
                String userMessage = node.get("userMessage").asText();

                Optional<ToolSelectionPolicy.ToolChoice> actual =
                        ToolSelectionPolicy.resolve(goalType, caseIdInHints, caseId, userMessage);

                JsonNode expectedToolNode = node.get("expectedTool");
                if (expectedToolNode == null || expectedToolNode.isNull()) {
                    if (actual.isPresent()) {
                        fail("Line " + total + " scenario=" + node.path("scenario").asText()
                                + " expected no tool but got " + actual.get().toolName());
                    }
                } else {
                    String expectedTool = expectedToolNode.asText();
                    int lineNumber = total;
                    ToolSelectionPolicy.ToolChoice actualChoice = actual.orElseThrow(() ->
                            new AssertionError("Line " + lineNumber + " expected tool " + expectedTool + " but got none"));
                    if (!expectedTool.equals(actualChoice.toolName())) {
                        fail("Line " + total + " scenario=" + node.path("scenario").asText()
                                + " expected tool " + expectedTool + " but got " + actualChoice.toolName());
                    }
                    Map<String, String> expectedArgs = readStringMap(node.get("expectedArgs"));
                    for (Map.Entry<String, String> entry : expectedArgs.entrySet()) {
                        assertEquals(entry.getValue(), actualChoice.args().get(entry.getKey()),
                                "Line " + total + " arg mismatch for " + entry.getKey());
                    }
                }
                passed++;
            }
        }
        assertTrue(total >= 50, "Expected at least 50 tool-selection eval cases");
        assertEquals(total, passed);
    }

    private static Map<String, String> readStringMap(JsonNode node) {
        Map<String, String> map = new HashMap<>();
        if (node == null || node.isNull()) {
            return map;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            map.put(field.getKey(), field.getValue().asText());
        }
        return map;
    }
}
