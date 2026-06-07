# M63: Match Outcome Data Flywheel

**Status:** **Done** (archived 2026-06-08)  
**Created:** 2026-06-07  
**Depends on:** M61 (archived), M62 (archived)

## Problem Statement

Historical performance contributes 30% of Hybrid GraphRAG scoring, but labels were largely **synthetic or static**.

## Goal

Close the data flywheel: capture anonymized match outcomes and feed them into historical scoring calibration.

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | Outcome entity + repo | `MatchOutcome` record + `match_outcomes` table | **Done** |
| 2 | Ingestion API | `POST /api/v1/match-outcomes` | **Done** |
| 3 | Historical weight calibration job | `POST /api/v1/admin/match-outcomes/calibrate` + `MatchOutcomeCalibrationService` | **Done** |
| 4 | Graph quality metrics | `GraphQualityHealthIndicator` on actuator health | **Done** |
| 5 | Evidence freshness | TTL metadata on `EvidenceHealthIndicator` + `medexpertmatch.system.graph-quality` config | **Done** |

## Acceptance criteria

- [x] No PHI in outcome table or logs (synthetic/anonymized IDs in tests)
- [x] Calibration job demonstrably shifts ranking on held-out synthetic outcomes (`MatchOutcomeCalibrationEvalRunner`)
- [x] Graph quality metrics exposed on `/actuator/health`
- [x] Flyway V1 consolidation — `match_outcomes` + `doctor_outcome_affinities` in V1
- [x] `scoring-weight-ab-cases.jsonl` extended; `match-outcome-calibration-cases.jsonl` added to flywheel gate

## Key artifacts

| Artifact | Location |
|----------|----------|
| Domain | `retrieval/domain/MatchOutcome*.java` |
| REST | `retrieval/rest/MatchOutcomeRestController.java` |
| Calibration | `retrieval/service/MatchOutcomeCalibrationService` |
| Scoring wire-in | `SemanticGraphRetrievalServiceImpl.calculateHistoricalPerformanceScore` |
| Metrics | `system/health/GraphQualityHealthIndicator` |
| Eval | `llm/eval/MatchOutcomeCalibrationEvalRunner` |

## Follow-up

- M65 human override labels should call `POST /api/v1/match-outcomes` with `OVERRIDDEN`
- Optional: seed synthetic outcomes during data generation (ingestion module)
