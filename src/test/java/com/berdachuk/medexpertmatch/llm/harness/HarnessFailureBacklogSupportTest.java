package com.berdachuk.medexpertmatch.llm.harness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessFailureBacklogSupportTest {

    @Test
    @DisplayName("builds backlog markdown with failure reason and run id")
    void buildsBacklogMarkdown() {
        String markdown = HarnessFailureBacklogSupport.buildBacklogMarkdown(
                "TOOL_OUTPUT_INVALID",
                "run-abc",
                HarnessWorkflowType.DOCTOR_MATCH);
        assertTrue(markdown.contains("run-abc"));
        assertTrue(markdown.contains("Tool output invalid"));
        assertTrue(markdown.contains(".agents/templates/harness-backlog-item.md"));
    }
}
