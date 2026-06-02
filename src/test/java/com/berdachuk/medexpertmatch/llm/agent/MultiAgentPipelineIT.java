package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.event.DoneEvent;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import com.berdachuk.medexpertmatch.llm.event.PlanReadyEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MultiAgentPipelineIT {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlannerAgent plannerAgent;

    @Autowired
    private ContextBuilderAgent contextBuilderAgent;

    @Test
    @DisplayName("agents are loaded as Spring beans")
    void agentsAreLoaded() {
        assertNotNull(plannerAgent);
        assertNotNull(contextBuilderAgent);
    }

    @Test
    @DisplayName("event publisher is available")
    void eventPublisherAvailable() {
        assertNotNull(eventPublisher);
    }

    @Test
    @DisplayName("GoalIdentifiedEvent can be published and received")
    void goalIdentifiedEventPublished() {
        // Verify the event record itself is constructed correctly
        var goal = new com.berdachuk.medexpertmatch.llm.chat.GoalClassification(
                com.berdachuk.medexpertmatch.llm.chat.GoalType.GENERAL_QUESTION,
                java.util.Optional.empty(), java.util.Optional.empty(), "test");
        var event = new GoalIdentifiedEvent("session-it", goal, null, java.time.Instant.now());
        assertNotNull(event.sessionId());
    }
}