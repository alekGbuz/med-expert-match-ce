package com.berdachuk.medexpertmatch.llm.eval;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI entry point for comparing two live FunctionGemma eval JSON reports.
 */
public final class ToolSelectionEvalCompareMain {

    private ToolSelectionEvalCompareMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: ToolSelectionEvalCompareMain <before.json> <after.json> [output.md]");
            System.exit(1);
        }
        Path before = Path.of(args[0]);
        Path after = Path.of(args[1]);
        Path output = args.length >= 3 ? Path.of(args[2]) : Path.of("target/eval/tool-selection-comparison.md");

        ToolSelectionEvalComparator.Comparison comparison = ToolSelectionEvalComparator.compareFiles(before, after);
        Files.createDirectories(output.getParent());
        Files.writeString(output, comparison.markdownSummary());
        System.out.println("Wrote comparison to " + output.toAbsolutePath());
    }
}
