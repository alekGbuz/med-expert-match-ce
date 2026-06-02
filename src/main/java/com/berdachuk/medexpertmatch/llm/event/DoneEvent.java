package com.berdachuk.medexpertmatch.llm.event;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;

import java.time.Instant;

public record DoneEvent(String sessionId, MedicalAgentService.AgentResponse finalResponse, Instant timestamp) {
}