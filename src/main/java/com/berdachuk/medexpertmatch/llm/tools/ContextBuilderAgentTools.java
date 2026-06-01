package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundleService;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolCaseIdValidator;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ContextBuilderAgentTools {

    private final CaseContextBundleService caseContextBundleService;

    public ContextBuilderAgentTools(CaseContextBundleService caseContextBundleService) {
        this.caseContextBundleService = caseContextBundleService;
    }

    @Tool(description = "Build a compressed, PHI-safe context bundle for a medical case before matching or analysis.")
    public String build_case_context_bundle(
            @ToolParam(description = "24-character medical case ID") String caseId,
            @ToolParam(description = "Intent: MATCH, ANALYZE, ROUTE, EVIDENCE, CHAT_AUTO") String intent) {
        caseId = AgentToolCaseIdValidator.requireValid(caseId);
        CaseContextIntent resolved = parseIntent(intent);
        CaseContextBundle bundle = caseContextBundleService.build(caseId, resolved);
        return bundle.summary();
    }

    private static CaseContextIntent parseIntent(String intent) {
        if (intent == null || intent.isBlank()) {
            return CaseContextIntent.CHAT_AUTO;
        }
        try {
            return CaseContextIntent.valueOf(intent.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CaseContextIntent.CHAT_AUTO;
        }
    }
}
