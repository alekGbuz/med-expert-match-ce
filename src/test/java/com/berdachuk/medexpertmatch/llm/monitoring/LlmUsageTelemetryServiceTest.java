package com.berdachuk.medexpertmatch.llm.monitoring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.LlmCacheSource;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmOperation;
import com.berdachuk.medexpertmatch.core.util.LlmUsageContext;
import com.berdachuk.medexpertmatch.llm.event.LlmCallCompletedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LlmUsageTelemetryServiceTest {

    private LlmRoutingMetrics routingMetrics;
    private ApplicationEventPublisher eventPublisher;
    private LogStreamService logStreamService;
    private LlmUsageTelemetryService telemetryService;
    private Logger telemetryLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        routingMetrics = mock(LlmRoutingMetrics.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        logStreamService = mock(LogStreamService.class);
        telemetryService = new LlmUsageTelemetryService(routingMetrics, logStreamService, eventPublisher);

        // Capture log events emitted by the telemetry logger so the test
        // can assert that an INFO line is produced for every LLM call —
        // both live and cache-hit (M73 acceptance criteria).
        telemetryLogger = (Logger) LoggerFactory.getLogger(LlmUsageTelemetryService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        telemetryLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        if (logAppender != null) {
            telemetryLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    private List<ILoggingEvent> capturedInfoLines() {
        return logAppender.list.stream()
                .filter(e -> e.getLevel() == Level.INFO)
                .toList();
    }

    @Test
    @DisplayName("record publishes event without message content")
    void recordsMetricsAndEvent() {
        LlmCallSnapshot snapshot = new LlmCallSnapshot(
                "sess-1",
                LlmClientType.CLINICAL,
                LlmOperation.CASE_ANALYSIS,
                "FULL",
                "MATCH_DOCTORS",
                "medgemma:1.5-4b",
                1200,
                300,
                800L,
                null,
                "stop",
                1840L,
                4821,
                2,
                6000,
                LlmCacheSource.NONE,
                false);

        telemetryService.record(snapshot);

        verify(routingMetrics).recordTokens(LlmClientType.CLINICAL,
                com.berdachuk.medexpertmatch.llm.routing.RoutingTier.FULL,
                com.berdachuk.medexpertmatch.llm.chat.GoalType.MATCH_DOCTORS,
                1200, 300);
        verify(routingMetrics).recordLatency(LlmClientType.CLINICAL, LlmOperation.CASE_ANALYSIS, 1840L);

        ArgumentCaptor<LlmCallCompletedEvent> eventCaptor = ArgumentCaptor.forClass(LlmCallCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue().snapshot().compactMessage());
    }

    @Test
    @DisplayName("cache hit records cache metric")
    void recordsCacheHit() {
        LlmUsageContext context = new LlmUsageContext("sess-2", LlmClientType.CLINICAL,
                LlmOperation.CASE_ANALYSIS, null, null, null);
        telemetryService.record(LlmCallSnapshot.fromCacheHit(context, "analyze:case-abc"));

        verify(routingMetrics).recordCacheHit(LlmCacheSource.LLM_RESPONSES_CACHE);
        verify(logStreamService).logLlmUsage(eq("sess-2"), any());
    }

    @Test
    @DisplayName("snapshot toString contains no PHI fixture text")
    void snapshotToStringSafe() {
        LlmCallSnapshot snapshot = LlmCallSnapshot.fromCacheHit(
                new LlmUsageContext("sess", LlmClientType.CLINICAL, LlmOperation.OTHER, null, null, null),
                "analyze:x");
        assertFalse(snapshot.toString().contains("John Doe"));
        assertTrue(snapshot.compactMessage().startsWith("LLM ·"));
    }

    // ==================================================================
    // M73 Part 2 — LLM cache visibility. The standard log file must
    // contain an INFO line for every LLM call (live or cached) so
    // operators can tell from the log file which calls were served
    // from the in-process Caffeine cache and which hit the LLM
    // provider. The pre-M73 code only logged live calls at DEBUG
    // level.
    // ==================================================================

    @Test
    @DisplayName("M73: live LLM call emits INFO log with cache_hit=false")
    void liveCallEmitsInfoLogWithCacheHitFalse() {
        LlmCallSnapshot snapshot = new LlmCallSnapshot(
                "sess-live",
                LlmClientType.CLINICAL,
                LlmOperation.CASE_ANALYSIS,
                null,
                null,
                "medgemma:1.5-4b",
                100,
                50,
                null,
                null,
                "stop",
                200L,
                1000,
                2,
                6000,
                LlmCacheSource.NONE,
                false);

        telemetryService.record(snapshot);

        List<ILoggingEvent> infoLines = capturedInfoLines();
        assertEquals(1, infoLines.size(),
                "exactly one INFO line must be emitted for a live LLM call");
        ILoggingEvent event = infoLines.get(0);
        String msg = event.getFormattedMessage();
        assertTrue(msg.contains("LLM usage"),
                "INFO line must be a 'LLM usage' line, was: " + msg);
        assertTrue(msg.contains("CLINICAL"),
                "INFO line must name the client type, was: " + msg);
        assertTrue(msg.contains("cache_hit=false"),
                "INFO line must declare cache_hit=false, was: " + msg);
    }

    @Test
    @DisplayName("M73: cache-hit LLM call emits INFO log with cache_hit=true")
    void cacheHitEmitsInfoLogWithCacheHitTrue() {
        LlmUsageContext context = new LlmUsageContext("sess-cache", LlmClientType.CLINICAL,
                LlmOperation.CASE_ANALYSIS, null, null, null);
        telemetryService.record(LlmCallSnapshot.fromCacheHit(context, "analyze:case-xyz"));

        List<ILoggingEvent> infoLines = capturedInfoLines();
        assertEquals(1, infoLines.size(),
                "exactly one INFO line must be emitted for a cache hit (the pre-M73 code emitted 0)");
        ILoggingEvent event = infoLines.get(0);
        String msg = event.getFormattedMessage();
        assertTrue(msg.contains("LLM usage"),
                "INFO line must be a 'LLM usage' line, was: " + msg);
        assertTrue(msg.contains("CLINICAL"));
        assertTrue(msg.contains("cache_hit=true"),
                "INFO line must declare cache_hit=true, was: " + msg);
        assertTrue(msg.contains("latency=0ms"),
                "INFO line must show the 0ms latency for a cache hit, was: " + msg);
    }
}
