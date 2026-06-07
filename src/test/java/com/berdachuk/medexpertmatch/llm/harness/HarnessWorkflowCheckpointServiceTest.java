package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryHarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import com.berdachuk.medexpertmatch.retrieval.service.MatchOutcomeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HarnessWorkflowCheckpointServiceTest {

    @Test
    @DisplayName("reject records adjudication and OVERRIDDEN match outcome")
    void rejectRecordsAuditAndOverride() throws Exception {
        InMemoryHarnessWorkflowRunStore runStore = new InMemoryHarnessWorkflowRunStore();
        DoctorMatchWorkflowEngine doctorEngine = mock(DoctorMatchWorkflowEngine.class);
        HarnessAdjudicationService adjudicationService = mock(HarnessAdjudicationService.class);
        MatchOutcomeService matchOutcomeService = mock(MatchOutcomeService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        Doctor doctor = new Doctor(
                "d1", "Dr. Review", null, List.of("Neurology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 46.0, 1, "borderline");
        DoctorMatchCheckpointPayload payload = new DoctorMatchCheckpointPayload(
                "case123",
                "session",
                10,
                List.of(match),
                "{}",
                1);
        String payloadJson = objectMapper.writeValueAsString(payload);
        String runId = "run-1";
        runStore.save(new HarnessWorkflowRun(
                runId,
                "session",
                "case123",
                HarnessWorkflowType.DOCTOR_MATCH,
                DoctorMatchWorkflowState.NEEDS_HUMAN,
                "token-1",
                payloadJson,
                Instant.now(),
                Instant.now()));

        HarnessWorkflowCheckpointService service = new HarnessWorkflowCheckpointService(
                runStore,
                doctorEngine,
                mock(RoutingWorkflowEngine.class),
                mock(CaseIntakeWorkflowEngine.class),
                adjudicationService,
                matchOutcomeService,
                objectMapper);

        Map<String, Object> result = service.checkpoint(
                runId,
                new HarnessWorkflowCheckpointService.CheckpointDecision(
                        HarnessWorkflowCheckpointService.CheckpointAction.REJECT,
                        "token-1",
                        "not suitable"),
                "admin");

        assertEquals("REJECT", result.get("decision"));
        assertEquals("FAILED", result.get("harnessState"));
        verify(adjudicationService).record(
                eq(runId),
                eq("case123"),
                eq("admin"),
                eq(HarnessWorkflowCheckpointService.CheckpointAction.REJECT),
                eq("not suitable"));
        verify(matchOutcomeService).recordOutcome("case123", "d1", MatchOutcomeLabel.OVERRIDDEN);
    }

    @Test
    @DisplayName("approve resumes workflow and marks run done")
    void approveResumesWorkflow() throws Exception {
        InMemoryHarnessWorkflowRunStore runStore = new InMemoryHarnessWorkflowRunStore();
        DoctorMatchWorkflowEngine doctorEngine = mock(DoctorMatchWorkflowEngine.class);
        ObjectMapper objectMapper = new ObjectMapper();

        DoctorMatchCheckpointPayload payload = new DoctorMatchCheckpointPayload(
                "case123", "session", 10, List.of(), "{}", 1);
        String payloadJson = objectMapper.writeValueAsString(payload);
        String runId = "run-2";
        runStore.save(new HarnessWorkflowRun(
                runId, "session", "case123", HarnessWorkflowType.DOCTOR_MATCH,
                DoctorMatchWorkflowState.NEEDS_HUMAN, "token-2", payloadJson, Instant.now(), Instant.now()));

        when(doctorEngine.resumeAfterCheckpoint(eq(runId), any()))
                .thenReturn(new MedicalAgentService.AgentResponse("approved", Map.of("harnessState", "DONE")));

        HarnessWorkflowCheckpointService service = new HarnessWorkflowCheckpointService(
                runStore,
                doctorEngine,
                mock(RoutingWorkflowEngine.class),
                mock(CaseIntakeWorkflowEngine.class),
                mock(HarnessAdjudicationService.class),
                mock(MatchOutcomeService.class),
                objectMapper);

        Map<String, Object> result = service.checkpoint(
                runId,
                new HarnessWorkflowCheckpointService.CheckpointDecision(
                        HarnessWorkflowCheckpointService.CheckpointAction.APPROVE,
                        "token-2",
                        null),
                "clinician");

        assertEquals("APPROVE", result.get("decision"));
        assertEquals("DONE", result.get("harnessState"));
        assertTrue(result.get("response").toString().contains("approved"));
    }
}
