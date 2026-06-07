package com.berdachuk.medexpertmatch.core.monitoring;

import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer counters for LLM API calls by client type (M64 Phase 0).
 */
@Component
public class LlmCallMetrics {

    private final MeterRegistry meterRegistry;

    public LlmCallMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCall(LlmClientType clientType) {
        meterRegistry.counter("llm.calls.by_client.total",
                "client_type", clientType.name())
                .increment();
    }
}
