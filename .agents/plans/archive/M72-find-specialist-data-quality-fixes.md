# M72: Find Specialist — PubMed, Match Transaction, and Data Quality Fixes

**Status:** **Done** (2026-06-09)  
**Created:** 2026-06-09 (retrospective; work done in feature branch `feature/match-doctor-data-quality-fixes`)  
**Depends on:** M70 (archived — Find Specialist explainability panel)

## Problem Statement

Three independent defects converged on the Find Specialist flow and produced
nonsensical matches against test cases with no real medical data:

1. **PubMed query returned 0 articles** for case analysis. The case analysis
   workflow pulled the PubMed query from `medicalCase.chiefComplaint()`, but a
   batch of legacy test cases had a MongoDB ObjectId (`6a1ef43dc2e41f0001b9d81d`)
   stored in `chiefComplaint`. PubMed searched for that string and returned 0
   results. Logs showed:
   `[INFO] Evidence retrieved: search_clinical_guidelines (3), query_pubmed (0 articles)`
   but no error.

2. **Match doctors failed with `current transaction is aborted` (SQL state 25P02)**.
   `MatchingServiceImpl.matchDoctorsToCase` is `@Transactional` and calls
   `MatchOutcomeRepositoryImpl.findLatestForPair` for every candidate doctor.
   The V1 migration (`V1__initial_schema.sql`) defined
   `medexpertmatch.match_outcomes` and `medexpertmatch.doctor_outcome_affinities`
   but the running database was missing both tables (V1 had been edited after
   being applied; the earlier Flyway checksum repair made the schema history
   happy but the tables were still missing). The first SELECT against the
   missing table raised `BadSqlGrammarException` and Spring marked the
   transaction as rollback-only; the subsequent
   `DELETE FROM medexpertmatch.consultation_matches WHERE case_id = ?` then
   failed with `25P02: current transaction is aborted, commands ignored until
   end of transaction block`.

3. **Find Specialist matched random specialties** (Pediatrics, Allergy,
   Rheumatology) for a case whose abstract clearly described a cardiac
   presentation, because the `chiefComplaint` ObjectId made the case have
   no usable medical data. The graph returned default 0.30 scores for every
   doctor, the vector search matched against the AI-generated abstract
   boilerplate, and the LLM interpretation admitted that 8 of 10 top
   matches were "Not directly relevant" but still presented them.

A related pre-existing circular dependency prevented the app from starting
at all once the schema was fixed — addressed as part of the same work.

## Goal

Stop the Find Specialist flow from producing misleading matches when a case
has no real medical data, and make the surrounding PubMed / LLM / Spring
plumbing robust to that situation. File-backed logs so the failures are
easy to triage next time.

## Changes

| Area | File | Change |
|------|------|--------|
| PubMed | `MedicalAgentCaseAnalysisWorkflowServiceImpl` | New `isLikelyObjectId()` guard detects 24-char hex strings in `chiefComplaint` / `currentDiagnosis` / `icd10Codes` and falls back to a generic medical term. Surfaces a WARN log. |
| LLM advisor context | `MedicalAgentLlmSupportServiceImpl` | `callMedGemmaOnce()` now sets `SESSION_ID_CONTEXT_KEY` on the `caseAnalysisChatClient` prompt from `OrchestrationContextHolder`. Was failing with `IllegalStateException: No session ID found in advisor context` for harness-driven calls. |
| Circular dependency | `EvidenceAgentTools`, `ClinicalAdvisorAgentTools` | `@Lazy` on the `caseAnalysisChatClient` constructor parameter breaks the cycle between `caseAnalysisServiceImpl` → `caseAnalysisChatClient` → `agentToolCallAdvisor` → `toolCallbackResolver` → `taskTool` → `evidenceAgentTools` / `clinicalAdvisorAgentTools` → back to `caseAnalysisChatClient`. |
| Circular dependency | `docker-compose.yml` | Adds `SPRING_MAIN_ALLOW_CIRCULAR_REFERENCES: "true"` for belt-and-suspenders defence. |
| Match validation | `MatchingServiceImpl` | New `hasInsufficientMedicalData()` rejects cases whose `chiefComplaint` / `icd10Codes` / `currentDiagnosis` are all blank or invalid. New `buildExtendedContext()` tries to mine the case's own free-text fields (`symptoms`, `additionalNotes`, `abstractText`, with LLM "thought" boilerplate stripped) and then borrows the first real `chiefComplaint` from a sibling case with the same `caseType`. Borrowed context is surfaced in the error message so the harness / user can re-run with it. |
| Match fallback | (PostgreSQL) | Manually created `medexpertmatch.match_outcomes` and `medexpertmatch.doctor_outcome_affinities` using the schema from `V1__initial_schema.sql`. V1 had been edited after it was applied, so the schema history was patched but the tables were still missing. |
| Test data | (PostgreSQL) | Updated `medical_cases` row `6a1f1fb3e279920001ea2d5f` with real medical data: `chief_complaint='chest pain and shortness of breath'`, `icd10_codes=['I20.9','R07.1','R06.0']`, `required_specialty='Cardiology'`, `current_diagnosis='suspected acute coronary syndrome'`, `patient_age=58`. |
| Logs | `docker-compose.yml` | Mounts container's `/app/logs` to host's `./logs/app/`. Sets `LOGGING_FILE_NAME`, `LOGGING_FILE_MAX_SIZE=50MB`, `LOGGING_FILE_MAX_HISTORY=7`, `LOGGING_FILE_TOTAL_SIZE_CAP=500MB`, `LOGGING_PATTERN_FILE`. |
| Logs | `start-stack.sh` | Pre-creates `./logs/app/`, truncates the log on each start, and prints the new log paths in the final help text. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | PubMed ObjectId guard + `isLikelyObjectId` helper | Done |
| 2 | LLM session ID in advisor context | Done |
| 3 | Break circular dependency (`@Lazy` + allow-circular-references) | Done |
| 4 | Recreate missing `match_outcomes` / `doctor_outcome_affinities` tables | Done |
| 5 | Match validation + `buildExtendedContext` sibling-case borrow | Done |
| 6 | Update test case with real medical data | Done |
| 7 | File-backed logs (host `./logs/app/`, rotation 50MB × 7) | Done |
| 8 | Verify with curl: good case → match, bad case → FAILED with extended context, log file written | Done |

