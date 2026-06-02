# M37: Harness Production Hardening & Context Strategy

Hardens the harness for production observability and fixes the context-strategy gap where all chat contexts use the generic `CHAT_AUTO` intent instead of per-goal bundles.

**Prerequisite:** M36 complete (see `.agents/plans/archive/M36-chat-context-hardening.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | Per-goal `CaseContextIntent` — wire `MATCH_DOCTORS`→`MATCH`, `ANALYZE_CASE`→`ANALYZE`, `ROUTE_CASE`→`ROUTE`, `TRIAGE_INTAKE`→`MATCH`, `SEARCH_EVIDENCE`→`EVIDENCE`, default→`CHAT_AUTO` | `feat/m37-goal-context-strategy` | ⬜ Planned | 5h |
| 2 | Harness run retention — purge `llm_harness_workflow_run` + `llm_harness_chain_event` rows older than configurable cutoff via scheduled job | `feat/m37-run-retention` | ⬜ Planned | 6h |
| 3 | Harness failure rate Grafana alert — Micrometer gauge `harness.verify.failures` + Prometheus alert rule at 30% threshold | `feat/m37-harness-alerts` | ⬜ Planned | 4h |
| 4 | LLM fallback when harness engine returns 0 results — `processViaHarnessEngine` retry via chat LLM with "no matches" context hint | `feat/m37-zero-result-fallback` | ⬜ Planned | 5h |
| 5 | Streaming harness state timeline to chat UI — SSE events surface engine type + current state + verification/critic results | `feat/m37-harness-ui-timeline` | ⬜ Planned | 6h |

**Total effort: ~26h**

---

## D1: Per-Goal CaseContextIntent

Currently `ChatCasePromptSupport` hardcodes `CaseContextIntent.CHAT_AUTO` for all chat contexts. The `CaseContextBundleService.build(caseId, intent)` switches on intent type to include different fields (MATCH includes ICD-10 + specialty, ROUTE includes geography, etc.), but the chat path never selects the right intent.

### Changes

1. **`GoalClassifier` → new method `toContextIntent(GoalType)`**:
   ```java
   static CaseContextIntent toContextIntent(GoalType goalType) {
       return switch (goalType) {
           case MATCH_DOCTORS -> CaseContextIntent.MATCH;
           case ANALYZE_CASE -> CaseContextIntent.ANALYZE;
           case ROUTE_CASE -> CaseContextIntent.ROUTE;
           case TRIAGE_INTAKE -> CaseContextIntent.MATCH;
           case SEARCH_EVIDENCE -> CaseContextIntent.EVIDENCE;
           default -> CaseContextIntent.CHAT_AUTO;
       };
   }
   ```

2. **`ChatCasePromptSupport`** — accept `GoalClassification` parameter, resolve intent via `GoalClassifier.toContextIntent(goal.goalType())`, pass to `caseContextBundleService.build(caseId, intent)`.

3. **`ChatAssistantServiceImpl.buildUserPrompt(profile, content, goal)`** — already has `GoalClassification goal` — pass it through to `chatCasePromptSupport`.

4. **`GoalClassifierTest`** — add `toContextIntent` mapping tests.

## D2: Harness Run Retention

M34 deliverable 5. The `llm_harness_workflow_run` and `llm_harness_chain_event` tables accumulate indefinitely.

### Schema (V1, no new Flyway version)

Existing tables from V1 migration (lines 467-494):
```sql
CREATE TABLE IF NOT EXISTS medexpertmatch.llm_harness_workflow_run (...);
CREATE TABLE IF NOT EXISTS medexpertmatch.llm_harness_chain_event (...);
```

### Implementation

- `HarnessRetentionProperties` — `enabled` (default false), `retentionDays` (default 90), `batchSize` (default 100)
- `HarnessRetentionService` + `HarnessRetentionServiceImpl`:
  - Delete `llm_harness_chain_event` rows where `created_at < cutoff`
  - Delete `llm_harness_workflow_run` rows where `updated_at < cutoff` AND `state != 'WAITING_FOR_HUMAN'`
  - Micrometer gauge: last run timestamp, rows purged
- New SQL files: `src/main/resources/sql/harness/deleteChainEventsOlderThan.sql`, `deleteWorkflowRunsOlderThan.sql`
- `HarnessRetentionScheduler` — `@Scheduled(cron = "${medexpertmatch.harness.retention.cron:0 0 3 * * ?}")`

### TDD

1. `HarnessRetentionServiceImplTest` — retention disabled, purges non-human workflows, deletes chain events
2. `HarnessRetentionRepositoryIT` — verify SQL deletes correct rows

## D3: Harness Failure Rate Grafana Alert

### Implementation

- `HarnessMetrics` — add `Counter` for `harness.verify.failures.total` and `harness.verify.attempts.total`
- Calculate failure rate: `failures / attempts` over 5-minute window
- Prometheus alert rule in `src/main/resources/prometheus/harness-alerts.yml`:
  ```yaml
  - alert: HarnessVerifyFailureRateHigh
    expr: rate(harness_verify_failures_total[5m]) / rate(harness_verify_attempts_total[5m]) > 0.3
    for: 5m
  ```
- `HarnessMetricsTest` — verify counters increment

## D4: LLM Fallback on Zero-Result Engine

Currently if `processViaHarnessEngine` returns 0 matches, it still returns the engine response ("no matches found"). The user gets a dead end.

### Implementation

- After `DoctorMatchWorkflowEngine.execute()` returns, check if response contains zero matches
- If zero matches:
  - Build a `CaseContextBundle` with `CHAT_AUTO` intent and a hint "GraphRAG found no exact matches"
  - Fall back to `prepareTurn()` + `invokeSync()` with the LLM chat path
  - The LLM prompt includes the "no matches" hint and suggests alternative search criteria
- Gate behind `harnessProperties.zeroResultFallbackEnabled()` (new property)

### TDD

1. `ChatTurnContinuityE2ETest` — add zero-result fallback test (mock engine 0-match response, verify LLM fallback invoked)

## D5: Streaming Harness State Timeline to Chat UI

Currently `processViaHarnessEngine()` calls `logStreamService.sendLog(sessionId, "HARNESS_STATE", ...)` but the chat UI doesn't surface these as a visual timeline. The SSE already flows — just need UI consumption.

### Implementation

- `ChatAssistantServiceImpl` — emit structured SSE events during `processViaHarnessEngine`:
  ```json
  {"type": "harness_progress", "engine": "DoctorMatch", "state": "TOOLS_EXECUTED", "detail": "Found 7 candidates"}
  ```
- Chat UI (`chat.js`) — add `<div id="harnessProgress">` that renders a simple state-stepper widget
- Thymeleaf template — add progress widget HTML + CSS

### TDD

1. `ChatE2ESmokeIT` — verify harness progress events in SSE body
2. `ChatAssistantServiceImplTest` — verify harness progress events emitted

---

## Files

| Action | File | Purpose |
|--------|------|---------|
| **MOD** | `llm/chat/GoalClassifier.java` | Add `toContextIntent(GoalType)` |
| **MOD** | `llm/chat/ChatCasePromptSupport.java` | Accept `GoalClassification`, select per-goal intent |
| **MOD** | `llm/service/impl/ChatAssistantServiceImpl.java` | Pass goal to `chatCasePromptSupport`; add progress events; add zero-result fallback |
| **NEW** | `llm/harness/HarnessRetentionProperties.java` | `@ConfigurationProperties(prefix = "medexpertmatch.harness.retention")` |
| **NEW** | `llm/harness/HarnessRetentionService.java` | Interface: `int purgeExpiredRuns()` |
| **NEW** | `llm/harness/HarnessRetentionServiceImpl.java` | JDBC-based purge implementation |
| **NEW** | `src/main/resources/sql/harness/deleteChainEventsOlderThan.sql` | Delete chain events by age |
| **NEW** | `src/main/resources/sql/harness/deleteWorkflowRunsOlderThan.sql` | Delete non-human-pending workflow runs by age |
| **MOD** | `llm/harness/HarnessMetrics.java` | Add `harness.verify.failures/attempts` counters |
| **NEW** | `src/main/resources/prometheus/harness-alerts.yml` | Prometheus alert rule |
| **MOD** | `llm/config/HarnessProperties.java` | Add `zeroResultFallbackEnabled`, `retention` |
| **MOD** | `src/main/resources/static/js/chat.js` | Render harness progress stepper widget |
| **MOD** | `src/main/resources/templates/chat.html` | Add harness progress container |
| **MOD** | `src/main/resources/templates/fragments/chat.css` | Harness progress styles |
| **NEW** | `src/test/java/.../llm/harness/HarnessRetentionServiceImplTest.java` | Unit tests |
| **NEW** | `src/test/java/.../llm/harness/HarnessRetentionRepositoryIT.java` | Integration test |
| **MOD** | `src/test/java/.../llm/chat/GoalClassifierTest.java` | `toContextIntent` tests |
| **MOD** | `src/test/java/.../llm/service/impl/ChatTurnContinuityE2ETest.java` | Zero-result fallback test |

---

## Acceptance Criteria

1. **Per-goal context bundles work** — `MATCH_DOCTORS` → `CaseContextIntent.MATCH` bundle includes ICD-10 + specialty; `ANALYZE_CASE` → `ANALYZE` includes full clinical details
2. **Run retention purges old records** — workflow runs > 90 days are deleted (except WAITING_FOR_HUMAN state); chain events are also cleaned
3. **Grafana alert fires on >30% failure rate** — Prometheus rule exists and is documented
4. **Zero-result fallback works** — 0-match engine response triggers LLM fallback with "no matches" hint
5. **UI shows harness progress** — chat page displays engine type + state transitions (PLANNING→TOOLS_EXECUTED→DONE)
6. **All tests pass** — existing tests unaffected; new tests cover all new code

---

## Ship Order

**D1 → D3 → D5 → D4 → D2**

- D1: Safe replacement — changes context bundle selection, additive
- D3: Metrics only — no behavioral change, purely observational
- D5: UI additive — new SSE events + widget, no existing behavior changes
- D4: Fallback gated behind new property (default false) — zero risk on deployment
- D2: Batch deletion with safety gate (skips WAITING_FOR_HUMAN state)

---

## Rollback

- **D1:** Revert `ChatCasePromptSupport` to hardcoded `CHAT_AUTO` — context bundles degrade gracefully to full context
- **D2:** Retention is disabled by default (`enabled: false`) — no risk; tables are small (<10k rows) in normal operation
- **D3:** Alert rule file has no runtime impact — remove file to revert
- **D4:** Gated behind `zeroResultFallbackEnabled: false` — disable property to revert
- **D5:** UI-only — remove widget HTML/JS to revert

---

## Out of Scope

- Multi-agent event-driven pipeline (Phase 2 — `docs/harnes.md` item 1)
- Error-driven harness evolution with auto-GitHub issues (`docs/harnes.md` item 2)
- Chain replay API (M34 item 3 — requires API surface design)
- Clinician role UI parity (M34 item 4)
- Eval baseline auto-bump on green CI (M34 item 1 — requires CI pipeline changes)
