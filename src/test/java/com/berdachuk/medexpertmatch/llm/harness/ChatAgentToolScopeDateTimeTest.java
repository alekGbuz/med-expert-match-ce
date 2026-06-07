package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;
import com.berdachuk.medexpertmatch.llm.chat.ChatToolContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatAgentToolScopeDateTimeTest {

    @AfterEach
    void clearContext() {
        ChatToolContextHolder.clear();
    }

    @Test
    @DisplayName("get_current_date_time allowed for every scoped chat profile")
    void dateTimeToolAllowedForScopedProfiles() {
        for (ChatAgentProfile profile : ChatAgentProfile.values()) {
            assertTrue(
                    ChatAgentToolScope.isAllowed(profile, "get_current_date_time"),
                    () -> "expected get_current_date_time for " + profile);
        }
    }

    @Test
    @DisplayName("get_current_date_time allowed for auto orchestrator during harness goals")
    void dateTimeToolAllowedForOrchestratorWithGoal() {
        ChatToolContextHolder.setProfile(ChatAgentProfile.AUTO);
        ChatToolContextHolder.setGoalType(GoalType.MATCH_DOCTORS);

        assertTrue(ChatAgentToolScope.isAllowed(ChatAgentProfile.AUTO, "get_current_date_time"));
    }
}
