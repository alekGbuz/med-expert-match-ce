# M18: Chat Production Readiness & Observability

M17 delivered rich SSE activity events, activity panel collapse summary, JDBC session compaction IT, chat UX hardening ITs, and A2A JSON-RPC contract/error tests. M18 focuses on production readiness: server-side markdown safety, chat metrics, E2E smoke coverage, and A2A AgentCard discovery.

**Prerequisite:** M17 complete (see `.agents/plans/archive/M17-chat-agent-polish-and-a2a-hardening.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Server-side CommonMark allowlist for SSR assistant messages (W-02) | `feat/chat-ssr-markdown-sanitize` | ⬜ Planned | 3h |
| 2 | Chat turn metrics (latency, token count, tool-call count) | `feat/chat-turn-metrics` | ⬜ Planned | 3h |
| 3 | A2A AgentCard `GET /.well-known/agent.json` discovery endpoint | `feat/a2a-agent-card` | ⬜ Planned | 2h |
| 4 | Playwright / browser IT smoke for chat send + panel collapse | `feat/chat-e2e-smoke` | ⬜ Planned | 4h |
| 5 | Session compaction observability (log + health indicator) | `feat/session-compaction-observability` | ⬜ Planned | 2h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~14h (+4h optional)**

---

## Step 1: Server-side Markdown allowlist (W-02)

Render assistant history through a server-side CommonMark → HTML sanitizer (allowlist: p, strong, em, code, pre, ul, ol, li, a[href=http/https]).

Keep client `marked` + DOMPurify for streaming; SSR path becomes defense-in-depth.

**Acceptance:** `ChatAgenticUxIT` asserts no raw `javascript:` in rendered HTML even if client JS is disabled.

---

## Step 2: Chat turn metrics

Micrometer counters/timers for:

- `chat.turn.duration` — SSE stream wall time
- `chat.turn.tool_calls` — per session turn
- `chat.stream.errors` — stream failures

Expose via existing Actuator/prometheus endpoint.

---

## Step 3: A2A AgentCard discovery

`GET /.well-known/agent.json` listing `doctor_match` and `evidence_search` skills with JSON-RPC endpoint URL.

Contract test in `A2aAgentCardIT`.

---

## Step 4: Browser E2E smoke

Minimal Playwright test (or cursor-ide-browser IT):

1. Load `/chat`
2. Send message
3. Assert activity panel visible, collapses on `done`
4. Assert assistant bubble renders markdown

Skip if CI lacks browser; document local-only runner in `testing` skill.

---

## Step 5: Session compaction observability

- INFO log when compaction fires (session id hash only, no message content)
- Health indicator: last compaction timestamp / failure count

---

## Step 6: Optional A2A autoconfigure migration

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml` per AGENTS.md.

Skip if custom implementation continues to meet interoperability needs.

---

## References

- `docs/WISHLIST.md` W-02 server-side path
- `.agents/plans/archive/M17-chat-agent-polish-and-a2a-hardening.md`
- `llm/rest/A2aJsonRpcController.java`, `web/controller/ChatWebController.java`
