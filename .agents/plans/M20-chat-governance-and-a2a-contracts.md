# M20: Chat Governance & A2A Contract Hardening

M19 delivered per-user chat rate limits, Grafana chat panels, PHI-safe export, A2A SSE stream parity, and Playwright profile documentation. M20 strengthens governance (tier-aware limits, audit), expands A2A contract coverage, and adds full Playwright navigation when desired.

**Prerequisite:** M19 complete (see `.agents/plans/archive/M19-chat-ops-and-interop.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Tier-aware chat limits via `ApiSessionToken.rateLimitTier` | `feat/chat-tier-rate-limits` | ⬜ Planned | 3h |
| 2 | Chat export audit log (hashed user/chat id, no PHI) | `feat/chat-export-audit` | ⬜ Planned | 2h |
| 3 | OpenAPI + JSON schema for A2A stream/jsonrpc responses | `feat/a2a-openapi-schemas` | ⬜ Planned | 3h |
| 4 | Full Playwright chat navigation test (optional CI) | `feat/chat-playwright-full` | ⬜ Planned | 4h |
| 5 | Rate-limit integration with Retry-After response headers | `feat/chat-rate-limit-headers` | ⬜ Planned | 2h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~14h (+4h optional)**

---

## Step 1: Tier-aware chat limits

Resolve `RateLimitTier` from authenticated API session (when present) or default for header-based web users.

Wire `DEFAULT` / `HIGH` / `UNLIMITED` to `ChatRateLimitService.tryAcquire(userId, tier)`.

---

## Step 2: Export audit

Log export events with hashed identifiers; metric `chat.export.count`.

---

## Step 3: A2A OpenAPI schemas

Document `/a2a/v1/jsonrpc`, `/a2a/v1/stream`, and response envelopes in springdoc.

Contract tests for error codes and stream token shape.

---

## Step 4: Playwright full smoke

Extend `ChatPlaywrightSmokeTest` to load `/chat`, send a message, assert activity panel + assistant bubble (local/optional CI).

---

## Step 5: Retry-After headers

Return `Retry-After` on 429 from chat rate limiter (align with global IP limiter).

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M19-chat-ops-and-interop.md`
- `docs/WISHLIST.md` W-03
- `ChatRateLimitService.java`, `ApiSessionToken.java`
