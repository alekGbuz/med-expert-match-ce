DELETE FROM medexpertmatch.llm_harness_chain_event
WHERE created_at < :cutoff
LIMIT :batchSize
