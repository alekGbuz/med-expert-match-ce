package com.berdachuk.medexpertmatch.llm.harness;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

public final class DeniedToolCallback implements ToolCallback {

    private final String toolName;

    public DeniedToolCallback(String toolName) {
        this.toolName = toolName;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name(toolName)
                .description("Tool denied by harness scope policy")
                .build();
    }

    @Override
    public String call(String toolInput) {
        return "{\"error\":\"TOOL_SCOPE_VIOLATION\",\"tool\":\"" + toolName + "\"}";
    }
}
