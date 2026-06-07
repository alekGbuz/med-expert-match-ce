SELECT id, case_id, doctor_id, label, recorded_at
FROM medexpertmatch.match_outcomes
WHERE case_id = :caseId AND doctor_id = :doctorId
ORDER BY recorded_at DESC
LIMIT 1
