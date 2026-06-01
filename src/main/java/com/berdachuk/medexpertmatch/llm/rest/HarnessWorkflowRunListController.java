package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.core.security.CheckpointAccessGuard;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchWorkflowState;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Harness Workflows", description = "Harness workflow run APIs (requires X-User-Id: admin or clinician)")
@RestController
@RequestMapping("/api/v1/workflows")
public class HarnessWorkflowRunListController {

    private final CheckpointAccessGuard checkpointAccessGuard;
    private final HarnessWorkflowRunQueryService runQueryService;

    public HarnessWorkflowRunListController(
            CheckpointAccessGuard checkpointAccessGuard,
            HarnessWorkflowRunQueryService runQueryService) {
        this.checkpointAccessGuard = checkpointAccessGuard;
        this.runQueryService = runQueryService;
    }

    @Operation(summary = "List harness workflow runs by state")
    @GetMapping("/runs")
    public List<Map<String, Object>> listRuns(
            @RequestParam(defaultValue = "NEEDS_HUMAN") String state,
            @RequestParam(defaultValue = "50") int limit) {
        checkpointAccessGuard.requireCheckpointReviewer();
        DoctorMatchWorkflowState workflowState = DoctorMatchWorkflowState.valueOf(state.toUpperCase());
        if (workflowState == DoctorMatchWorkflowState.FAILED) {
            return runQueryService.listRecentFailedWithBacklog(limit);
        }
        return runQueryService.listByState(workflowState, limit);
    }
}
