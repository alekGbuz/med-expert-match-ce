SELECT COUNT(*)
FROM medexpertmatch.clinical_experiences
WHERE updated_at < :cutoffTimestamp
