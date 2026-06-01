package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.llm.harness.HarnessChainTraceService;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/harness-chains")
public class HarnessChainsWebController {

    private final HarnessChainTraceService chainTraceService;
    private final HarnessWorkflowRunQueryService runQueryService;

    public HarnessChainsWebController(
            HarnessChainTraceService chainTraceService,
            HarnessWorkflowRunQueryService runQueryService) {
        this.chainTraceService = chainTraceService;
        this.runQueryService = runQueryService;
    }

    @GetMapping
    public String harnessChainsPage(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        List<Map<String, Object>> chains = chainTraceService.listRecentChains(50);
        List<Map<String, Object>> failedRuns = runQueryService.listRecentFailedWithBacklog(20);
        model.addAttribute("currentPage", "harness-chains");
        model.addAttribute("chains", chains);
        model.addAttribute("failedRuns", failedRuns);
        return "admin/harness-chains";
    }
}
