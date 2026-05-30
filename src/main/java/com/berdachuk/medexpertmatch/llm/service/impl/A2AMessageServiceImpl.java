package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.automemory.PhiGuard;
import com.berdachuk.medexpertmatch.llm.service.A2AMessageService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class A2AMessageServiceImpl implements A2AMessageService {

    private static final int EVIDENCE_MAX_RESULTS = 5;

    private final MedicalAgentService medicalAgentService;
    private final MedicalAgentTools medicalAgentTools;

    public A2AMessageServiceImpl(MedicalAgentService medicalAgentService, MedicalAgentTools medicalAgentTools) {
        this.medicalAgentService = medicalAgentService;
        this.medicalAgentTools = medicalAgentTools;
    }

    @Override
    public Map<String, Object> sendMessage(Map<String, Object> request) {
        String skill = resolveSkill(request);
        String message = extractMessageText(request);

        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message text is required");
        }
        if (PhiGuard.containsPhi(message)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PHI detected in message payload — anonymize before sending");
        }

        return executeSkill(skill, message);
    }

    @Override
    public Map<String, Object> handleJsonRpc(Map<String, Object> request) {
        Object id = request.get("id");
        String version = stringField(request, "jsonrpc");
        if (version != null && !"2.0".equals(version)) {
            return jsonRpcError(id, -32600, "Invalid Request: jsonrpc must be 2.0");
        }

        String method = stringField(request, "method");
        if (method == null || method.isBlank()) {
            return jsonRpcError(id, -32600, "Invalid Request: method is required");
        }

        if (!"sendMessage".equals(method)) {
            return jsonRpcError(id, -32601, "Method not found: " + method);
        }

        Object params = request.get("params");
        if (!(params instanceof Map<?, ?> paramMap)) {
            return jsonRpcError(id, -32602, "Invalid params");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> paramBody = (Map<String, Object>) paramMap;
        try {
            Map<String, Object> result = sendMessage(paramBody);
            return jsonRpcResult(id, result);
        } catch (ResponseStatusException ex) {
            return jsonRpcError(id, mapHttpStatus(ex.getStatusCode().value()), ex.getReason());
        }
    }

    private Map<String, Object> executeSkill(String skill, String message) {
        String sessionId = "a2a-" + UUID.randomUUID();
        OrchestrationContextHolder.setSessionId(sessionId);
        try {
            return switch (skill) {
                case "doctor_match" -> bridgeDoctorMatch(message);
                case "evidence_search" -> bridgeEvidenceSearch(message);
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown skill: " + skill);
            };
        } finally {
            OrchestrationContextHolder.clear();
        }
    }

    private Map<String, Object> bridgeDoctorMatch(String message) {
        log.info("A2A doctor_match bridge — message length {}", message.length());
        MedicalAgentService.AgentResponse agentResponse = medicalAgentService.matchFromText(
                message, Map.of("interactiveIntake", false));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", agentResponse.response() != null ? agentResponse.response() : "");
        result.put("metadata", sanitizeMetadata(agentResponse.metadata()));
        result.put("phiDetected", false);

        return Map.of(
                "status", "completed",
                "skill", "doctor_match",
                "result", result);
    }

    private Map<String, Object> bridgeEvidenceSearch(String message) {
        log.info("A2A evidence_search bridge — message length {}", message.length());
        List<String> guidelines = medicalAgentTools.search_clinical_guidelines(message, null, EVIDENCE_MAX_RESULTS);
        List<String> pubmed = medicalAgentTools.query_pubmed(message, EVIDENCE_MAX_RESULTS);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("guidelines", guidelines);
        result.put("pubmed", pubmed);
        result.put("summary", buildEvidenceSummary(guidelines, pubmed));
        result.put("phiDetected", false);

        return Map.of(
                "status", "completed",
                "skill", "evidence_search",
                "result", result);
    }

    private static String buildEvidenceSummary(List<String> guidelines, List<String> pubmed) {
        return "Guidelines: " + guidelines.size() + " item(s); PubMed: " + pubmed.size() + " item(s)";
    }

    private static Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            String asText = value.toString();
            if (!PhiGuard.containsPhi(asText)) {
                safe.put(key, value);
            }
        });
        return safe;
    }

    private static Map<String, Object> jsonRpcResult(Object id, Map<String, Object> result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private static Map<String, Object> jsonRpcError(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message != null ? message : "error"));
        return response;
    }

    private static int mapHttpStatus(int httpStatus) {
        return switch (httpStatus) {
            case 400 -> -32602;
            case 404 -> -32601;
            default -> -32000;
        };
    }

    private static String resolveSkill(Map<String, Object> request) {
        String skill = stringField(request, "skill");
        if (skill == null || skill.isBlank()) {
            Object params = request.get("params");
            if (params instanceof Map<?, ?> paramMap) {
                Object nested = paramMap.get("skill");
                if (nested != null) {
                    skill = nested.toString();
                }
            }
        }
        return skill != null && !skill.isBlank() ? skill : "doctor_match";
    }

    @SuppressWarnings("unchecked")
    private static String extractMessageText(Map<String, Object> request) {
        Object params = request.get("params");
        if (params instanceof Map<?, ?> paramMap) {
            Object message = paramMap.get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object parts = messageMap.get("parts");
                if (parts instanceof Iterable<?> iterable) {
                    for (Object part : iterable) {
                        if (part instanceof Map<?, ?> partMap) {
                            Object text = partMap.get("text");
                            if (text != null) {
                                return text.toString();
                            }
                        }
                    }
                }
                Object text = messageMap.get("text");
                if (text != null) {
                    return text.toString();
                }
            }
            if (message instanceof String s) {
                return s;
            }
        }
        Object direct = request.get("message");
        return direct != null ? direct.toString() : null;
    }

    private static String stringField(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value != null ? value.toString() : null;
    }
}
