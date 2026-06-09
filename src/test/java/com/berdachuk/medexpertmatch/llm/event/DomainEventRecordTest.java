package com.berdachuk.medexpertmatch.llm.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlanReadyEventTest {

    @Test
    @DisplayName("creates event record with all fields")
    void createEventRecord() {
        var plan = new ExecutionPlan("session-1", java.util.List.of());
        var event = new PlanReadyEvent("session-1", plan, Instant.now());

        assertEquals("session-1", event.sessionId());
        assertEquals(plan, event.plan());
        assertNotNull(event.timestamp());
    }
}

class ContextReadyEventTest {

    @Test
    @DisplayName("creates event record with all fields")
    void createEventRecord() {
        var bundle = new com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle(
                "case-1", com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent.MATCH,
                java.util.List.of(), java.util.List.of(), "", java.util.Map.of());
        var event = new ContextReadyEvent("session-1", bundle, Instant.now());

        assertEquals("session-1", event.sessionId());
        assertEquals(bundle, event.bundle());
        assertNotNull(event.timestamp());
    }
}

class ResultsReadyEventTest {

    @Test
    @DisplayName("creates event record with all fields")
    void createEventRecord() {
        var response = new com.berdachuk.medexpertmatch.llm.service.MedicalAgentService.AgentResponse("result", java.util.Map.of());
        var event = new ResultsReadyEvent("session-1", response, Instant.now());

        assertEquals("session-1", event.sessionId());
        assertEquals(response, event.response());
        assertNotNull(event.timestamp());
    }
}

class DoneEventTest {

    @Test
    @DisplayName("creates event record with all fields")
    void createEventRecord() {
        var response = new com.berdachuk.medexpertmatch.llm.service.MedicalAgentService.AgentResponse("done", java.util.Map.of());
        var event = new DoneEvent("session-1", response, Instant.now());

        assertEquals("session-1", event.sessionId());
        assertEquals(response, event.finalResponse());
        assertNotNull(event.timestamp());
    }
}
