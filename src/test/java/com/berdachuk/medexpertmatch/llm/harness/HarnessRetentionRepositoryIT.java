package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
class HarnessRetentionRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.getJdbcTemplate().execute("DELETE FROM medexpertmatch.llm_harness_chain_event");
        jdbc.getJdbcTemplate().execute("DELETE FROM medexpertmatch.llm_harness_workflow_run");
    }

    @Test
    @DisplayName("deleteChainEventsOlderThan removes only old chain events")
    void deletesOldChainEvents() {
        Instant now = Instant.now();
        MapSqlParameterSource oldParams = new MapSqlParameterSource()
                .addValue("id", "old-event")
                .addValue("runId", "run-1")
                .addValue("createdAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS)))
                .addValue("eventType", "TOOL_CALL")
                .addValue("eventData", "{}");
        jdbc.update("INSERT INTO medexpertmatch.llm_harness_chain_event (id, run_id, created_at, event_type, event_data) VALUES (:id, :runId, :createdAt, :eventType, :eventData)", oldParams);

        MapSqlParameterSource recentParams = new MapSqlParameterSource()
                .addValue("id", "recent-event")
                .addValue("runId", "run-2")
                .addValue("createdAt", Timestamp.from(now))
                .addValue("eventType", "TOOL_CALL")
                .addValue("eventData", "{}");
        jdbc.update("INSERT INTO medexpertmatch.llm_harness_chain_event (id, run_id, created_at, event_type, event_data) VALUES (:id, :runId, :createdAt, :eventType, :eventData)", recentParams);

        Instant cutoff = now.minus(60, ChronoUnit.DAYS);
        int deleted = jdbc.update(
                "DELETE FROM medexpertmatch.llm_harness_chain_event WHERE created_at < :cutoff LIMIT 100",
                new MapSqlParameterSource("cutoff", Timestamp.from(cutoff)));

        assertEquals(1, deleted, "Should delete only the old event");

        Long remaining = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM medexpertmatch.llm_harness_chain_event", Long.class);
        assertEquals(1, remaining);
    }

    @Test
    @DisplayName("deleteWorkflowRunsOlderThan removes old runs except NEEDS_HUMAN")
    void deletesOldWorkflowRunsExceptHuman() {
        Instant now = Instant.now();

        jdbc.update("INSERT INTO medexpertmatch.llm_harness_workflow_run (id, state, created_at, updated_at, target_entity_id, target_entity_type) VALUES (:id, :state, :createdAt, :updatedAt, :entityId, :entityType)",
                new MapSqlParameterSource()
                        .addValue("id", "old-run")
                        .addValue("state", DoctorMatchWorkflowState.DONE.name())
                        .addValue("createdAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS)))
                        .addValue("updatedAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS)))
                        .addValue("entityId", "case-1")
                        .addValue("entityType", "MEDICAL_CASE"));

        jdbc.update("INSERT INTO medexpertmatch.llm_harness_workflow_run (id, state, created_at, updated_at, target_entity_id, target_entity_type) VALUES (:id, :state, :createdAt, :updatedAt, :entityId, :entityType)",
                new MapSqlParameterSource()
                        .addValue("id", "human-run")
                        .addValue("state", DoctorMatchWorkflowState.NEEDS_HUMAN.name())
                        .addValue("createdAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS)))
                        .addValue("updatedAt", Timestamp.from(now.minus(100, ChronoUnit.DAYS)))
                        .addValue("entityId", "case-2")
                        .addValue("entityType", "MEDICAL_CASE"));

        Instant cutoff = now.minus(60, ChronoUnit.DAYS);
        int deleted = jdbc.update(
                "DELETE FROM medexpertmatch.llm_harness_workflow_run WHERE updated_at < :cutoff AND state != :needsHumanState LIMIT 100",
                new MapSqlParameterSource()
                        .addValue("cutoff", Timestamp.from(cutoff))
                        .addValue("needsHumanState", DoctorMatchWorkflowState.NEEDS_HUMAN.name()));

        assertEquals(1, deleted, "Should delete old DONE run but NOT the NEEDS_HUMAN run");

        Long remaining = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM medexpertmatch.llm_harness_workflow_run", Long.class);
        assertEquals(1, remaining);
    }
}
