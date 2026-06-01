package com.berdachuk.medexpertmatch.llm.harness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HarnessChainTraceServiceTest {

    @Test
    @DisplayName("derives chain root from handoff session suffixes")
    void chainRootSessionId() {
        assertEquals("sess-a", HarnessChainTraceService.chainRootSessionId("sess-a-match"));
        assertEquals("sess-b", HarnessChainTraceService.chainRootSessionId("sess-b-recommend"));
        assertEquals("sess-c", HarnessChainTraceService.chainRootSessionId("sess-c"));
    }
}
