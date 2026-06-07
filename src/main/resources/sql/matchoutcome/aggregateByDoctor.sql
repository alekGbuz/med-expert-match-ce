SELECT doctor_id,
       COUNT(*) AS sample_count,
       AVG(CASE label
               WHEN 'ACCEPTED' THEN 1.0
               WHEN 'REJECTED' THEN 0.0
               WHEN 'OVERRIDDEN' THEN 0.35
               ELSE 0.5
           END) AS affinity_score
FROM medexpertmatch.match_outcomes
GROUP BY doctor_id
