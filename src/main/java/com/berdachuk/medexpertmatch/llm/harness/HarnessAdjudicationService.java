package com.berdachuk.medexpertmatch.llm.harness;

import java.util.List;

public interface HarnessAdjudicationService {

    HarnessAdjudicationEntry record(
            String runId,
            String caseId,
            String reviewerId,
            HarnessWorkflowCheckpointService.CheckpointAction decision,
            String comment);

    List<HarnessAdjudicationEntry> listRecent(int limit);
}
