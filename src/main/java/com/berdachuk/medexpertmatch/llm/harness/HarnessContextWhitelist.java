package com.berdachuk.medexpertmatch.llm.harness;

import java.util.Set;

/**
 * Fields that must never be dropped during harness context shaping (M68).
 */
public final class HarnessContextWhitelist {

    public static final Set<String> PRESERVED_FIELDS = Set.of(
            "case_id",
            "verify_status",
            "policy_gate_status",
            "harness_state",
            "checkpoint",
            "harnessFailureReason",
            "harnessFailureDetail"
    );

    private HarnessContextWhitelist() {
    }

    public static boolean isPreserved(String field) {
        return PRESERVED_FIELDS.contains(field);
    }
}
