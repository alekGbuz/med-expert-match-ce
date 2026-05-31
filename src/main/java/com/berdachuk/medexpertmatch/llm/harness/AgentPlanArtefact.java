package com.berdachuk.medexpertmatch.llm.harness;

import java.time.Instant;
import java.util.List;

public record AgentPlanArtefact(
        String sessionId,
        HarnessWorkflowType workflowType,
        String caseId,
        List<String> steps,
        List<String> acceptanceCriteria,
        Instant createdAt) {
}
