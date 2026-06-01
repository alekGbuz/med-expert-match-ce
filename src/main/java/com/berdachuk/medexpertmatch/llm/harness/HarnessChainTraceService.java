package com.berdachuk.medexpertmatch.llm.harness;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HarnessChainTraceService {

    private final HarnessChainTraceStore chainTraceStore;

    public HarnessChainTraceService(HarnessChainTraceStore chainTraceStore) {
        this.chainTraceStore = chainTraceStore;
    }

    public void record(String sessionId, String caseId, HarnessChainStep step) {
        chainTraceStore.append(new HarnessChainEvent(
                HarnessChainEventJdbcRepository.newId(),
                chainRootSessionId(sessionId),
                sessionId,
                caseId,
                step,
                Instant.now()));
    }

    public List<Map<String, Object>> listRecentChains(int limit) {
        List<HarnessChainEvent> events = chainTraceStore.findRecent(limit);
        Map<String, Map<String, Object>> chains = new LinkedHashMap<>();
        for (HarnessChainEvent event : events) {
            chains.computeIfAbsent(event.chainRootSessionId(), root -> {
                Map<String, Object> chain = new LinkedHashMap<>();
                chain.put("chainRootSessionIdHash", hashSession(root));
                chain.put("caseId", event.caseId());
                chain.put("steps", new ArrayList<String>());
                chain.put("lastUpdatedAt", event.createdAt().toString());
                return chain;
            });
            @SuppressWarnings("unchecked")
            List<String> steps = (List<String>) chains.get(event.chainRootSessionId()).get("steps");
            if (!steps.contains(event.step().name())) {
                steps.add(event.step().name());
            }
        }
        return new ArrayList<>(chains.values());
    }

    static String chainRootSessionId(String sessionId) {
        if (sessionId == null) {
            return "unknown";
        }
        if (sessionId.endsWith("-recommend")) {
            return sessionId.substring(0, sessionId.length() - "-recommend".length());
        }
        if (sessionId.endsWith("-match")) {
            return sessionId.substring(0, sessionId.length() - "-match".length());
        }
        return sessionId;
    }

    private static String hashSession(String sessionId) {
        return com.berdachuk.medexpertmatch.core.util.IdentifierHasher.sha256Hex(sessionId);
    }
}
