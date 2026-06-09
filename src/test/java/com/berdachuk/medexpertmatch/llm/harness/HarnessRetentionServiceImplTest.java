package com.berdachuk.medexpertmatch.llm.harness;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HarnessRetentionServiceImplTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    private HarnessRetentionProperties enabledProps;
    private HarnessRetentionProperties disabledProps;
    private HarnessRetentionServiceImpl enabledService;

    @BeforeEach
    void setUp() {
        enabledProps = new HarnessRetentionProperties(true, 30, 50);
        disabledProps = HarnessRetentionProperties.defaults();
        enabledService = new HarnessRetentionServiceImpl(jdbc, enabledProps);
        org.springframework.test.util.ReflectionTestUtils.setField(enabledService, "deleteChainEventsSql", "DELETE FROM chain");
        org.springframework.test.util.ReflectionTestUtils.setField(enabledService, "deleteWorkflowRunsSql", "DELETE FROM workflow");
    }

    @Test
    @DisplayName("purgeExpiredRuns returns 0 when retention is disabled")
    void returnsZeroWhenDisabled() {
        HarnessRetentionServiceImpl service = new HarnessRetentionServiceImpl(jdbc, disabledProps);
        assertEquals(0, service.purgeExpiredRuns());
        verify(jdbc, never()).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    @DisplayName("purgeExpiredRuns deletes chain events and workflow runs when enabled")
    void purgesWhenEnabled() {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(5);

        int total = enabledService.purgeExpiredRuns();

        assertEquals(10, total);
    }

    @Test
    @DisplayName("purgeExpiredRuns passes correct cutoff and batch parameters")
    void passesCorrectParams() {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        enabledService.purgeExpiredRuns();

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, org.mockito.Mockito.atLeastOnce()).update(anyString(), captor.capture());

        MapSqlParameterSource params = captor.getValue();
        assertTrue(params.getValues().containsKey("cutoff"));
        assertTrue(params.getValues().containsKey("batchSize"));
        assertEquals(50, params.getValues().get("batchSize"));

        Timestamp cutoff = (Timestamp) params.getValues().get("cutoff");
        Instant expectedCutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        assertTrue(cutoff.toInstant().isBefore(expectedCutoff.plusSeconds(5)));
        assertTrue(cutoff.toInstant().isAfter(expectedCutoff.minusSeconds(5)));
    }

    @Test
    @DisplayName("purgeWorkflowRuns skips NEEDS_HUMAN state")
    void workflowRunsSkipsHumanState() {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(2);

        enabledService.purgeExpiredRuns();

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, org.mockito.Mockito.atLeastOnce()).update(anyString(), captor.capture());

        boolean foundNeedsHuman = captor.getAllValues().stream()
                .anyMatch(p -> "NEEDS_HUMAN".equals(p.getValues().get("needsHumanState")));
        assertTrue(foundNeedsHuman, "Should filter out NEEDS_HUMAN workflow runs");
    }
}
