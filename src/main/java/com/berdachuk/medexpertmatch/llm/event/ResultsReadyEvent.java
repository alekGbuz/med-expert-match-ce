package com.berdachuk.medexpertmatch.llm.event;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;

import java.time.Instant;

public record ResultsReadyEvent(String sessionId, MedicalAgentService.AgentResponse response, Instant timestamp) {
}