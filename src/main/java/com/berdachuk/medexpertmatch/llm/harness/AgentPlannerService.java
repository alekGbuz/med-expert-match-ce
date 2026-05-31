package com.berdachuk.medexpertmatch.llm.harness;

public interface AgentPlannerService {

    AgentPlanArtefact buildPlan(String sessionId, String caseId, HarnessWorkflowType workflowType);
}
