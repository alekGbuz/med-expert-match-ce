package com.berdachuk.medexpertmatch.llm.harness;

public interface AgentResponseVerifier {

    VerificationResult verify(VerificationRequest request);
}
