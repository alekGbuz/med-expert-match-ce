package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.advisor.DateTimeContextAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DateTimeContextConfiguration {

    @Bean
    DateTimeContextAdvisor dateTimeContextAdvisor() {
        return new DateTimeContextAdvisor();
    }
}
