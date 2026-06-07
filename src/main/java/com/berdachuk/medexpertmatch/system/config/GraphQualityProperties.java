package com.berdachuk.medexpertmatch.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medexpertmatch.system.graph-quality")
public record GraphQualityProperties(
        int staleExperienceDays,
        int evidenceFreshnessTtlDays) {

    public GraphQualityProperties {
        if (staleExperienceDays <= 0) {
            staleExperienceDays = 365;
        }
        if (evidenceFreshnessTtlDays <= 0) {
            evidenceFreshnessTtlDays = 180;
        }
    }
}
