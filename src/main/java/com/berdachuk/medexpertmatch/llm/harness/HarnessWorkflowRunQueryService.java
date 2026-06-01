package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.util.IdentifierHasher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HarnessWorkflowRunQueryService {

    private final HarnessWorkflowRunStore runStore;

    public HarnessWorkflowRunQueryService(HarnessWorkflowRunStore runStore) {
        this.runStore = runStore;
    }

    public List<Map<String, Object>> listAwaitingHumanReview(int limit) {
        return listByState(DoctorMatchWorkflowState.NEEDS_HUMAN, limit);
    }

    public List<Map<String, Object>> listByState(DoctorMatchWorkflowState state, int limit) {
        return runStore.findByState(state, limit).stream()
                .map(this::toView)
                .toList();
    }

    public List<Map<String, Object>> listRecentFailedWithBacklog(int limit) {
        return runStore.findRecentByStates(List.of(DoctorMatchWorkflowState.FAILED), limit).stream()
                .map(run -> {
                    Map<String, Object> view = toView(run);
                    view.put("backlogMarkdown", HarnessFailureBacklogSupport.buildBacklogMarkdown(
                            inferFailureReason(run),
                            run.runId(),
                            run.workflowType()));
                    return view;
                })
                .toList();
    }

    private static String inferFailureReason(HarnessWorkflowRun run) {
        if (run.payloadJson() == null) {
            return "UNKNOWN";
        }
        if (run.payloadJson().contains("harnessFailureReason")) {
            return "TOOL_OUTPUT_INVALID";
        }
        return "UNKNOWN";
    }

    private Map<String, Object> toView(HarnessWorkflowRun run) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("runId", run.runId());
        view.put("sessionIdHash", IdentifierHasher.sha256Hex(run.sessionId()));
        view.put("caseId", run.caseId());
        view.put("workflowType", run.workflowType().name());
        view.put("state", run.state().name());
        view.put("updatedAt", run.updatedAt() != null ? run.updatedAt().toString() : Instant.now().toString());
        return view;
    }
}
