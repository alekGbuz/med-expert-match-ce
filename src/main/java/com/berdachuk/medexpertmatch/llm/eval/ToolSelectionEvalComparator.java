package com.berdachuk.medexpertmatch.llm.eval;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Compares before/after FunctionGemma live eval reports (e.g. baseline vs fine-tuned).
 */
public final class ToolSelectionEvalComparator {

    private ToolSelectionEvalComparator() {
    }

    public record Comparison(
            ToolSelectionLiveEvalReport before,
            ToolSelectionLiveEvalReport after,
            double beforeAccuracy,
            double afterAccuracy,
            double accuracyDelta,
            int improvedCaseCount,
            int regressedCaseCount,
            List<String> improvedScenarios,
            List<String> regressedScenarios) {

        public String markdownSummary() {
            StringBuilder md = new StringBuilder();
            md.append("# FunctionGemma before/after comparison\n\n");
            md.append("| Metric | Before (").append(before.label()).append(") | After (")
                    .append(after.label()).append(") | Delta |\n");
            md.append("|--------|---------|--------|-------|\n");
            md.append(String.format(Locale.ROOT, "| Accuracy | %.1f%% | %.1f%% | %+.1f%% |\n",
                    beforeAccuracy * 100.0, afterAccuracy * 100.0, accuracyDelta * 100.0));
            md.append("| Model | ").append(before.modelName()).append(" | ")
                    .append(after.modelName()).append(" | — |\n\n");
            md.append("Improved cases: ").append(improvedCaseCount).append("\n\n");
            md.append("Regressed cases: ").append(regressedCaseCount).append("\n\n");
            if (!improvedScenarios.isEmpty()) {
                md.append("### Improved scenarios\n");
                improvedScenarios.forEach(s -> md.append("- ").append(s).append("\n"));
                md.append("\n");
            }
            if (!regressedScenarios.isEmpty()) {
                md.append("### Regressed scenarios\n");
                regressedScenarios.forEach(s -> md.append("- ").append(s).append("\n"));
            }
            return md.toString();
        }
    }

    public static Comparison compare(ToolSelectionLiveEvalReport before, ToolSelectionLiveEvalReport after) {
        Map<String, ToolSelectionLiveEvalReport.CaseResult> beforeByKey = index(before);
        Map<String, ToolSelectionLiveEvalReport.CaseResult> afterByKey = index(after);

        List<String> improved = new ArrayList<>();
        List<String> regressed = new ArrayList<>();
        for (String key : beforeByKey.keySet()) {
            ToolSelectionLiveEvalReport.CaseResult b = beforeByKey.get(key);
            ToolSelectionLiveEvalReport.CaseResult a = afterByKey.get(key);
            if (a == null) {
                continue;
            }
            if (!b.passed() && a.passed()) {
                improved.add(key);
            } else if (b.passed() && !a.passed()) {
                regressed.add(key);
            }
        }

        return new Comparison(
                before,
                after,
                before.accuracy(),
                after.accuracy(),
                after.accuracy() - before.accuracy(),
                improved.size(),
                regressed.size(),
                List.copyOf(improved),
                List.copyOf(regressed));
    }

    public static Comparison compareFiles(Path beforeJson, Path afterJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ToolSelectionLiveEvalReport before = mapper.readValue(Files.readString(beforeJson), ToolSelectionLiveEvalReport.class);
        ToolSelectionLiveEvalReport after = mapper.readValue(Files.readString(afterJson), ToolSelectionLiveEvalReport.class);
        return compare(before, after);
    }

    private static Map<String, ToolSelectionLiveEvalReport.CaseResult> index(ToolSelectionLiveEvalReport report) {
        return report.caseResults().stream()
                .collect(Collectors.toMap(
                        result -> result.scenario() + "|" + result.userMessage(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }
}
