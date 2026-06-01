package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatAgentToolScopeTest {

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
    @DisplayName("auto orchestrator allows all tools")
    void autoAllowsAll() {
        assertTrue(ChatAgentToolScope.isAllowed(ChatAgentProfile.AUTO, "match_doctors_to_case"));
    }
}
