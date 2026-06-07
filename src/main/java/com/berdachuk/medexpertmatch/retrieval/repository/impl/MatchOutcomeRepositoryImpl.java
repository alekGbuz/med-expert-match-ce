package com.berdachuk.medexpertmatch.retrieval.repository.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorOutcomeAffinity;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcome;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import com.berdachuk.medexpertmatch.retrieval.repository.MatchOutcomeRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MatchOutcomeRepositoryImpl implements MatchOutcomeRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/matchoutcome/insert.sql")
    private String insertSql;

    @InjectSql("/sql/matchoutcome/findLatestForPair.sql")
    private String findLatestForPairSql;

    @InjectSql("/sql/matchoutcome/aggregateByDoctor.sql")
    private String aggregateByDoctorSql;

    @InjectSql("/sql/matchoutcome/count.sql")
    private String countSql;

    @InjectSql("/sql/matchoutcome/deleteAll.sql")
    private String deleteAllSql;

    @InjectSql("/sql/matchoutcome/upsertAffinity.sql")
    private String upsertAffinitySql;

    @InjectSql("/sql/matchoutcome/findAffinityByDoctorId.sql")
    private String findAffinityByDoctorIdSql;

    @InjectSql("/sql/matchoutcome/deleteAllAffinities.sql")
    private String deleteAllAffinitiesSql;

    public MatchOutcomeRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public String insert(MatchOutcome outcome) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", outcome.id())
                .addValue("caseId", normalizeCaseId(outcome.caseId()))
                .addValue("doctorId", outcome.doctorId())
                .addValue("label", outcome.label().name())
                .addValue("recordedAt", Timestamp.from(outcome.recordedAt()));
        namedJdbcTemplate.update(insertSql, params);
        return outcome.id();
    }

    @Override
    public Optional<MatchOutcome> findLatestForPair(String caseId, String doctorId) {
        if (caseId == null || caseId.isBlank() || doctorId == null || doctorId.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> params = Map.of(
                "caseId", normalizeCaseId(caseId),
                "doctorId", doctorId.trim());
        List<MatchOutcome> results = namedJdbcTemplate.query(findLatestForPairSql, params, (rs, rowNum) ->
                new MatchOutcome(
                        rs.getString("id"),
                        rs.getString("case_id"),
                        rs.getString("doctor_id"),
                        MatchOutcomeLabel.valueOf(rs.getString("label")),
                        rs.getTimestamp("recorded_at").toInstant()));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<DoctorOutcomeAffinity> aggregateByDoctor() {
        return namedJdbcTemplate.query(aggregateByDoctorSql, Map.of(), (rs, rowNum) ->
                new DoctorOutcomeAffinity(
                        rs.getString("doctor_id"),
                        rs.getDouble("affinity_score"),
                        rs.getInt("sample_count"),
                        Instant.now()));
    }

    @Override
    public long count() {
        Long result = namedJdbcTemplate.queryForObject(countSql, Map.of(), Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.getJdbcTemplate().update(deleteAllSql);
    }

    @Override
    public void upsertAffinity(DoctorOutcomeAffinity affinity) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("doctorId", affinity.doctorId())
                .addValue("affinityScore", affinity.affinityScore())
                .addValue("sampleCount", affinity.sampleCount());
        namedJdbcTemplate.update(upsertAffinitySql, params);
    }

    @Override
    public Optional<DoctorOutcomeAffinity> findAffinityByDoctorId(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) {
            return Optional.empty();
        }
        List<DoctorOutcomeAffinity> results = namedJdbcTemplate.query(
                findAffinityByDoctorIdSql,
                Map.of("doctorId", doctorId.trim()),
                (rs, rowNum) -> new DoctorOutcomeAffinity(
                        rs.getString("doctor_id"),
                        rs.getDouble("affinity_score"),
                        rs.getInt("sample_count"),
                        rs.getTimestamp("calibrated_at").toInstant()));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public int deleteAllAffinities() {
        return namedJdbcTemplate.getJdbcTemplate().update(deleteAllAffinitiesSql);
    }

    private static String normalizeCaseId(String caseId) {
        return caseId.trim().toLowerCase();
    }
}
