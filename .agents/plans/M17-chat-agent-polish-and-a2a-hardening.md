# M17: Chat Agent Polish & A2A Hardening

M16 delivered custom A2A JSON-RPC, domain bridge, session turn-safety unit/wiring ITs, chat agent activity panel (W-01), Markdown rendering (W-02), and Flyway V1 consolidation. M17 closes remaining UX parity gaps, strengthens test coverage, and optionally aligns with the official Spring AI A2A autoconfigure module.

**Prerequisite:** M16 complete (see `.agents/plans/archive/M16-a2a-full-integration-and-m08-closeout.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Rich SSE activity events (`tool_call`, `reasoning`, todo inline) | `feat/chat-sse-activity-events` | ⬜ Planned | 4h |
| 2 | Activity panel polish — collapse summary (agents · steps · duration) | `feat/chat-activity-panel-summary` | ⬜ Planned | 2h |
| 3 | Full >20-turn session compaction JDBC IT | `feat/session-turn-safety-jdbc-it` | ⬜ Planned | 3h |
| 4 | Markdown XSS + panel lifecycle ITs (`ChatAgenticUxIT`) | `feat/chat-ux-it-hardening` | ⬜ Planned | 2h |
| 5 | A2A JSON-RPC contract tests + error mapping | `feat/a2a-jsonrpc-hardening` | ⬜ Planned | 2h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~13h (+4h optional)**

---

## Step 1: Rich SSE activity events

Extend `/api/v1/chats/{id}/messages/stream` beyond `agent_start` / `agent_done`:

| Event | Source | UI mapping |
|---|---|---|
| `tool_call` | `ToolCallAdvisor` / orchestration hooks | Activity tree node with tool name |
| `reasoning` | Model reasoning snippets (PHI-safe) | Status line in panel |
| `todo_update` | `AgentTodoTrackingService` | Inline todo list in panel |

**Acceptance:** Panel shows at least one tool call and todo update during a match-from-chat turn.

---

## Step 2: Activity panel collapse summary

On stream `done`, collapse to compact row: `{agentCount} agents · {stepCount} steps · {duration}s` with chevron expand.

Persist summary in turn metadata (optional JSON column or client-only state for MVP).

---

## Step 3: Full session turn-safety JDBC IT

Drive >20 USER/ASSISTANT turns against live `SessionMemoryAdvisor` + Postgres (Testcontainers).

Assert post-compaction window starts on USER message and oldest turns are dropped.

Requires custom Postgres+AGE+PgVector test image in CI.

---

## Step 4: Chat UX integration tests

- `ChatAgenticUxIT`: panel visible within 500 ms of send; collapses on stream complete
- Markdown XSS IT: malicious `**[x](javascript:...)**` does not execute
- Historical `data-markdown` messages render on page load

---

## Step 5: A2A JSON-RPC hardening

- JSON-RPC 2.0 error codes for invalid params / unknown method
- Contract tests for `doctor_match` and `evidence_search` response shapes
- Rate-limit / payload size guardrails (reuse existing PHI validation)

---

## Step 6: Optional A2A autoconfigure migration

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml` per AGENTS.md.

Replace custom `A2aJsonRpcController` with `DefaultAgentExecutor` while preserving `PhiGuard` and domain bridge.

Skip if custom implementation meets interoperability needs.

---

## References

- `docs/WISHLIST.md` W-01 acceptance gaps
- `.agents/plans/archive/M16-a2a-full-integration-and-m08-closeout.md`
- `static/js/chat.js`, `ChatAssistantServiceImpl.java`
- `docs/decisions/M15-automemory-advisor-decision.md`
