package com.berdachuk.medexpertmatch.llm.tool;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ChatToolContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ConversationGoalContext;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolSelectionGuardingResolverTest {

    private static final String SESSION_ID = "user-guard-chat";
    private static final String CASE_ID = "6a23f05200155d711484cf69";

    private ToolCallback analyzeCase;
    private ToolCallback analyzeCaseText;
    private ToolCallback matchToCase;
    private ToolCallback matchFromText;

    @BeforeEach
    void setUp() {
        analyzeCase = callback("analyze_case");
        analyzeCaseText = callback("analyze_case_text");
        matchToCase = callback("match_doctors_to_case");
        matchFromText = callback("match_doctors_from_text");

        OrchestrationContextHolder.setSessionId(SESSION_ID);
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, CASE_ID, SESSION_ID);
        ChatToolContextHolder.setGoalType(GoalType.ANALYZE_CASE);
    }

    @AfterEach
    void tearDown() {
        OrchestrationContextHolder.clear();
        ConversationGoalContext.clear(SESSION_ID);
        ChatToolContextHolder.clear();
    }

    @Test
    @DisplayName("remaps analyze_case_text to analyze_case when session has case ID")
    void remapsAnalyzeTextToCaseId() {
        ToolCallbackResolver delegate = name -> switch (name) {
            case "analyze_case" -> analyzeCase;
            case "analyze_case_text" -> analyzeCaseText;
            default -> null;
        };
        ToolSelectionGuardingResolver resolver = new ToolSelectionGuardingResolver(delegate);

        ToolCallback resolved = resolver.resolve("analyze_case_text");
        assertNotNull(resolved);
        assertSame(analyzeCase, resolved);
    }

    @Test
    @DisplayName("remaps match_doctors_from_text to match_doctors_to_case when session has case ID")
    void remapsMatchFromTextToCaseId() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);
        ChatToolContextHolder.setGoalType(GoalType.MATCH_DOCTORS);

        ToolCallbackResolver delegate = name -> switch (name) {
            case "match_doctors_to_case" -> matchToCase;
            case "match_doctors_from_text" -> matchFromText;
            default -> null;
        };
        ToolSelectionGuardingResolver resolver = new ToolSelectionGuardingResolver(delegate);

        ToolCallback resolved = resolver.resolve("match_doctors_from_text");
        assertNotNull(resolved);
        assertSame(matchToCase, resolved);
    }

    private static ToolCallback callback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        when(definition.name()).thenReturn(name);
        return callback;
    }
}
