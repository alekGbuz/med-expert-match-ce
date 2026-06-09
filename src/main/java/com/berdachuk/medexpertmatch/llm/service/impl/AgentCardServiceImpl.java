package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.chat.service.ChatRateLimitService;
import com.berdachuk.medexpertmatch.chat.service.RateLimitScope;
import com.berdachuk.medexpertmatch.llm.harness.ChatAgentToolScope;
import com.berdachuk.medexpertmatch.llm.service.AgentCardService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentCardServiceImpl implements AgentCardService {

    private final ChatRateLimitService chatRateLimitService;

    public AgentCardServiceImpl(ChatRateLimitService chatRateLimitService) {
        this.chatRateLimitService = chatRateLimitService;
    }

    @Override
    public Map<String, Object> buildAgentCard(String baseUrl) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("name", "MedExpertMatch");
        card.put("description", "Medical expert recommendation and clinical evidence agent (PHI-safe, research use only)");
        card.put("url", baseUrl);
        card.put("version", "1.0.0");
        card.put("protocolVersion", "0.2.0");
        card.put("skills", List.of(
                skillEntry(
                        "doctor_match",
                        "Doctor Match",
                        "Rank specialists for anonymized clinical cases using GraphRAG hybrid retrieval",
                        "specialist-matcher"),
                skillEntry(
                        "evidence_search",
                        "Evidence Search",
                        "Retrieve PubMed literature and clinical guideline summaries",
                        "evidence-scout")));
        card.put("allowedTools", Map.of(
                "doctor_match", toolList("specialist-matcher"),
                "evidence_search", toolList("evidence-scout")));
        card.put("capabilities", Map.of(
                "streaming", true,
                "pushNotifications", false));
        card.put("endpoints", Map.of(
                "jsonrpc", baseUrl + "/a2a/v1/jsonrpc",
                "stream", baseUrl + "/a2a/v1/stream",
                "skills", baseUrl + "/a2a/v1/skills"));
        card.put("rateLimits", Map.of(
                "windowSeconds", chatRateLimitService.windowSeconds(),
                "defaultPerMinute", 10,
                "highPerMinute", 30,
                "scopes", List.of(RateLimitScope.CHAT_SSE.name(), RateLimitScope.A2A.name())));
        return card;
    }

    private static Map<String, Object> skillEntry(String id, String name, String description, String agentId) {
        Map<String, Object> skill = new LinkedHashMap<>();
        skill.put("id", id);
        skill.put("name", name);
        skill.put("description", description);
        skill.put("allowedTools", toolList(agentId));
        return skill;
    }

    private static List<String> toolList(String agentId) {
        Set<String> tools = ChatAgentToolScope.allowedToolsForAgentCard(agentId);
        return new ArrayList<>(tools);
    }
}
