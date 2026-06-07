SELECT COUNT(*)
FROM medexpertmatch.doctors d
WHERE NOT EXISTS (
    SELECT 1 FROM medexpertmatch.clinical_experiences ce WHERE ce.doctor_id = d.id
)
