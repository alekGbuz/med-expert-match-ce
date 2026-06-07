# M65: Human-in-the-Loop Harness

**Status:** **Done** (archived 2026-06-08)  
**Created:** 2026-06-07  
**Depends on:** M61 (archived — escalation signals); M63 (archived — `POST /api/v1/match-outcomes` for `OVERRIDDEN`)

## Problem Statement

Harness engines auto-complete match/routing flows without a structured **human adjudication** step for URGENT or
low-confidence cases. Enterprise medical workflows require auditability and clinician approval checkpoints.

## Goal

Add `HUMAN_REVIEW` checkpoint to harness state machine with admin approve/reject and audit trail feeding M63.

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | `HUMAN_REVIEW` state + persistence | `NEEDS_HUMAN` harness run + checkpoint payload | **Done** |
| 2 | Admin UI actions | `/admin/harness-chains` approve/reject with comment | **Done** |
| 3 | Audit trail | `llm_harness_adjudication_log` + `GET /api/v1/admin/harness-adjudications` | **Done** |
| 4 | Chat blocked response until approved | `ChatAssistantServiceImpl` pending clinician review progress | **Done** |
| 5 | Wire reject/override → M63 | `MatchOutcomeService.recordOutcome(OVERRIDDEN)` on reject | **Done** |

## Acceptance criteria

- [x] URGENT + low-confidence scenarios pause at HUMAN_REVIEW in harness tests (`DoctorMatchWorkflowEngineTest`)
- [x] Audit entries queryable via admin API (no PHI in test data)
- [x] Approved flow resumes to POLICY_GATE; rejected returns safe fallback
- [x] Override events recorded for M63 calibration (`HarnessWorkflowCheckpointServiceTest`)

## Key artifacts

| Artifact | Location |
|----------|----------|
| Policy pause | `llm/harness/HumanAdjudicationSupport.java` |
| Engine | `llm/harness/DoctorMatchWorkflowEngine.java` |
| Checkpoint | `llm/harness/HarnessWorkflowCheckpointService.java` |
| Audit | `llm/harness/HarnessAdjudicationService` + SQL under `sql/harness/adjudication/` |
| Admin UI | `web/controller/HarnessChainsWebController` + `templates/admin/harness-chains.html` |
| REST | `llm/rest/WorkflowCheckpointController`, `HarnessAdjudicationRestController` |
| Config | `HarnessProperties.humanAdjudicationEnabled` (default `false`) |

## Follow-up

- M66: surface harness vs chat packaging and explainability in UI
- Optional: eval flywheel family for adjudication pause/resume scenarios
