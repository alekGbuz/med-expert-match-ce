package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads tool-selection golden cases from JSONL.
 */
public final class ToolSelectionGoldenDataset {

    public static final String GOLDEN_CLASSPATH = "/eval/tool-selection-golden.jsonl";

    private final ObjectMapper objectMapper;

    public ToolSelectionGoldenDataset() {
        this(new ObjectMapper());
    }

    public ToolSelectionGoldenDataset(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ToolSelectionGoldenCase> loadClasspathGolden() throws IOException {
        InputStream stream = ToolSelectionGoldenDataset.class.getResourceAsStream(GOLDEN_CLASSPATH);
        if (stream == null) {
            throw new IOException("Missing classpath resource: " + GOLDEN_CLASSPATH);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return load(reader);
        }
    }

    public List<ToolSelectionGoldenCase> loadFile(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    public List<ToolSelectionGoldenCase> load(BufferedReader reader) throws IOException {
        List<ToolSelectionGoldenCase> cases = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            cases.add(parseLine(line));
        }
        return List.copyOf(cases);
    }

    private ToolSelectionGoldenCase parseLine(String line) throws IOException {
        JsonNode node = objectMapper.readTree(line);
        String expectedTool = node.hasNonNull("expectedTool") ? node.get("expectedTool").asText() : null;
        return new ToolSelectionGoldenCase(
                node.path("scenario").asText("unknown"),
                node.path("locale").asText("en"),
                GoalType.valueOf(node.get("goalType").asText()),
                node.path("caseIdInHints").asBoolean(false),
                node.hasNonNull("caseId") ? node.get("caseId").asText() : null,
                node.get("userMessage").asText(),
                expectedTool,
                readStringMap(node.get("expectedArgs")));
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

    public static void requireMinimumSize(List<ToolSelectionGoldenCase> cases, int minimum) {
        Objects.requireNonNull(cases, "cases");
        if (cases.size() < minimum) {
            throw new IllegalStateException("Expected at least " + minimum + " golden cases, got " + cases.size());
        }
    }
}
