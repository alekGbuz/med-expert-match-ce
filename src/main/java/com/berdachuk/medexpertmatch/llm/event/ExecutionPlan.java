package com.berdachuk.medexpertmatch.llm.event;

import java.time.Instant;
import java.util.List;

public record ExecutionPlan(String sessionId, List<Step> steps) {

    public record Step(String stepType, String targetEngine, Object params) {}
}