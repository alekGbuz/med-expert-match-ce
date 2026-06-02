package com.berdachuk.medexpertmatch.llm.event;

import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;

import java.time.Instant;

public record GoalIdentifiedEvent(String sessionId, GoalClassification goal, String caseId, Instant timestamp) {
}