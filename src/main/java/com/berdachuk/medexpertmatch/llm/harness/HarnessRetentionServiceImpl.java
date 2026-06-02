package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class HarnessRetentionServiceImpl implements HarnessRetentionService {

    @InjectSql("/sql/harness/deleteChainEventsOlderThan.sql")
    private String deleteChainEventsSql;

    @InjectSql("/sql/harness/deleteWorkflowRunsOlderThan.sql")
    private String deleteWorkflowRunsSql;

    private final NamedParameterJdbcTemplate jdbc;
    private final HarnessRetentionProperties properties;

    public HarnessRetentionServiceImpl(NamedParameterJdbcTemplate jdbc, HarnessRetentionProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @Override
    public int purgeExpiredRuns() {
        if (!properties.enabled()) {
            log.debug("Harness retention disabled — skipping purge");
            return 0;
        }

        Instant cutoff = Instant.now().minus(properties.retentionDays(), ChronoUnit.DAYS);
        int totalPurged = 0;

        int chainEventsPurged = purgeChainEvents(cutoff);
        totalPurged += chainEventsPurged;

        int workflowRunsPurged = purgeWorkflowRuns(cutoff);
        totalPurged += workflowRunsPurged;

        if (totalPurged > 0) {
            log.info("Harness retention purged {} rows ({} chain events, {} workflow runs)",
                    totalPurged, chainEventsPurged, workflowRunsPurged);
        }
        return totalPurged;
    }

    private int purgeChainEvents(Instant cutoff) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cutoff", Timestamp.from(cutoff))
                .addValue("batchSize", properties.batchSize());
        return jdbc.update(deleteChainEventsSql, params);
    }

    private int purgeWorkflowRuns(Instant cutoff) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cutoff", Timestamp.from(cutoff))
                .addValue("needsHumanState", DoctorMatchWorkflowState.NEEDS_HUMAN.name())
                .addValue("batchSize", properties.batchSize());
        return jdbc.update(deleteWorkflowRunsSql, params);
    }
}
