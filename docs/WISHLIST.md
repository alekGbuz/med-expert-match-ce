# Product Wishlist

Nice-to-have features and UX improvements not yet scheduled in an active milestone. Items here may graduate into `.agents/plans/M{NN}-*.md` when prioritized.

**Last updated:** 2026-05-30

---

## W-01: Real-time agent activity panel (Cursor-style) — ✅ Delivered (M16)

**Priority:** High (chat UX)  
**Status:** ✅ Delivered in **M16** — polish deferred to **`.agents/plans/M17-chat-agent-polish-and-a2a-hardening.md`**  
**Related:** `static/js/chat.js`, `static/css/chat.css`, `templates/chat.html`

### Delivered (M16)

- Agents column with live SSE `agent_start` / `agent_done` events
- Inline assistant token streaming (no full page reload)
- Collapsible activity panel with execution trace

### Remaining polish (M17)

- Rich `tool_call`, `reasoning`, and inline todo SSE events
- Collapse summary row (agents · steps · duration)
- `ChatAgenticUxIT` panel lifecycle assertions

---

## W-02: Markdown rendering in AI chat responses — ✅ Delivered (M16)

**Priority:** High (chat UX)  
**Status:** ✅ Delivered in **M16** — XSS IT deferred to **M17**  
**Related:** `templates/chat.html`, `static/js/chat.js`

### Delivered (M16)

- `marked` + DOMPurify client-side rendering
- `data-markdown` for historical assistant messages
- Streaming-safe re-render during SSE

### Remaining polish (M17)

- Automated XSS integration test
- Optional server-side CommonMark allowlist for SSR

---

## W-03: *(placeholder for future items)*

Add new wishlist entries as `W-{NN}` with the same structure.
