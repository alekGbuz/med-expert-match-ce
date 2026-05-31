package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.service.impl.AgentCardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class A2AAgentCardTest {

    @Test
    @DisplayName("AgentCardServiceImpl builds expected skill ids and JSON-RPC endpoint")
    void serviceBuildsSkills() {
        var service = new AgentCardServiceImpl();
        Map<String, Object> card = service.buildAgentCard("http://test");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = (List<Map<String, Object>>) card.get("skills");
        assertFalse(skills.isEmpty());
        assertTrue(skills.stream().anyMatch(s -> "evidence_search".equals(s.get("id"))));
        assertTrue(skills.stream().anyMatch(s -> "doctor_match".equals(s.get("id"))));
        assertTrue(card.containsKey("endpoints"));
        @SuppressWarnings("unchecked")
        Map<String, Object> endpoints = (Map<String, Object>) card.get("endpoints");
        assertTrue(endpoints.get("jsonrpc").toString().contains("/a2a/v1/jsonrpc"));
    }
}
