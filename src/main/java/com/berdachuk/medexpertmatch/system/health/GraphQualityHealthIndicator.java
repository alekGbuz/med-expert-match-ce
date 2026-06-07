package com.berdachuk.medexpertmatch.system.health;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import com.berdachuk.medexpertmatch.system.config.GraphQualityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GraphQualityHealthIndicator implements HealthIndicator {

    private final GraphService graphService;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final GraphQualityProperties properties;

    @InjectSql("/sql/system/graphquality/countDoctors.sql")
    private String countDoctorsSql;

    @InjectSql("/sql/system/graphquality/countDoctorsWithoutExperience.sql")
    private String countDoctorsWithoutExperienceSql;

    @InjectSql("/sql/system/graphquality/countStaleExperiences.sql")
    private String countStaleExperiencesSql;

    public GraphQualityHealthIndicator(
            GraphService graphService,
            NamedParameterJdbcTemplate namedJdbcTemplate,
            GraphQualityProperties properties) {
        this.graphService = graphService;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.properties = properties;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        long start = System.currentTimeMillis();
        try {
            long doctorCount = queryCount(countDoctorsSql, Map.of());
            long doctorsWithoutExperience = queryCount(countDoctorsWithoutExperienceSql, Map.of());
            Instant cutoff = Instant.now().minus(properties.staleExperienceDays(), ChronoUnit.DAYS);
            long staleExperiences = queryCount(
                    countStaleExperiencesSql,
                    Map.of("cutoffTimestamp", Timestamp.from(cutoff)));

            details.put("doctorCount", doctorCount);
            details.put("doctorsWithoutExperience", doctorsWithoutExperience);
            details.put("staleClinicalExperiences", staleExperiences);
            details.put("staleExperienceDaysThreshold", properties.staleExperienceDays());
            details.put("evidenceFreshnessTtlDays", properties.evidenceFreshnessTtlDays());

            long graphDoctorNodes = 0;
            long orphanGraphNodes = 0;
            if (graphService.graphExists()) {
                graphDoctorNodes = readGraphCount(
                        "MATCH (d:Doctor) RETURN count(d) as count");
                orphanGraphNodes = readGraphCount(
                        "MATCH (n) WHERE NOT (n)--() RETURN count(n) as count");
            }
            details.put("graphDoctorNodes", graphDoctorNodes);
            details.put("orphanGraphNodes", orphanGraphNodes);
            double coverage = doctorCount > 0 ? (double) graphDoctorNodes / doctorCount : 0.0;
            details.put("doctorGraphCoverage", String.format("%.2f", coverage));

            boolean degraded = doctorsWithoutExperience > 0 || orphanGraphNodes > 0 || staleExperiences > 0;
            details.put("status", degraded ? "DEGRADED" : "UP");
            details.put("responseTime", (System.currentTimeMillis() - start) + "ms");

            return degraded
                    ? Health.status("DEGRADED").withDetails(details).build()
                    : Health.up().withDetails(details).build();
        } catch (Exception e) {
            details.put("status", "DOWN");
            details.put("error", e.getClass().getSimpleName());
            details.put("message", e.getMessage());
            details.put("responseTime", (System.currentTimeMillis() - start) + "ms");
            log.error("Graph quality health check failed", e);
            return Health.down().withDetails(details).build();
        }
    }

    private long queryCount(String sql, Map<String, ?> params) {
        Long result = namedJdbcTemplate.queryForObject(sql, params, Long.class);
        return result != null ? result : 0L;
    }

    private long readGraphCount(String cypher) {
        List<Map<String, Object>> results = graphService.executeCypher(cypher, Map.of());
        if (results.isEmpty() || !results.getFirst().containsKey("count")) {
            return 0L;
        }
        Object count = results.getFirst().get("count");
        if (count instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(count));
    }
}
