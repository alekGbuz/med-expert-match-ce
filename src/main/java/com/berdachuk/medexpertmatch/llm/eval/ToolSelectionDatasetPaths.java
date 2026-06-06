package com.berdachuk.medexpertmatch.llm.eval;

import java.nio.file.Path;

/**
 * Resolves which tool-selection eval dataset to use (classpath golden vs generated file).
 */
public final class ToolSelectionDatasetPaths {

    public static final String DATASET_PROPERTY = "medexpertmatch.eval.tool-selection.dataset";
    public static final String DATASET_ENV = "MEDEXPERTMATCH_TOOL_SELECTION_DATASET";

    private ToolSelectionDatasetPaths() {
    }

    /**
     * Custom dataset file from system property or env; empty if default classpath golden should be used.
     */
    public static Path customDatasetPathOrNull() {
        String property = System.getProperty(DATASET_PROPERTY);
        if (property != null && !property.isBlank()) {
            return Path.of(property.trim());
        }
        String env = System.getenv(DATASET_ENV);
        if (env != null && !env.isBlank()) {
            return Path.of(env.trim());
        }
        return null;
    }
}
