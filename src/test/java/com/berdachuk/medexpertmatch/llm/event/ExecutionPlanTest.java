package com.berdachuk.medexpertmatch.llm.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExecutionPlanTest {

    @Test
    @DisplayName("creates plan with steps")
    void createPlan() {
        var step1 = new ExecutionPlan.Step("CONTEXT_BUILD", "CaseContextBundleService", null);
        var step2 = new ExecutionPlan.Step("DOCTOR_MATCH", "DoctorMatchWorkflowEngine", null);
        var plan = new ExecutionPlan("session-1", List.of(step1, step2));

        assertEquals("session-1", plan.sessionId());
        assertEquals(2, plan.steps().size());
        assertEquals("CONTEXT_BUILD", plan.steps().get(0).stepType());
    }

    @Test
    @DisplayName("toString works")
    void toStringWorks() {
        var plan = new ExecutionPlan("session-1", List.of());
        assertNotNull(plan.toString());
    }
}
