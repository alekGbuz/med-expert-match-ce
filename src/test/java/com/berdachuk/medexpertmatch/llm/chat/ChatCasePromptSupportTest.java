package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundleService;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatCasePromptSupportTest {

    private final PromptTemplate caseIdHintTemplate = mock(PromptTemplate.class);
    private final PromptTemplate noCaseIdHintTemplate = mock(PromptTemplate.class);
    private final CaseContextBundleService caseContextBundleService = mock(CaseContextBundleService.class);
    private final ChatCasePromptSupport support =
            new ChatCasePromptSupport(caseIdHintTemplate, noCaseIdHintTemplate, caseContextBundleService);

    @BeforeEach
    void setUp() {
        OrchestrationContextHolder.setSessionId("test-session");
    }

    @AfterEach
    void tearDown() {
        OrchestrationContextHolder.clear();
        ConversationGoalContext.clear("test-session");
    }

    @Test
    @DisplayName("buildCaseToolHints guides text-based matching when no case ID")
    void textBasedHintWithoutCaseId() {
        when(noCaseIdHintTemplate.render(Collections.emptyMap()))
                .thenReturn("No medical case ID. Use match_doctors_from_text.");

        String hints = support.buildCaseToolHints(
                "90-year-old with chest pain and double vision, heart failure, need cardiologist");

        assertTrue(hints.contains("match_doctors_from_text"));
        assertTrue(hints.contains("No medical case ID"));
        verify(noCaseIdHintTemplate).render(Collections.emptyMap());
    }

    @Test
    @DisplayName("buildCaseToolHints injects case ID when present")
    void caseIdHintWhenPresent() {
        String caseId = "6a1c68963a08e800010de68e";
        when(caseIdHintTemplate.render(eq(Map.of("caseId", caseId))))
                .thenReturn("Case ID " + caseId + " for match_doctors_to_case");
        when(caseContextBundleService.build(caseId, CaseContextIntent.CHAT_AUTO))
                .thenReturn(new CaseContextBundle(caseId, CaseContextIntent.CHAT_AUTO,
                        List.of("urgency=HIGH"), List.of(), "summary", Map.of()));

        String hints = support.buildCaseToolHints("Case ID: " + caseId);

        assertTrue(hints.contains(caseId));
        assertTrue(hints.contains("match_doctors_to_case"));
        assertTrue(hints.contains("Context bundle"));
        verify(caseIdHintTemplate).render(Map.of("caseId", caseId));
    }

    @Test
    @DisplayName("shouldFallbackToGoalContextForCaseId — caseId inherited from ConversationGoalContext")
    void shouldFallbackToGoalContextForCaseId() {
        String contextCaseId = "aabbccddeeff001122334455";
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, contextCaseId, "test-session");

        when(caseIdHintTemplate.render(eq(Map.of("caseId", contextCaseId))))
                .thenReturn("Case ID " + contextCaseId + " from context");
        when(caseContextBundleService.build(contextCaseId, CaseContextIntent.CHAT_AUTO))
                .thenReturn(new CaseContextBundle(contextCaseId, CaseContextIntent.CHAT_AUTO,
                        List.of("urgency=HIGH"), List.of(), "summary", Map.of()));

        String hints = support.buildCaseToolHints(
                "tell me more about the doctor", null);

        assertTrue(hints.contains(contextCaseId));
        assertTrue(hints.contains("from context"));
    }
}
