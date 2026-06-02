package com.berdachuk.medexpertmatch.llm.event;

import java.time.Instant;

public record PlanReadyEvent(String sessionId, ExecutionPlan plan, Instant timestamp) {
}