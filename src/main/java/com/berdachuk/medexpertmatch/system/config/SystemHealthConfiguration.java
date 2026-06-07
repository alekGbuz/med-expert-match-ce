package com.berdachuk.medexpertmatch.system.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphQualityProperties.class)
public class SystemHealthConfiguration {
}
