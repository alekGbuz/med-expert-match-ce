package com.berdachuk.medexpertmatch.llm.harness;

public interface CaseContextBundleService {

    CaseContextBundle build(String caseId, CaseContextIntent intent);
}
