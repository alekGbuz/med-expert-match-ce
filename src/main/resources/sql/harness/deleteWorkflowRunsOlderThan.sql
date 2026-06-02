DELETE FROM medexpertmatch.llm_harness_workflow_run
WHERE updated_at < :cutoff
  AND state != :needsHumanState
LIMIT :batchSize
