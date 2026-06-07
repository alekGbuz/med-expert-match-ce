INSERT INTO medexpertmatch.doctor_outcome_affinities (doctor_id, affinity_score, sample_count, calibrated_at)
VALUES (:doctorId, :affinityScore, :sampleCount, CURRENT_TIMESTAMP)
ON CONFLICT (doctor_id) DO UPDATE
SET affinity_score = EXCLUDED.affinity_score,
    sample_count = EXCLUDED.sample_count,
    calibrated_at = CURRENT_TIMESTAMP
