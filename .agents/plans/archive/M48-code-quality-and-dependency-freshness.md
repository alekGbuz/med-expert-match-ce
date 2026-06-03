# M48: Code Quality and Dependency Freshness

**Goal**: Eliminate duplicated code, close compliance gaps, and update safe dependencies.

**Context**:
- M42-M47 complete (event pipeline, security, health tracking, infrastructure tests, doc fixes, Testcontainers upgrade).
- No active non-archived plans remain. No TODOs, FIXMEs, @Disabled tests, or @Deprecated annotations.
- Technical debt identified: duplicated haversine calculation, hardcoded LLM prompt, outdated minor/patch deps.

## Steps

### 1. Extract GeoDistance utility to `core/util/`

Both `MatchingServiceImpl` and `SemanticGraphRetrievalServiceImpl` contain identical `EARTH_RADIUS_KM` + `calculateDistanceKm()` (30 lines each). Extract to `core/util/GeoDistance`:

- `GeoDistance.EARTH_RADIUS_KM` constant
- `GeoDistance.calculateDistanceKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2): double`
- Replace both call sites with the shared utility
- Add `GeoDistanceTest` unit test (3 cases: London→Paris ~343km, antipodal ~20,037km, zero-distance)

### 2. Move RerankingServiceImpl prompt to external `.st` file

`RerankingServiceImpl` (lines 80-104) hardcodes a multi-line prompt — violates LLM module convention (all prompts must be external `.st` files wired via `PromptTemplate.builder().resource(...)`).

- Create `src/main/resources/prompts/reranking-doctors.st` with StringTemplate syntax + mandatory medical disclaimer
- Wire via `PromptTemplate.builder().resource("classpath:prompts/reranking-doctors.st")` in `PromptTemplateConfig`
- Inject into `RerankingServiceImpl` constructor
- Add `RerankingServiceImplTest` verifying prompt template structure equivalence

### 3. Bump safe dependency versions (non-breaking minor/patch only)

| Dependency | Current | Target | Risk |
|-----------|---------|--------|------|
| `spring-ai` | 2.0.0-M6 | 2.0.0-M8 | Low (milestone) |
| `spring-ai-session-bom` | 0.2.0 | 0.3.0 | Low |
| `pgvector` | 0.1.4 | 0.1.6 | Low (patch) |
| `postgresql` | 42.7.10 | 42.7.11 | Low (patch) |
| `commons-io` | 2.18.0 | 2.22.0 | Low |
| `commons-lang3` | 3.14.0 | 3.20.0 | Low |
| `pdfbox` | 3.0.3 | 3.0.7 | Low |
| `caffeine` | 3.2.3 | 3.2.4 | Low (patch) |
| `datafaker` | 2.5.3 | 2.5.4 | Low (patch) |
| `httpclient5` | 5.5.2 | 5.6.1 | Low |
| `playwright` | 1.49.0 | 1.60.0 | Low (test-only) |

Update `pom.xml` properties. Excluded: Spring Boot 4.1.0-RC1, Modulith 2.1.0-RC1, springdoc-openapi 3.0.3, hapi-fhir 8.10.0, flyway-database-postgresql 12.7.0 (all major/RC — separate milestone).

### 4. `mvn verify` + fix regressions

Run full `mvn verify` to confirm all 210+ test files pass after all changes.

### 5. Update docs/version references

Update any stale Spring AI version references in docs (PRD.md, IMPLEMENTATION_PLAN.md, etc.) to reflect M8.

## Success Criteria

- [ ] `GeoDistance` utility extracted, both call sites refactored, unit test passes
- [ ] RerankingServiceImpl prompt externalized to `.st` file with medical disclaimer
- [ ] All 11 dependency versions bumped, `mvn verify` passes
- [ ] No new deprecation warnings
- [ ] Docs updated for Spring AI M8
