package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;
import com.berdachuk.medexpertmatch.llm.chat.ChatToolContextHolder;
import com.berdachuk.medexpertmatch.llm.tool.AgentToolNameNormalizer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

public class ToolScopeEnforcingResolver implements ToolCallbackResolver {

    private final ToolCallbackResolver delegate;

    public ToolScopeEnforcingResolver(ToolCallbackResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolCallback resolve(String toolName) {
        ChatAgentProfile profile = ChatToolContextHolder.profileOrNull();
        if (profile != null && toolName != null && !toolName.isBlank()) {
            String snake = AgentToolNameNormalizer.toSnakeCase(toolName);
            if (!ChatAgentToolScope.isAllowed(profile, snake) && !ChatAgentToolScope.isAllowed(profile, toolName)) {
                return new DeniedToolCallback(snake);
            }
        }
        return delegate.resolve(toolName);
    }
}
