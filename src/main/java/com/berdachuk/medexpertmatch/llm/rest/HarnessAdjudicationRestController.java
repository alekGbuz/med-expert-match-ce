package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.llm.harness.HarnessAdjudicationEntry;
import com.berdachuk.medexpertmatch.llm.harness.HarnessAdjudicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Harness Adjudication", description = "Human-in-the-loop harness audit trail (M65)")
@RestController
@RequestMapping("/api/v1/admin/harness-adjudications")
public class HarnessAdjudicationRestController {

    private final AdminAccessGuard adminAccessGuard;
    private final HarnessAdjudicationService adjudicationService;

    public HarnessAdjudicationRestController(
            AdminAccessGuard adminAccessGuard,
            HarnessAdjudicationService adjudicationService) {
        this.adminAccessGuard = adminAccessGuard;
        this.adjudicationService = adjudicationService;
    }

    @Operation(summary = "List recent harness adjudication audit entries")
    @GetMapping
    public List<Map<String, Object>> listRecent(@RequestParam(defaultValue = "50") int limit) {
        adminAccessGuard.requireAdmin();
        return adjudicationService.listRecent(limit).stream()
                .map(HarnessAdjudicationRestController::toView)
                .toList();
    }

    private static Map<String, Object> toView(HarnessAdjudicationEntry entry) {
        return Map.of(
                "id", entry.id(),
                "runId", entry.runId(),
                "caseId", entry.caseId() != null ? entry.caseId() : "",
                "reviewerId", entry.reviewerId(),
                "decision", entry.decision(),
                "comment", entry.comment() != null ? entry.comment() : "",
                "recordedAt", entry.recordedAt().toString());
    }
}
