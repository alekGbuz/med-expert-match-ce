# Active Context

## Current Focus

All milestones M01–M114 are complete. M114 fixed the integration test suite: 549 tests, 0 failures. Root cause was `ChatModel.getOptions()` not being stubbed in mock (Spring AI 2.0.0 GA uses `getOptions()` instead of deprecated `getDefaultOptions()`). Also fixed auth config for web ITs, matching service validation, and test message assertion.

M115 (dependency freshness, CI optimization) and M116 (application hardening, observability) carry over as active infrastructure work.

**M117** (completed, 2026-06-15) introduced the semantic markup and traceability foundation: stable ID scheme (`REQ-###`, `SCN-###`, `TEST-###`, `DEC-###`, `RISK-###`), new `bdd-traceability` skill, and a seed traceability table in `productContext.md`. Documentation + skill scaffolding only; no production code change required.

**M118** (completed, 2026-06-15) closed all 5 traceability gaps from M117. All 15 rows in `productContext.md` now **verified**.

**M119** (active, 2026-06-15) adopts Cucumber JVM for BDD acceptance: 3 `.feature` files (case-analyzer, doctor-matcher, routing-planner), thin step definitions, CucumberIT runner. 6 Cucumber scenarios pass.

## Current Milestone

**M119** — BDD Cucumber Adoption: Cucumber JVM deps added to pom.xml, 3 `.feature` files (6 scenarios), thin step definitions, CucumberIT runner. 6/6 Cucumber scenarios pass.

**Deferred:** M60 (FunctionGemma fine-tune — needs GPU capacity, stakeholder sign-off)

## Completed Recently

- **M114** — Integration test hardening: fixed NPE (getOptions() stub), auth 401s (medexpertmatch.auth.enabled=false), validate maxDistanceKm requires coordinates, ChatWebControllerIT assertion. 549 ITs green.
- **M113** — Presentation slides finalize: reorder slides, speaker script, mindmap alignment
- **M112** — Post-upgrade stabilization: presentation slides, local auth fix
- **M111** — Core Framework Upgrades: Spring Boot 4.0.6 → 4.1.0, Spring AI 2.0.0-M8 → 2.0.0 GA (ToolCallAdvisor → ToolCallingAdvisor rename, internalToolExecutionEnabled removal), Spring Modulith 2.0.7 → 2.1.0, spring-ai-agent-utils 0.8.0 → 0.9.0

## Open Questions

- When will GPU capacity become available for M60?
- Is WireMock 4.0.0-beta.36 API-compatible with current test fixtures?
- **M117 traceability:** Which `REQ-###` rows still lack a verified `TEST-###` link? (See "Traceability gaps" below.)

## Traceability Gaps

All 5 gaps identified in M117 have been closed in M118:

- REQ-002 (Second Opinion): `secondOpinionReturnsIndependentDifferentials()` added to MatchingServiceIT — **verified** ✓
- REQ-003 (Queue Prioritization): existing `testComputePriorityScore` / `testComputePriorityScoreWithDifferentUrgencyLevels` / `testComputePriorityScoreUsesDoctorAvailability` in SemanticGraphRetrievalServiceIT confirmed — **verified** ✓
- REQ-004 (Network Analytics): DEC-014 decided — graph-ops-only, tied to GraphQueryServiceIT — **verified** ✓
- REQ-006 (Regional Routing): existing `testSemanticGraphRetrievalRouteScore` / `testFacilityHistoricalOutcomesScore` / `testSemanticGraphRetrievalRouteScoreUsesLocationCompleteness` in SemanticGraphRetrievalServiceIT confirmed — **verified** ✓
- SCN-001..SCN-009: All 9 agent-skill test classes annotated with `SCN-###` in javadoc — **verified** ✓ (see `productContext.md`)

**No remaining traceability gaps.** The seed table in `productContext.md` has all 15 rows marked **verified**.

## Risks

- **Options mutability resolved** — Spring AI 2.0.0 GA uses `getOptions()` now; mock properly stubs both `getOptions()` and `getDefaultOptions()`

## Next Steps

1. **M120** — extend Cucumber coverage to remaining 6 agent skills (evidence-retriever, recommendation-engine, clinical-advisor, network-analyzer, clinical-guideline, triage).
2. **M116** — application hardening and observability (still active, in flight).
3. Follow-up: enforce `SCN-###` annotations on all new test classes as part of code review.