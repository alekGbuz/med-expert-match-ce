package com.berdachuk.medexpertmatch.llm.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes live FunctionGemma eval reports to JSON and Markdown.
 */
public class ToolSelectionLiveEvalReportWriter {

    private final ObjectMapper objectMapper;

    public ToolSelectionLiveEvalReportWriter() {
        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path writeReport(ToolSelectionLiveEvalReport report, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(java.time.ZoneOffset.UTC)
                .format(report.evaluatedAt());
        String safeLabel = report.label().replaceAll("[^a-zA-Z0-9_-]", "-");
        Path jsonPath = outputDirectory.resolve("tool-selection-" + safeLabel + "-" + date + ".json");
        objectMapper.writeValue(jsonPath.toFile(), report);

        Path mdPath = outputDirectory.resolve("tool-selection-" + safeLabel + "-" + date + ".md");
        Files.writeString(mdPath, toMarkdown(report));
        return jsonPath;
    }

    public String toMarkdown(ToolSelectionLiveEvalReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# FunctionGemma live tool-selection eval — ").append(report.label()).append("\n\n");
        md.append("| Field | Value |\n|-------|-------|\n");
        md.append("| Model | ").append(report.modelName()).append(" |\n");
        md.append("| Evaluated at | ").append(report.evaluatedAt()).append(" |\n");
        md.append("| Cases | ").append(report.totalCases()).append(" |\n");
        md.append("| Passed | ").append(report.passedCases()).append(" |\n");
        md.append(String.format(Locale.ROOT, "| Accuracy | %.1f%% |\n\n", report.accuracy() * 100.0));

        md.append("## Accuracy by scenario\n\n");
        md.append("| Scenario | Accuracy |\n|----------|----------|\n");
        for (Map.Entry<String, Double> entry : report.accuracyByScenario().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList())) {
            md.append("| ").append(entry.getKey()).append(" | ")
                    .append(String.format(Locale.ROOT, "%.0f%%", entry.getValue() * 100.0))
                    .append(" |\n");
        }

        md.append("\n## Failures\n\n");
        boolean anyFailures = false;
        for (ToolSelectionLiveEvalReport.CaseResult result : report.caseResults()) {
            if (!result.passed()) {
                anyFailures = true;
                md.append("- **").append(result.scenario()).append("** (`").append(result.locale()).append("`): ")
                        .append("expected `").append(result.expectedTool()).append("`, got `")
                        .append(result.actualTool() == null ? "TEXT_ONLY" : result.actualTool()).append("` — ")
                        .append(result.userMessage()).append("\n");
            }
        }
        if (!anyFailures) {
            md.append("_None — all golden cases passed._\n");
        }
        return md.toString();
    }
}
