# M24: A2A Production Readiness

M23 delivered A2A stream rate limits, export-bundle audit with admin filters, admin cookie auth, Grafana tier panels, and chat lifecycle OpenAPI. M24 focuses on production hardening and operator UX.

**Prerequisite:** M23 complete (see `.agents/plans/archive/M23-chat-security-and-a2a-governance.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | A2A JSON-RPC rate limiting (non-stream parity) | `feat/a2a-jsonrpc-rate-limits` | ⬜ Planned | 2h |
| 2 | Admin audit UI for chat exports (Thymeleaf) | `feat/admin-export-audit-ui` | ⬜ Planned | 3h |
| 3 | Chat lifecycle web UI (export bundle + delete data) | `feat/chat-lifecycle-ui` | ⬜ Planned | 4h |
| 4 | Alert rules: tier-specific rate-limit panels in `grafana/chat-alerts.yml` | `feat/tier-alert-rules` | ⬜ Planned | 2h |
| 5 | Session token expiry warnings in admin UI | `feat/token-expiry-ui` | ⬜ Planned | 2h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~13h (+4h optional)**

---

## Step 1: A2A JSON-RPC rate limits

Apply shared limiter to `/a2a/v1/jsonrpc` sendMessage path with 429 + Retry-After.

---

## Step 2: Admin export audit UI

Thymeleaf page `/admin/chat-exports?user=admin` with action filter dropdown.

---

## Step 3: Chat lifecycle web UI

User-facing controls on chat page for export bundle download and delete-all confirmation modal.

---

## Step 4: Tier alert rules

Extend `grafana/chat-alerts.yml` with per-tier rate-limit spike rules.

---

## Step 5: Token expiry warnings

Highlight expiring session tokens in admin UI (7-day window).

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M23-chat-security-and-a2a-governance.md`
- `A2aJsonRpcController.java`, `AdminController.java`, `ChatDataLifecycleService.java`
