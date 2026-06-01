package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.util.CaseIdExtractor;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundleService;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Builds chat prompt hints for case-ID vs free-text clinical requests from external templates.
 */
@Component
public class ChatCasePromptSupport {

    private final PromptTemplate caseIdHintTemplate;
    private final PromptTemplate noCaseIdHintTemplate;
    private final CaseContextBundleService caseContextBundleService;

    public ChatCasePromptSupport(
            @Qualifier("chatCaseIdHintPromptTemplate") PromptTemplate caseIdHintTemplate,
            @Qualifier("chatNoCaseIdHintPromptTemplate") PromptTemplate noCaseIdHintTemplate,
            CaseContextBundleService caseContextBundleService) {
        this.caseIdHintTemplate = caseIdHintTemplate;
        this.noCaseIdHintTemplate = noCaseIdHintTemplate;
        this.caseContextBundleService = caseContextBundleService;
    }

    public String buildCaseToolHints(String content) {
        return CaseIdExtractor.extractFromText(content)
                .map(caseId -> {
                    String hint = caseIdHintTemplate.render(Map.of("caseId", caseId));
                    CaseContextBundle bundle = caseContextBundleService.build(caseId, CaseContextIntent.CHAT_AUTO);
                    return hint + "\n\nContext bundle: " + bundle.summary();
                })
                .orElseGet(() -> noCaseIdHintTemplate.render(Collections.emptyMap()));
    }
}
