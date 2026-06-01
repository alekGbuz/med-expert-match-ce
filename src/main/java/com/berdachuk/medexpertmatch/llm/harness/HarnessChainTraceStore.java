package com.berdachuk.medexpertmatch.llm.harness;

import java.util.List;

public interface HarnessChainTraceStore {

    void append(HarnessChainEvent event);

    List<HarnessChainEvent> findRecent(int limit);
}
