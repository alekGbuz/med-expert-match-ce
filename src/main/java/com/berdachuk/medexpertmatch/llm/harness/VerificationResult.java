package com.berdachuk.medexpertmatch.llm.harness;

import java.util.List;

public record VerificationResult(
        boolean passed,
        List<String> violations,
        HarnessFailureReason reasonCode) {

    public static VerificationResult pass() {
        return new VerificationResult(true, List.of(), null);
    }

    public static VerificationResult fail(List<String> violations, HarnessFailureReason reason) {
        return new VerificationResult(false, List.copyOf(violations), reason);
    }
}
