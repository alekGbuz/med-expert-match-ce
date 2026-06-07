package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.llm.harness.HarnessAdjudicationEntry;
import com.berdachuk.medexpertmatch.llm.harness.HarnessAdjudicationService;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowCheckpointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HarnessAdjudicationServiceImpl implements HarnessAdjudicationService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/harness/adjudication/insert.sql")
    private String insertSql;

    @InjectSql("/sql/harness/adjudication/listRecent.sql")
    private String listRecentSql;

    public HarnessAdjudicationServiceImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    @Transactional
    public HarnessAdjudicationEntry record(
            String runId,
            String caseId,
            String reviewerId,
            HarnessWorkflowCheckpointService.CheckpointAction decision,
            String comment) {
        Instant recordedAt = Instant.now();
        HarnessAdjudicationEntry entry = new HarnessAdjudicationEntry(
                IdGenerator.generateId(),
                runId,
                caseId,
                reviewerId != null ? reviewerId : "unknown",
                decision.name(),
                comment,
                recordedAt);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", entry.id())
                .addValue("runId", entry.runId())
                .addValue("caseId", entry.caseId())
                .addValue("reviewerId", entry.reviewerId())
                .addValue("decision", entry.decision())
                .addValue("comment", entry.comment())
                .addValue("recordedAt", Timestamp.from(recordedAt));
        namedJdbcTemplate.update(insertSql, params);
        log.info("Recorded harness adjudication runId={} decision={} reviewer={}", runId, decision, reviewerId);
        return entry;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HarnessAdjudicationEntry> listRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return namedJdbcTemplate.query(
                listRecentSql,
                Map.of("limit", safeLimit),
                (rs, rowNum) -> new HarnessAdjudicationEntry(
                        rs.getString("id"),
                        rs.getString("run_id"),
                        rs.getString("case_id"),
                        rs.getString("reviewer_id"),
                        rs.getString("decision"),
                        rs.getString("comment"),
                        rs.getTimestamp("recorded_at").toInstant()));
    }
}
