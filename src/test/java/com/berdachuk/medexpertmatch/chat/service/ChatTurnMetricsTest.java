package com.berdachuk.medexpertmatch.chat.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatTurnMetricsTest {

    @Test
    @DisplayName("Records chat turn duration and stream errors")
    void recordsMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatTurnMetrics metrics = new ChatTurnMetrics(registry);

        var sample = metrics.startTurn();
        metrics.recordTurnSuccess(sample);
        metrics.recordStreamError();
        metrics.recordToolCall();

        assertEquals(1.0, registry.get("chat.turn.duration").timer().count());
        assertEquals(1.0, registry.get("chat.stream.errors").counter().count());
        assertEquals(1.0, registry.get("chat.turn.tool_calls").counter().count());
    }
}
