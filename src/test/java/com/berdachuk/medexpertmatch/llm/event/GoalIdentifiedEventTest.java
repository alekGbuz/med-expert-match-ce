package com.berdachuk.medexpertmatch.llm.event;

import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GoalIdentifiedEventTest {

    @Test
    @DisplayName("creates event record with all fields")
    void createEventRecord() {
        var goal = new GoalClassification(GoalType.MATCH_DOCTORS, Optional.of("case-1"), Optional.empty(), "test");
        var event = new GoalIdentifiedEvent("session-1", goal, "case-1", Instant.now());

        assertEquals("session-1", event.sessionId());
        assertEquals(GoalType.MATCH_DOCTORS, event.goal().goalType());
        assertEquals("case-1", event.caseId());
        assertNotNull(event.timestamp());
    }

    @Test
    @DisplayName("serializes to string without error")
    void toStringWorks() {
        var goal = new GoalClassification(GoalType.GENERAL_QUESTION, Optional.empty(), Optional.empty(), "general");
        var event = new GoalIdentifiedEvent("session-2", goal, null, Instant.now());
        String str = event.toString();
        assertNotNull(str);
    }
}