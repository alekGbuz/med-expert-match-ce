package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;
import com.berdachuk.medexpertmatch.llm.chat.ChatToolContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatAgentToolScopeTest {

    @AfterEach
    void clearContext() {
        ChatToolContextHolder.clear();
    }

    @Test
    @DisplayName("evidence scout cannot invoke doctor match tool")
    void evidenceScoutDeniedDoctorMatch() {
        assertFalse(ChatAgentToolScope.isAllowed(ChatAgentProfile.EVIDENCE_SCOUT, "match_doctors_to_case"));
    }

    @Test
    @DisplayName("specialist matcher can invoke match_doctors_to_case")
    void specialistMatcherAllowsMatch() {
        assertTrue(ChatAgentToolScope.isAllowed(ChatAgentProfile.SPECIALIST_MATCHER, "match_doctors_to_case"));
    }

    @Test
    @DisplayName("auto orchestrator allows direct match tools")
    void autoAllowsDirectMatchTools() {
        assertTrue(ChatAgentToolScope.isAllowed(ChatAgentProfile.AUTO, "match_doctors_to_case"));
    }

    @Test
    @DisplayName("agent card always lists get_current_date_time")
    void agentCardIncludesDateTimeTool() {
        assertTrue(ChatAgentToolScope.allowedToolsForAgentCard("specialist-matcher")
                .contains("get_current_date_time"));
        assertEquals(
                java.util.Set.of("get_current_date_time"),
                ChatAgentToolScope.allowedToolsForAgentCard("auto"));
    }

    @Test
    @DisplayName("auto orchestrator denies Task when goal is doctor matching")
    void autoDeniesTaskDelegationForMatchGoal() {
        ChatToolContextHolder.setProfile(ChatAgentProfile.AUTO);
        ChatToolContextHolder.setGoalType(GoalType.MATCH_DOCTORS);
        assertFalse(ChatAgentToolScope.isAllowed(ChatAgentProfile.AUTO, "task"));
        assertFalse(ChatAgentToolScope.isAllowed(ChatAgentProfile.AUTO, "todo_write"));
        assertTrue(ChatAgentToolScope.isAllowed(ChatAgentProfile.AUTO, "match_doctors_to_case"));
    }
}
