package com.berdachuk.medexpertmatch.llm.harness;

import java.util.Map;

public interface MedicalAgentCriticService {

    CriticResult review(String responseText, Map<String, Object> metadata);

    record CriticResult(boolean approved, String sanitizedResponse, HarnessFailureReason reason, String detail) {}
}
