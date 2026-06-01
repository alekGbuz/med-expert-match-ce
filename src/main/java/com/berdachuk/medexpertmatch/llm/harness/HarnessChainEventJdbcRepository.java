package com.berdachuk.medexpertmatch.llm.harness;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class HarnessChainEventJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public HarnessChainEventJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(HarnessChainEvent event) {
        jdbcTemplate.update("""
                INSERT INTO medexpertmatch.llm_harness_chain_event
                    (id, chain_root_session_id, session_id, case_id, step, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                event.id(),
                event.chainRootSessionId(),
                event.sessionId(),
                event.caseId(),
                event.step().name(),
                Timestamp.from(event.createdAt()));
    }

    public List<HarnessChainEvent> findRecent(int limit) {
        return jdbcTemplate.query("""
                        SELECT id, chain_root_session_id, session_id, case_id, step, created_at
                        FROM medexpertmatch.llm_harness_chain_event
                        ORDER BY created_at DESC
                        LIMIT ?
                        """,
                rowMapper(),
                limit);
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    private static RowMapper<HarnessChainEvent> rowMapper() {
        return (rs, rowNum) -> new HarnessChainEvent(
                rs.getString("id"),
                rs.getString("chain_root_session_id"),
                rs.getString("session_id"),
                rs.getString("case_id"),
                HarnessChainStep.valueOf(rs.getString("step")),
                rs.getTimestamp("created_at").toInstant());
    }
}
