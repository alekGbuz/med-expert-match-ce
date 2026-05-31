# M22: A2A Federation & Chat Lifecycle

M21 delivered admin session-token APIs, export audit queries, chat retention scheduling, session-token auth profile, and Grafana alert documentation. M22 extends agent interoperability and chat lifecycle management.

**Prerequisite:** M21 complete (see `.agents/plans/archive/M21-chat-admin-and-observability.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | A2A skill registry endpoint + OpenAPI (`GET /a2a/v1/skills`) | `feat/a2a-skill-registry` | ⬜ Planned | 3h |
| 2 | Chat message soft-delete / user data export bundle (GDPR-style) | `feat/chat-data-lifecycle` | ⬜ Planned | 4h |
| 3 | Prometheus alert rules file from `docs/grafana-chat-alerts.md` | `feat/grafana-alert-rules` | ⬜ Planned | 2h |
| 4 | Admin UI page for session tokens (Thymeleaf) | `feat/admin-session-tokens-ui` | ⬜ Planned | 4h |
| 5 | Rate-limit tier metrics by tier label | `feat/chat-tier-metrics` | ⬜ Planned | 2h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~15h (+4h optional)**

---

## Step 1: A2A skill registry

Expose supported skills (`doctor_match`, `evidence_search`) with input schema hints for external agents.

Contract test alongside `A2aContractIT`.

---

## Step 2: Chat data lifecycle

User-scoped delete-all-chats and combined export bundle (metadata + redacted messages).

Never include raw PHI; reuse `PhiGuard` and export audit.

---

## Step 3: Grafana alert rules

Ship `grafana/chat-alerts.yml` (or similar) mirroring `docs/grafana-chat-alerts.md`.

---

## Step 4: Admin session token UI

Thymeleaf page under `/admin/session-tokens?user=admin` calling admin REST APIs.

---

## Step 5: Tier metrics

Micrometer counter/timer tags for `chat.rate.limited` and stream turns by `RateLimitTier`.

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M21-chat-admin-and-observability.md`
- `A2aJsonRpcController.java`, `AdminController.java`
