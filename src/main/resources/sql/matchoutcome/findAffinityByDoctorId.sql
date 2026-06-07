SELECT doctor_id, affinity_score, sample_count, calibrated_at
FROM medexpertmatch.doctor_outcome_affinities
WHERE doctor_id = :doctorId
