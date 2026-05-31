# M25: Chat Platform Hardening

M24 delivered A2A JSON-RPC rate limits, admin export audit UI, chat lifecycle web controls, tier alert rules, and session token expiry warnings. M25 strengthens platform reliability and operator workflows.

**Prerequisite:** M24 complete (see `.agents/plans/archive/M24-a2a-production-readiness.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Separate A2A vs chat SSE rate-limit buckets | `feat/a2a-rate-buckets` | ⬜ Planned | 3h |
| 2 | Admin dashboard landing page (`/admin?user=admin`) | `feat/admin-dashboard` | ⬜ Planned | 3h |
| 3 | Chat export bundle audit in lifecycle UI feedback toast | `feat/lifecycle-audit-feedback` | ⬜ Planned | 2h |
| 4 | Playwright smoke: admin session tokens + chat lifecycle | `feat/playwright-admin-smoke` | ⬜ Planned | 4h |
| 5 | Docs: operator runbook for chat/A2A governance | `feat/chat-ops-runbook` | ⬜ Planned | 2h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~14h (+4h optional)**

---

## Step 1: Separate rate buckets

Use distinct bucket keys for `/a2a/*` vs `/api/v1/chats/*/stream` so federated agents do not exhaust user chat quota.

---

## Step 2: Admin dashboard

Single Thymeleaf hub linking session tokens, chat exports, synthetic data, and graph visualization.

---

## Step 3: Lifecycle audit feedback

After export/delete, show non-blocking confirmation referencing hashed audit id (no PHI).

---

## Step 4: Playwright admin smoke

Optional profile tests for admin pages and chat lifecycle buttons (behind `-Pplaywright`).

---

## Step 5: Operator runbook

Add `docs/chat-ops-runbook.md` covering rate limits, retention, exports, and audit queries.

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M24-a2a-production-readiness.md`
- `A2aJsonRpcController.java`, `ChatExportsWebController.java`, `chat.js`
