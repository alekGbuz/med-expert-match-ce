package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.core.security.CheckpointAccessGuard;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowCheckpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Harness Workflows", description = "Human checkpoint APIs (requires X-User-Id: admin or clinician)")
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowCheckpointController {

    private final CheckpointAccessGuard checkpointAccessGuard;
    private final HarnessWorkflowCheckpointService checkpointService;

    public WorkflowCheckpointController(
            CheckpointAccessGuard checkpointAccessGuard,
            HarnessWorkflowCheckpointService checkpointService) {
        this.checkpointAccessGuard = checkpointAccessGuard;
        this.checkpointService = checkpointService;
    }

    @Operation(summary = "Approve or reject a paused harness workflow run")
    @PostMapping("/{runId}/checkpoint")
    public Map<String, Object> checkpoint(
            @PathVariable String runId,
            @RequestBody @Valid CheckpointRequestBody body) {
        return checkpointService.checkpoint(
                runId,
                new HarnessWorkflowCheckpointService.CheckpointDecision(
                        body.decision(), body.resumeToken(), body.comment()),
                checkpointAccessGuard.currentReviewerId());
    }

    public record CheckpointRequestBody(
            @NotNull HarnessWorkflowCheckpointService.CheckpointAction decision,
            @NotBlank String resumeToken,
            String comment) {
    }
}
