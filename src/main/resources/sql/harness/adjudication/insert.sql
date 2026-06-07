INSERT INTO medexpertmatch.llm_harness_adjudication_log
    (id, run_id, case_id, reviewer_id, decision, comment, recorded_at)
VALUES (:id, :runId, :caseId, :reviewerId, :decision, :comment, :recordedAt)
