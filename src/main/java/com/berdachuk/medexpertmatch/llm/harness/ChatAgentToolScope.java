package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;
import com.berdachuk.medexpertmatch.llm.chat.ChatToolContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Allowed {@code @Tool} method names per chat agent profile (M29 guarded patch).
 */
public final class ChatAgentToolScope {

    private static final Set<String> EVIDENCE = Set.of(
            "search_clinical_guidelines",
            "query_pubmed",
            "build_case_context_bundle");

    private static final Set<String> SPECIALIST_MATCHER = Set.of(
            "match_doctors_to_case",
            "match_doctors_from_text",
            "query_candidate_doctors",
            "score_doctor_match",
            "build_case_context_bundle");

    private static final Set<String> CASE_ANALYZER = Set.of(
            "analyze_case",
            "analyze_case_text",
            "extract_icd10_codes",
            "classify_urgency",
            "determine_required_specialty",
            "generate_recommendations",
            "differential_diagnosis",
            "risk_assessment",
            "build_case_context_bundle");

    private static final Set<String> TRIAGE = Set.of(
            "analyze_case_text",
            "classify_urgency",
            "determine_required_specialty",
            "extract_icd10_codes",
            "build_case_context_bundle");

    private static final Set<String> ROUTING = Set.of(
            "graph_query_candidate_centers",
            "semantic_graph_retrieval_route_score",
            "match_facilities_for_case",
            "build_case_context_bundle");

    private static final Set<String> NETWORK = Set.of(
            "graph_query_top_experts",
            "aggregate_metrics",
            "build_case_context_bundle");

    private static final Set<String> ORCHESTRATOR_DELEGATION_TOOLS = Set.of(
            "task",
            "todo_write");

    private ChatAgentToolScope() {}

    public static boolean isAllowed(ChatAgentProfile profile, String toolName) {
        if (profile == null || toolName == null || toolName.isBlank()) {
            return true;
        }
        if (profile.orchestrator()) {
            return orchestratorAllowsTool(toolName);
        }
        Set<String> allowed = allowedTools(profile);
        if (allowed == null) {
            return true;
        }
        String normalized = toolName.trim().toLowerCase();
        return allowed.contains(normalized);
    }

    public static Set<String> allowedTools(ChatAgentProfile profile) {
        return switch (profile) {
            case EVIDENCE_SCOUT -> EVIDENCE;
            case SPECIALIST_MATCHER -> SPECIALIST_MATCHER;
            case CASE_ANALYZER -> CASE_ANALYZER;
            case TRIAGE_INTAKE -> TRIAGE;
            case ROUTING_PLANNER -> ROUTING;
            case NETWORK_ANALYST -> NETWORK;
            case AUTO -> null;
        };
    }

    private static boolean orchestratorAllowsTool(String toolName) {
        GoalType goal = ChatToolContextHolder.goalTypeOrNull();
        if (goal == null || goal == GoalType.GENERAL_QUESTION) {
            return true;
        }
        String normalized = toolName.trim().toLowerCase();
        return !ORCHESTRATOR_DELEGATION_TOOLS.contains(normalized);
    }

    public static Set<String> allowedToolsForAgentCard(String agentId) {
        return ChatAgentProfile.fromAgentId(agentId)
                .map(ChatAgentToolScope::allowedTools)
                .map(scope -> scope == null ? Collections.<String>emptySet() : scope)
                .orElseGet(() -> new LinkedHashSet<>());
    }
}
