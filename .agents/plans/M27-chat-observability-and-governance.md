# M27: Chat Observability & Governance

M26 delivered A2A agent card federation hints, retention metrics with admin visibility, export bundle schema validation, ops dashboard runbook link, and scoped rate-limit metrics. M27 deepens observability and governance automation.

**Prerequisite:** M26 complete (see `.agents/plans/archive/M26-chat-federation-and-compliance.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Grafana alerts for retention purge failures and scope rate-limit spikes | `feat/retention-alerts` | ⬜ Planned | 3h |
| 2 | Admin API for retention stats (`GET /api/v1/admin/chat-retention`) | `feat/retention-admin-api` | ⬜ Planned | 2h |
| 3 | A2A agent card OpenAPI component + contract IT | `feat/agent-card-openapi` | ⬜ Planned | 3h |
| 4 | Export bundle JSON Schema file + CI validation | `feat/export-json-schema` | ⬜ Planned | 3h |
| 5 | Playwright smoke: admin retention card + runbook link | `feat/admin-playwright-m26` | ⬜ Planned | 2h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~13h (+4h optional)**

---

## Step 1: Retention & scope Grafana alerts

Add alert rules in `grafana/chat-alerts.yml` for retention run failures and A2A vs SSE rate-limit imbalance.

---

## Step 2: Retention admin API

Expose last-run snapshot and config via admin REST (modulith-safe via core query interface).

---

## Step 3: Agent card OpenAPI

Document `/.well-known/agent.json` in OpenAPI; IT asserts response matches schema.

---

## Step 4: Export JSON Schema

Publish `ChatExportBundleResponse` as standalone JSON Schema; validate in CI.

---

## Step 5: Playwright admin polish

Extend optional Playwright suite to verify retention card and runbook link on admin hub.

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M26-chat-federation-and-compliance.md`
- `ChatRetentionMetrics.java`, `AgentCardServiceImpl.java`, `grafana/chat-alerts.yml`
