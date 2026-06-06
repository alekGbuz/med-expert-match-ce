package com.berdachuk.medexpertmatch.llm.tool;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ChatToolContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ConversationGoalContext;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

/**
 * Remaps wrong text-variant tool names to case-ID tools when session context already has a case ID.
 */
@Slf4j
public class ToolSelectionGuardingResolver implements ToolCallbackResolver {

    private final ToolCallbackResolver delegate;

    public ToolSelectionGuardingResolver(ToolCallbackResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolCallback resolve(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return delegate.resolve(toolName);
        }
        String guarded = ToolSelectionPolicy.correctToolName(toolName, currentGoalType(), sessionCaseIdOrNull());
        if (!guarded.equals(toolName)) {
            log.info("Tool selection guard remapped '{}' -> '{}' (goal={}, caseId={})",
                    toolName, guarded, currentGoalType(), sessionCaseIdOrNull());
        }
        ToolCallback resolved = delegate.resolve(guarded);
        if (resolved != null) {
            return resolved;
        }
        return delegate.resolve(toolName);
    }

    private static GoalType currentGoalType() {
        return ChatToolContextHolder.goalTypeOrNull();
    }

    private static String sessionCaseIdOrNull() {
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        if (sessionId == null) {
            return null;
        }
        ConversationGoalContext.Entry entry = ConversationGoalContext.get(sessionId);
        return entry != null ? entry.lastCaseId() : null;
    }
}
