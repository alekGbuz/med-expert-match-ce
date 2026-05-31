package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.exception.AgentExecutionException;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchWorkflowEngine;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentDoctorMatchingWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Doctor matching workflow delegated to harness state machine (M29).
 */
@Slf4j
@Service
public class MedicalAgentDoctorMatchingWorkflowServiceImpl implements MedicalAgentDoctorMatchingWorkflowService {

    private final DoctorMatchWorkflowEngine doctorMatchWorkflowEngine;
    private final LogStreamService logStreamService;

    public MedicalAgentDoctorMatchingWorkflowServiceImpl(
            DoctorMatchWorkflowEngine doctorMatchWorkflowEngine,
            LogStreamService logStreamService) {
        this.doctorMatchWorkflowEngine = doctorMatchWorkflowEngine;
        this.logStreamService = logStreamService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MedicalAgentService.AgentResponse matchDoctors(String caseId, Map<String, Object> request) {
        log.info("matchDoctors() called - caseId: {}", caseId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);

        try {
            logStreamService.logMatchDoctorsStep(sessionId, "Starting match doctors operation", "Case ID: " + caseId);
            logStreamService.sendProgress(sessionId, 5);
            MedicalAgentService.AgentResponse response = doctorMatchWorkflowEngine.execute(caseId, request);
            logStreamService.sendProgress(sessionId, 100);
            return response;
        } catch (AgentExecutionException e) {
            log.error("LLM error in matchDoctors for case {}", caseId, e);
            logStreamService.logError(sessionId, "Match doctors operation failed", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error in matchDoctors", e);
            logStreamService.logError(sessionId, "Match doctors operation failed", e.getMessage());
            throw new AgentExecutionException("Match doctors operation failed: " + e.getMessage(), e);
        } finally {
            OrchestrationContextHolder.clear();
            logStreamService.clearCurrentSessionId();
        }
    }
}