## Verification

```
$ curl -X POST .../api/v1/agent/match/6a1f1fb3e279920001ea2d5f (real data)
→ status: COMPLETED, 5 doctors returned
$ curl -X POST .../api/v1/agent/match/6a1f1f52e279920001ea2d52 (ObjectId chiefComplaint)
→ status: FAILED
  errorMessage: "Insufficient medical data on case 6a1f1f52e279920001ea2d52 for
  matching, but related context was found: 'chest pain and shortness of breath'.
  Please re-run the match with the extended context (e.g. pass the borrowed
  chief complaint in userFocus or update the case)."
$ grep "Borrowed" logs/app/med-expert-match.log
  Borrowed medical context from sibling case 6a1f1fb3e279920001ea2d5f
  ('chest pain and shortness of breath') for case 6a1f1f52e279920001ea2d52
$ ls -la logs/app/
  -rw-rw-r-- 1 ... med-expert-match.log  (grows with each request)
```

PubMed (case analysis on the updated case):

```
WARN  pubmedQuery appears to be an ObjectId ('6a1ef43dc2e41f0001b9d81d'),
      using condition '6a1ef43dc2e41f0001b9d81d' instead for PubMed search
INFO  Case analysis evidence: condition=6a1ef43dc2e41f0001b9d81d,
      specialty=general, pubmedQuery=clinical case medical evidence, maxResults=3
INFO  query_pubmed() tool called - query: clinical case medical evidence, maxResults: 3
INFO  query_pubmed: PubMed search returned 3 articles for query: clinical case medical evidence
INFO  Case analysis evidence retrieved (caseId: ...), guidelines: 1, pubmed articles: 3
```

## Acceptance criteria

- [x] Cases with an ObjectId in `chiefComplaint` no longer return 0 PubMed articles — fallback query returns real medical results
- [x] App starts without a `BeanCreationException` for `taskTool` (circular dependency resolved)
- [x] `match_doctors_to_case` no longer aborts with `25P02: current transaction is aborted`
- [x] `match_doctors_to_case` on a case with no real medical data returns `FAILED` with a message that includes the borrowed context (or a clear "no context" message if none is available)
- [x] App logs are written to `./logs/app/med-expert-match.log` on the host and survive container restarts
- [x] `mvn test -Dtest=MatchingServiceExcludeDoctorsTest` passes
- [x] `mvn compile` and `mvn test-compile` are clean

## References

- `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalAgentCaseAnalysisWorkflowServiceImpl.java:147` — `isLikelyObjectId`
- `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalAgentLlmSupportServiceImpl.java:307` — session ID in advisor context
- `src/main/java/com/berdachuk/medexpertmatch/llm/tools/EvidenceAgentTools.java:34` — `@Lazy` on chat client
- `src/main/java/com/berdachuk/medexpertmatch/llm/tools/ClinicalAdvisorAgentTools.java:52` — `@Lazy` on chat client
- `src/main/java/com/berdachuk/medexpertmatch/retrieval/service/impl/MatchingServiceImpl.java:74` — defensive validation
- `src/main/java/com/berdachuk/medexpertmatch/retrieval/service/impl/MatchingServiceImpl.java:351` — `buildExtendedContext`
- `src/main/resources/db/migration/V1__initial_schema.sql:228` — `match_outcomes` schema
- `docs/EVIDENCE_RETRIEVAL.md` — `query_pubmed` tool
