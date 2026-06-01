package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.HarnessChainEvent;
import com.berdachuk.medexpertmatch.llm.harness.HarnessChainEventJdbcRepository;
import com.berdachuk.medexpertmatch.llm.harness.HarnessChainTraceStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JdbcHarnessChainTraceStore implements HarnessChainTraceStore {

    private final HarnessChainEventJdbcRepository repository;

    public JdbcHarnessChainTraceStore(HarnessChainEventJdbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(HarnessChainEvent event) {
        repository.insert(event);
    }

    @Override
    public List<HarnessChainEvent> findRecent(int limit) {
        return repository.findRecent(limit);
    }
}
