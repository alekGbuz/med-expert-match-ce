package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.core.util.LlmDateTimeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeAgentToolsTest {

    @AfterEach
    void resetClock() {
        LlmDateTimeContext.resetClock();
    }

    @Test
    @DisplayName("get_current_date_time returns UTC context block")
    void returnsCurrentDateTime() {
        LlmDateTimeContext.setClock(Clock.fixed(Instant.parse("2026-06-08T09:15:00Z"), ZoneOffset.UTC));
        DateTimeAgentTools tools = new DateTimeAgentTools();

        assertEquals("Current date and time (UTC): 2026-06-08T09:15:00Z", tools.get_current_date_time());
    }
}
