package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.llm.harness.HarnessAdjudicationService;
import com.berdachuk.medexpertmatch.llm.harness.HarnessChainTraceService;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowCheckpointService;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/harness-chains")
public class HarnessChainsWebController {

    private final HarnessChainTraceService chainTraceService;
    private final HarnessWorkflowRunQueryService runQueryService;
    private final HarnessWorkflowCheckpointService checkpointService;
    private final HarnessAdjudicationService adjudicationService;

    public HarnessChainsWebController(
            HarnessChainTraceService chainTraceService,
            HarnessWorkflowRunQueryService runQueryService,
            HarnessWorkflowCheckpointService checkpointService,
            HarnessAdjudicationService adjudicationService) {
        this.chainTraceService = chainTraceService;
        this.runQueryService = runQueryService;
        this.checkpointService = checkpointService;
        this.adjudicationService = adjudicationService;
    }

    @GetMapping
    public String harnessChainsPage(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        List<Map<String, Object>> chains = chainTraceService.listRecentChains(50);
        List<Map<String, Object>> failedRuns = runQueryService.listRecentFailedWithBacklog(20);
        List<Map<String, Object>> pendingReviews = runQueryService.listAwaitingHumanReview(20);
        List<?> adjudicationLog = adjudicationService.listRecent(20);
        model.addAttribute("currentPage", "harness-chains");
        model.addAttribute("chains", chains);
        model.addAttribute("failedRuns", failedRuns);
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("adjudicationLog", adjudicationLog);
        return "admin/harness-chains";
    }

    @PostMapping("/{runId}/approve")
    public String approveCheckpoint(
            @PathVariable String runId,
            @RequestParam String resumeToken,
            @RequestParam(required = false) String comment,
            @RequestParam String user,
            RedirectAttributes redirectAttributes) {
        if (!"admin".equals(user)) {
            return "redirect:/";
        }
        checkpointService.checkpoint(
                runId,
                new HarnessWorkflowCheckpointService.CheckpointDecision(
                        HarnessWorkflowCheckpointService.CheckpointAction.APPROVE,
                        resumeToken,
                        comment),
                user);
        redirectAttributes.addFlashAttribute("message", "Approved run " + runId);
        return "redirect:/admin/harness-chains?user=admin";
    }

    @PostMapping("/{runId}/reject")
    public String rejectCheckpoint(
            @PathVariable String runId,
            @RequestParam String resumeToken,
            @RequestParam(required = false) String comment,
            @RequestParam String user,
            RedirectAttributes redirectAttributes) {
        if (!"admin".equals(user)) {
            return "redirect:/";
        }
        checkpointService.checkpoint(
                runId,
                new HarnessWorkflowCheckpointService.CheckpointDecision(
                        HarnessWorkflowCheckpointService.CheckpointAction.REJECT,
                        resumeToken,
                        comment),
                user);
        redirectAttributes.addFlashAttribute("message", "Rejected run " + runId);
        return "redirect:/admin/harness-chains?user=admin";
    }
}
