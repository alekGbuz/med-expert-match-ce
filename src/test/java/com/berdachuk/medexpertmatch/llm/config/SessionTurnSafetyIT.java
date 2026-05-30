package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SessionTurnSafetyIT extends BaseIntegrationTest {

    @Autowired(required = false)
    private SessionService sessionService;

    @Test
    void sessionServiceIsAvailableWithJdbcRepository() {
        assertNotNull(sessionService, "SessionService must be wired when ai_session tables exist");
    }
}
