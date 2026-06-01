package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class RecommendationWorkflowHandoffListener {

    private final MedicalAgentService medicalAgentService;
    private final HarnessProperties harnessProperties;
    private final ApplicationEventPublisher eventPublisher;

    public RecommendationWorkflowHandoffListener(
            MedicalAgentService medicalAgentService,
            HarnessProperties harnessProperties,
            ApplicationEventPublisher eventPublisher) {
        this.medicalAgentService = medicalAgentService;
        this.harnessProperties = harnessProperties;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void onDoctorMatchCompleted(DoctorMatchCompletedEvent event) {
        if (!harnessProperties.chainMatchToRecommend()) {
            return;
        }
        String handoffSessionId = event.sessionId() + "-recommend";
        String syntheticMatchId = "match-" + event.caseId();
        log.info("Chaining recommendations after doctor match caseId={} sessionId={}",
                event.caseId(), handoffSessionId);
        medicalAgentService.generateRecommendations(syntheticMatchId, Map.of("sessionId", handoffSessionId));
        eventPublisher.publishEvent(new RecommendationChainedEvent(
                event.caseId(), handoffSessionId, Instant.now()));
    }
}
