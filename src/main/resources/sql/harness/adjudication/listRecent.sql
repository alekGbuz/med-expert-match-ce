SELECT id, run_id, case_id, reviewer_id, decision, comment, recorded_at
FROM medexpertmatch.llm_harness_adjudication_log
ORDER BY recorded_at DESC
LIMIT :limit
