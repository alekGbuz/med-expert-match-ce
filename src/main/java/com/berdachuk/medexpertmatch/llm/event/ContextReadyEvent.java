package com.berdachuk.medexpertmatch.llm.event;

import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle;

import java.time.Instant;

public record ContextReadyEvent(String sessionId, CaseContextBundle bundle, Instant timestamp) {
}