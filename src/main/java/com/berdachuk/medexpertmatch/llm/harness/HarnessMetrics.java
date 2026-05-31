package com.berdachuk.medexpertmatch.llm.harness;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class HarnessMetrics {

    private final Counter verifyFailureCounter;
    private final Counter criticFailureCounter;

    public HarnessMetrics(MeterRegistry meterRegistry) {
        this.verifyFailureCounter = Counter.builder("harness.verify.failure")
                .description("Harness verify step failures")
                .register(meterRegistry);
        this.criticFailureCounter = Counter.builder("harness.critic.failure")
                .description("Harness critic step failures")
                .register(meterRegistry);
    }

    public void recordVerifyFailure(String reason) {
        verifyFailureCounter.increment();
    }

    public void recordCriticFailure(String reason) {
        criticFailureCounter.increment();
    }
}
