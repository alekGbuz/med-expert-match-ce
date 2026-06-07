package com.berdachuk.medexpertmatch.system.health;

import com.berdachuk.medexpertmatch.system.config.GraphQualityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EvidenceHealthIndicator implements HealthIndicator {

    /** Lightweight PubMed reachability probe (NCBI rejects bare /eutils/ — requires a named eutil). */
    private static final String PUBMED_PING_URL =
            "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi?db=pubmed&retmode=json"
                    + "&tool=MedExpertMatch&email=support@medexpertmatch.com";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final RestTemplate restTemplate;
    private final GraphQualityProperties graphQualityProperties;

    public EvidenceHealthIndicator(GraphQualityProperties graphQualityProperties) {
        this.restTemplate = new RestTemplate();
        this.graphQualityProperties = graphQualityProperties;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Instant start = Instant.now();

        try {
            String response = restTemplate.getForObject(PUBMED_PING_URL, String.class);
            long responseTime = Duration.between(start, Instant.now()).toMillis();

            details.put("status", "UP");
            details.put("responseTime", responseTime + "ms");
            details.put("endpoint", PUBMED_PING_URL);
            details.put("reachable", response != null && !response.isBlank());
            details.put("freshnessTtlDays", graphQualityProperties.evidenceFreshnessTtlDays());
            details.put("freshnessPolicy", "Evidence older than TTL should be deprioritized in scoring metadata");

            return Health.up().withDetails(details).build();
        } catch (Exception e) {
            long responseTime = Duration.between(start, Instant.now()).toMillis();
            details.put("status", "DOWN");
            details.put("responseTime", responseTime + "ms");
            details.put("endpoint", PUBMED_PING_URL);
            details.put("error", e.getClass().getSimpleName());
            details.put("message", e.getMessage());

            log.warn("PubMed evidence API health check failed: {}", e.getMessage());
            return Health.down().withDetails(details).build();
        }
    }
}
