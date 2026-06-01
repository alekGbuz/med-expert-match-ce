package com.berdachuk.medexpertmatch.llm.harness;

import java.util.List;
import java.util.Map;

public record CaseContextBundle(
        String caseId,
        CaseContextIntent intent,
        List<String> coreSections,
        List<String> maybeSections,
        String summary,
        Map<String, String> attributes) {
}
