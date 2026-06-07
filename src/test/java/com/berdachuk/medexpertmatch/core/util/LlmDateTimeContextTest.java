package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmDateTimeContextTest {

    @AfterEach
    void resetClock() {
        LlmDateTimeContext.resetClock();
    }

    @Test
    @DisplayName("formatNow returns fixed UTC instant when clock is pinned")
    void formatNowUsesPinnedClock() {
        LlmDateTimeContext.setClock(Clock.fixed(Instant.parse("2026-06-08T14:30:00Z"), ZoneOffset.UTC));

        assertEquals("2026-06-08T14:30:00Z", LlmDateTimeContext.formatNowUtc());
    }

    @Test
    @DisplayName("contextBlock prefixes human-readable UTC label")
    void contextBlockIncludesLabel() {
        LlmDateTimeContext.setClock(Clock.fixed(Instant.parse("2026-06-08T14:30:00Z"), ZoneOffset.UTC));

        assertEquals("Current date and time (UTC): 2026-06-08T14:30:00Z", LlmDateTimeContext.contextBlock());
    }
}
