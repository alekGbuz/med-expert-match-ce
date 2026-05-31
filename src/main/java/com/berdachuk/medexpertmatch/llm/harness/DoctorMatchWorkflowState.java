package com.berdachuk.medexpertmatch.llm.harness;

public enum DoctorMatchWorkflowState {
    TASK_CREATED,
    PLANNING,
    CONTEXT_BUILT,
    TOOLS_EXECUTED,
    VERIFYING,
    CRITIC,
    DONE,
    FAILED
}
