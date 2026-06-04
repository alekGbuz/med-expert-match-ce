# M54: System Health, Monitoring, and Shutdown Coverage

**Goal**: Improve test coverage for system-level concerns (health indicators, monitoring, shutdown) to bring overall coverage above 75%.

**Context**:
- M53 boosted ingestion module coverage from 24% to 71%.
- 534 tests pass with zero failures.
- Weakest remaining areas: `core.monitoring` (0%), `system.rest` (4%), `system.health` (30%), `core.shutdown` (65%).
- Overall instruction coverage: **70.1%** (target: >75%).

## Steps

### 1. System Health Indicator Unit Tests

**Current**: `system.health` package has health indicators at ~30% line coverage. Missing unit tests.

**Add**:
- `GraphHealthIndicatorTest` — unit test for graph health check:
  - Graph connection healthy
  - Graph connection unhealthy (simulated failure)
  - Graph connection timeout
- `DatabaseHealthIndicatorTest` — unit test for DB health check:
  - Database responsive
  - Database slow/timeout
  - Pool exhaustion detection
- `PgVectorHealthIndicatorTest` — unit test for vector store health:
  - Vector extension available
  - Vector extension missing
- Use Mockito for mocking dependencies (no DB required).

### 2. Graceful Shutdown Tests

**Current**: `core.shutdown` at 65%. Shutdown handler logic partially tested.

**Add**:
- `GracefulShutdownHandlerTest` — unit test for shutdown lifecycle:
  - Normal shutdown sequence
  - Shutdown timeout handling
  - Active request drain counting
  - Concurrent shutdown prevention
- `ShutdownHealthIndicatorTest` — unit test:
  - Healthy state before shutdown
  - OUT_OF_SERVICE during shutdown
  - Recovery after shutdown complete (if applicable)

### 3. Core Monitoring Tests

**Current**: `core.monitoring` at 0%. Prometheus metrics setup untested.

**Add**:
- `PrometheusMetricsConfigTest` — unit test:
  - MeterRegistry bean creation
  - Custom metrics registration
  - Metric names follow conventions
- `MeterRegistryAccessorTest` — unit test for metric access:
  - Counter increment/read
  - Timer recording
  - Gauge registration

### 4. Core Config Coverage Gaps

**Current**: `core.config` at 67%. Several config classes at 0%.

**Add**:
- `DockerSecurityConfigTest` — unit test for Docker profile security:
  - Security filter chain permits all paths in Docker profile
- `LocalSecurityConfigTest` — unit test for local profile security
- `LocalHomeBrowserLauncherTest` — unit test for browser launcher:
  - Enabled condition check
  - Disabled condition check
- `WebConfigTest` — unit test for web MVC configuration:
  - Path rewrite wrapper behavior

### 5. Coverage baseline and reporting

- Run JaCoCo coverage report
- Document per-package coverage improvements
- Update CHANGELOG with updated test counts and coverage figures

## Success Criteria

- [ ] Overall instruction coverage ≥ 75% (from 70.1%)
- [ ] `core.monitoring` coverage ≥ 50% (from 0%)
- [ ] `system.health` coverage ≥ 60% (from 30%)
- [ ] `core.shutdown` coverage ≥ 80% (from 65%)
- [ ] `core.config` coverage ≥ 75% (from 67%)
- [ ] All new tests pass with zero failures
- [ ] `mvn verify` passes with zero failures
- [ ] CHANGELOG updated

## Estimated Test Count

- 10-15 health indicator unit tests
- 5-8 shutdown handler tests
- 5-8 monitoring tests
- 5-8 config tests
- **Total: ~25-40 new tests** → overall ≥ 560 tests
