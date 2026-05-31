# M19: Chat Ops, Rate Limits & Browser Interop

M18 delivered server-side markdown SSR sanitization, chat turn Micrometer metrics, A2A AgentCard discovery alias, chat E2E smoke ITs, and session compaction observability. M19 focuses on operational hardening and optional browser-level verification.

**Prerequisite:** M18 complete (see `.agents/plans/archive/M18-chat-production-readiness.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Per-user chat rate limiting (reuse token bucket pattern) | `feat/chat-rate-limits` | ⬜ Planned | 3h |
| 2 | Grafana panel for `chat.turn.*` metrics | `feat/chat-grafana-panel` | ⬜ Planned | 2h |
| 3 | Playwright smoke test (local/optional CI profile) | `feat/chat-playwright-smoke` | ⬜ Planned | 4h |
| 4 | Chat session export (JSON download, PHI-safe) | `feat/chat-export` | ⬜ Planned | 3h |
| 5 | A2A streaming `sendMessageStream` parity test | `feat/a2a-stream-parity` | ⬜ Planned | 3h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~15h (+4h optional)**

---

## Step 1: Chat rate limits

Apply existing `RateLimitingConfig` token bucket to `POST /api/v1/chats/{id}/messages/stream` with per-user tier defaults.

**Acceptance:** IT returns 429 when bucket exhausted; metric `chat.rate.limited` increments.

---

## Step 2: Grafana chat panel

Extend `grafana/dashboard.json` with panels for `chat.turn.duration`, `chat.stream.errors`, `chat.turn.tool_calls`.

---

## Step 3: Playwright smoke (optional CI)

Document in `testing` skill; run locally via profile `-Pplaywright` (test dep only).

Scenarios: load `/chat`, send message, assert activity panel + formatted assistant bubble.

---

## Step 4: Chat export

`GET /api/v1/chats/{id}/export` returns anonymized JSON transcript (no PHI fields).

---

## Step 5: A2A stream parity

Integration test asserting JSON-RPC streaming response shape matches chat SSE token envelope.

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M18-chat-production-readiness.md`
- `docs/WISHLIST.md`
- `grafana/dashboard.json`, `core/config/RateLimitingConfig.java`
