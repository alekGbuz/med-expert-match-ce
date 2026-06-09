package com.berdachuk.medexpertmatch.llm.harness;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HarnessMetricsTest {

    private MeterRegistry meterRegistry;
    private HarnessMetrics harnessMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        harnessMetrics = new HarnessMetrics(meterRegistry);
    }

    @Test
    @DisplayName("recordVerifyAttempt increments attempts counter")
    void recordVerifyAttemptIncrementsCounter() {
        harnessMetrics.recordVerifyAttempt();
        harnessMetrics.recordVerifyAttempt();

        double count = meterRegistry.counter("harness.verify.attempts.total").count();
        assertEquals(2.0, count);
    }

    @Test
    @DisplayName("recordVerifyFailure increments failures counter and reason counter")
    void recordVerifyFailureIncrementsCounters() {
        harnessMetrics.recordVerifyFailure("TOO_FEW_MATCHES");

        double failureCount = meterRegistry.counter("harness.verify.failures.total").count();
        double reasonCount = meterRegistry.counter("harness.verify.failure.reason", "reason", "TOO_FEW_MATCHES").count();

        assertEquals(1.0, failureCount);
        assertEquals(1.0, reasonCount);
    }

    @Test
    @DisplayName("recordVerifyFailure with null reason uses UNKNOWN")
    void recordVerifyFailureWithNullReason() {
        harnessMetrics.recordVerifyFailure(null);

        double reasonCount = meterRegistry.counter("harness.verify.failure.reason", "reason", "UNKNOWN").count();
        assertEquals(1.0, reasonCount);
    }
}
