package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolSelectionDatasetPathsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("loads large generated eval JSONL from custom path")
    void loadsGeneratedDatasetFile() throws Exception {
        Path dataset = tempDir.resolve("generated.jsonl");
        Files.writeString(dataset, """
                {"scenario":"analyze_with_case_id_en","goalType":"ANALYZE_CASE","caseIdInHints":true,"caseId":"6a23f05200155d711484cf69","userMessage":"detail case","expectedTool":"analyze_case","expectedArgs":{"caseId":"6a23f05200155d711484cf69"},"locale":"en"}
                {"scenario":"negative_text_only","goalType":"GENERAL_QUESTION","caseIdInHints":false,"userMessage":"What is GraphRAG?","expectedTool":null,"locale":"en"}
                """);

        ToolSelectionGoldenDataset loader = new ToolSelectionGoldenDataset();
        var cases = loader.loadFile(dataset);
        assertEquals(2, cases.size());
        assertEquals("analyze_case", cases.getFirst().expectedTool());
    }
}
