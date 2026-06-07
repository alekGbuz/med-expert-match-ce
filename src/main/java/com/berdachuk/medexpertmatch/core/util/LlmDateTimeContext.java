package com.berdachuk.medexpertmatch.core.util;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Supplies current UTC date/time for LLM prompts and the {@code get_current_date_time} agent tool.
 */
public final class LlmDateTimeContext {

    public static final String TOOL_NAME = "get_current_date_time";

    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private static volatile Clock clock = Clock.systemUTC();

    private LlmDateTimeContext() {
    }

    public static void setClock(Clock testClock) {
        clock = testClock;
    }

    public static void resetClock() {
        clock = Clock.systemUTC();
    }

    public static String formatNowUtc() {
        return UTC_FORMATTER.format(ZonedDateTime.now(clock).withZoneSameInstant(ZoneOffset.UTC));
    }

    public static String contextBlock() {
        return "Current date and time (UTC): " + formatNowUtc();
    }
}
