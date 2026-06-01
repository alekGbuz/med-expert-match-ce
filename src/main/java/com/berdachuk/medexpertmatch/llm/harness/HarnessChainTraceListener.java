package com.berdachuk.medexpertmatch.llm.harness;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HarnessChainTraceListener {

    private final HarnessChainTraceService chainTraceService;

    public HarnessChainTraceListener(HarnessChainTraceService chainTraceService) {
        this.chainTraceService = chainTraceService;
    }

    @EventListener
    public void onCaseAnalysisCompleted(CaseAnalysisCompletedEvent event) {
        chainTraceService.record(event.sessionId(), event.caseId(), HarnessChainStep.ANALYSIS);
        log.debug("Recorded harness chain ANALYSIS for caseId={}", event.caseId());
    }

    @EventListener
    public void onDoctorMatchCompleted(DoctorMatchCompletedEvent event) {
        chainTraceService.record(event.sessionId(), event.caseId(), HarnessChainStep.DOCTOR_MATCH);
        log.debug("Recorded harness chain DOCTOR_MATCH for caseId={}", event.caseId());
    }

    @EventListener
    public void onRecommendationChained(RecommendationChainedEvent event) {
        chainTraceService.record(event.sessionId(), event.caseId(), HarnessChainStep.RECOMMEND);
        log.debug("Recorded harness chain RECOMMEND for caseId={}", event.caseId());
    }
}
