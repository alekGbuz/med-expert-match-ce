INSERT INTO medexpertmatch.match_outcomes (id, case_id, doctor_id, label, recorded_at)
VALUES (:id, :caseId, :doctorId, :label, COALESCE(:recordedAt, CURRENT_TIMESTAMP))
