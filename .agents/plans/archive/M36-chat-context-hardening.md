# M36: Chat Context Hardening & Production Readiness

Hardens the M35 `ConversationGoalContext` for production: bounded cache, cross-device continuity foundation,
and follow-up detection edge-case coverage. Also delivers M34's planned end-to-end harness integration test.

**Prerequisite:** M35 complete (see `.agents/plans/archive/M35-chat-conversation-context-turn-continuity.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | Caffeine-backed bounded cache for `ConversationGoalContext` (max 500 entries, 60s TTL, replaces `ConcurrentHashMap` + daemon thread) | `feat/m36-context-cache` | ⬜ Planned | 3h |
| 2 | Cross-device conversation continuity — DB-persisted goal context via new `chat_goal_context` table + Flyway migration | `feat/m36-cross-device-context` | ⬜ Planned | 6h |
| 3 | Follow-up detection edge-case hardening — ambiguous short messages, multi-turn "what if" pivots, case-id-switch detection | `feat/m36-followup-edge-cases` | ⬜ Planned | 5h |
| 4 | End-to-end harness IT (intake → match → recommend chain) — M34 deliverable 6 | `feat/m36-e2e-harness-it` | ⬜ Planned | 8h |
| 5 | `ConversationGoalContext` JMH microbenchmark — throughput + cross-session isolation | `feat/m36-context-benchmark` | ⬜ Planned | 4h |

**Total effort: ~26h**

---

## D1: Caffeine-Backed Bounded Cache

Replace the `ConcurrentHashMap` + daemon `ScheduledExecutorService` thread in `ConversationGoalContext`
with a Caffeine cache. Caffeine is already present in the project classpath via Spring Boot.

```java
private static final Cache<String, Entry> STORE = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();
```

Changes:
- Remove `CLEANER` daemon thread and `clear()` call in static block
- `set()` → `STORE.put(sessionId, entry)`
- `get()` → `STORE.getIfPresent(sessionId)`
- `clear()` → `STORE.invalidate(sessionId)`
- Update `ConversationGoalContextTest` — no behavioral change expected

## D2: Cross-Device Conversation Continuity

Currently `ConversationGoalContext` is in-memory only — if a user switches from desktop to mobile
mid-conversation, follow-up detection fails. Add a DB-backed fallback.

### Schema (V1 only, no new Flyway version per AGENTS.md)

```sql
CREATE TABLE IF NOT EXISTS medexpertmatch.chat_goal_context (
    session_id    VARCHAR(255) PRIMARY KEY,
    goal_type     VARCHAR(64) NOT NULL,
    case_id       VARCHAR(32),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

### Implementation

- New `ChatGoalContextRepository` + `ChatGoalContextRepositoryImpl` (JDBC, external SQL files)
- `ConversationGoalContextDB` wrapper class:
  - `set()` writes to both Caffeine cache AND DB
  - `get()` reads Caffeine first, falls back to DB (populates cache on miss)
  - `clear()` removes from both
- `ChatRetentionService` extended to purge `chat_goal_context` rows when chat data is deleted
- Inject `ChatGoalContextRepository` into `ChatAssistantServiceImpl` (alternative: keep it in
  `ConversationGoalContext` as a static holder set at bootstrap)

### TDD

1. `ChatGoalContextRepositoryIT` — insert, find, delete, cleanup by session pattern
2. `ConversationGoalContextDBTest` — cache hit, cache miss (DB fallback), clear both layers
3. `ChatTurnContinuityIT` (existing) still passes with DB persistence

## D3: Follow-Up Detection Edge-Case Hardening

### Edge cases identified from the M35 review

| Input | M35 behavior | Risk | D3 fix |
|-------|-------------|------|--------|
| `"what about case 6a1b...?"` | Case ID extracted, might match keyword for ANALYZE_CASE | Follow-up intent lost, treated as fresh analysis | `detectFollowUp()` runs FIRST and has priority; already correct if case ID extraction doesn't override goal |
| `"yes but for a different case"` | `"yes"` triggers follow-up, inherits old case ID | Wrong case matched | Add `CASE_SWITCH_PATTERN` — if message contains `"different case"`, `"other case"`, `"case.*instead"`, `detectFollowUp()` returns null |
| `"tell me more"` | Would match length fallback | False positive if prior turn was GENERAL_QUESTION | `detectFollowUp()` already guards against `GENERAL_QUESTION` — correct |
| `"ok"` as response to `"Is this correct?"` | Follow-up detected, inherits MATCH_DOCTORS | Could be wrong if question wasn't about doctor matching | Acceptable — the LLM in the engine will handle context appropriately |

### Concrete changes

1. Add `CASE_SWITCH_PATTERN` to `GoalClassifier`:
   ```java
   private static final Pattern CASE_SWITCH_PATTERN = Pattern.compile(
       "\\b(different|other|another|separate)\\s+case\\b", Pattern.CASE_INSENSITIVE);
   ```
2. In `isFollowUpSignal()`, return `false` if `CASE_SWITCH_PATTERN.matcher(message).find()`
3. Add unit tests to `GoalClassifierFollowUpTest` for case-switch messages
4. Add `"tell me more"` and `"go on"` to `FOLLOW_UP_AFFIRMATIVES` (missed in M35)

## D4: End-to-End Harness IT (M34 Deliverable 6)

Carry-forward from M34's planned item 6.

### Test flow

```
POST /api/v1/cases (synthetic intake)  → caseId
POST /api/v1/chats/{chatId}/messages/stream  → "Find specialists for case {caseId}"  → SSE doctor list
POST /api/v1/chats/{chatId}/messages/stream  → "yes"  → SSE second doctor list (different ranking/rotation)
```

### Test class

`ChatTurnContinuityE2EIT.java` in `src/test/java/.../llm/service/impl/`:
- `@SpringBootTest` with Testcontainers (PostgreSQL + AGE + PgVector)
- Mocks LLM responses via WireMock or `@MockBean` on `ChatModel`
- Verifies:
  1. Turn 1 `GoalClassification` is `MATCH_DOCTORS` with correct case ID
  2. Turn 1 harness engine returns doctor matches (via mocked `MatchingService`)
  3. Turn 2 `GoalClassification` is `MATCH_DOCTORS` (inherited) with same case ID
  4. Turn 2 harness engine is re-invoked (via mocked `MedicalAgentService`)

## D5: ConversationGoalContext JMH Microbenchmark

Validate that the Caffeine-backed cache performs acceptably under concurrent access.

### Benchmarks

1. `setThenGet` — single-threaded write + read latency
2. `multiSessionThroughput` — 10 sessions, 4 threads, concurrent reads
3. `cacheEvictionOverhead` — measure cleanup cost at 500 entries

### Configuration

- 3 warmup iterations, 5 measurement iterations, 1 fork
- JMH via `src/test/java/.../jmh/` (separate from unit/IT tests)
- Not in CI pipeline — manual run only (`mvn test -Dtest=ConversationGoalContextBenchmark`)

---

## Files

| Action | File | Purpose |
|--------|------|---------|
| **MOD** | `llm/chat/ConversationGoalContext.java` | Replace ConcurrentHashMap + daemon thread with Caffeine cache |
| **NEW** | `chat/repository/ChatGoalContextRepository.java` | Interface for DB-persisted goal context |
| **NEW** | `chat/repository/impl/ChatGoalContextRepositoryImpl.java` | JDBC implementation |
| **NEW** | `src/main/resources/sql/chat/upsertGoalContext.sql` | Upsert goal context row |
| **NEW** | `src/main/resources/sql/chat/findGoalContext.sql` | Find by sessionId |
| **NEW** | `src/main/resources/sql/chat/deleteGoalContext.sql` | Delete by sessionId |
| **MOD** | `llm/chat/ConversationGoalContext.java` | Add DB fallback via `ChatGoalContextRepository` |
| **MOD** | `llm/chat/GoalClassifier.java` | Add `CASE_SWITCH_PATTERN`, expand `FOLLOW_UP_AFFIRMATIVES` |
| **MOD** | `chat/service/ChatRetentionService.java` | Purge `chat_goal_context` rows |
| **MOD** | `src/main/resources/sql/chat/deleteGoalContextByChat.sql` | Bulk delete by chat pattern |
| **MOD** | `src/main/resources/db/migration/V1__initial_schema.sql` | Add `chat_goal_context` table |
| **MOD** | `ChatAssistantServiceImpl.java` | Wire `ChatGoalContextRepository` for DB persistence |
| **NEW** | `src/test/java/.../chat/repository/ChatGoalContextRepositoryIT.java` | Integration test for DB persistence |
| **NEW** | `src/test/java/.../llm/chat/ConversationGoalContextDBTest.java` | Cache + DB fallback test |
| **MOD** | `src/test/java/.../llm/chat/GoalClassifierFollowUpTest.java` | Case-switch + new affirmative tests |
| **NEW** | `src/test/java/.../llm/service/impl/ChatTurnContinuityE2EIT.java` | Full E2E integration test |
| **NEW** | `src/test/java/.../jmh/ConversationGoalContextBenchmark.java` | JMH microbenchmark |

---

## Acceptance Criteria

1. **Caffeine cache replaces ConcurrentHashMap** — no daemon thread, bounded at 500 entries, 60s TTL
2. **DB fallback works** — follow-up detection succeeds after app restart (goal context persisted in DB)
3. **Case-switch messages NOT treated as follow-ups** — "yes but for a different case" returns null from `detectFollowUp()`
4. **E2E IT passes** — full intake → match → follow-up "yes" → re-match round-trip
5. **Benchmark shows <1μs per operation** — `set` + `get` + `clear` complete in sub-microsecond range
6. **Retention cleanup works** — deleting a chat also purges its `chat_goal_context` rows
7. **No PHI in DB table** — only `goal_type` enum + `case_id` (24-char hex) + `session_id`

---

## Ship Order

**D1 → D5 → D3 → D2 → D4**

- D1: Safe replacement — same API, better resource management
- D5: Benchmark validates D1 performance before proceeding
- D3: Edge-case hardening — additive, no D1/D2 dependency
- D2: DB persistence — depends on D1's Caffeine cache as front layer
- D4: E2E IT — depends on D2 (DB persistence) + D3 (edge cases) for realistic scenarios

---

## Rollback

- **D1:** Revert `ConversationGoalContext` to pre-M36 state (ConcurrentHashMap + daemon) — no DB schema change
- **D2:** The `chat_goal_context` table is additive — dropping it has no impact on core chat flow; `ConversationGoalContext` falls back to cache-only mode
- **D3:** No rollback needed — changes are within `GoalClassifier` and are purely additive keyword/pattern checks
- **D4:** Test-only, no production code changes
- **D5:** Benchmark-only, no production code changes

---

## Out of Scope

- Full multi-device session sync (requires session token propagation — M37 candidate)
- Non-English follow-up detection (English-only medical UI)
- ConversationGoalContext distributed cache (single-node architecture, Redis unnecessary)
- Harness engine retry with different case ID in follow-up turns (M37 candidate)
