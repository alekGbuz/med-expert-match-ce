# Product Wishlist

Nice-to-have features and UX improvements not yet scheduled in an active milestone. Items here may graduate into `.agents/plans/M{NN}-*.md` when prioritized.

**Last updated:** 2026-05-31

---

## W-01: Real-time agent activity panel (Cursor-style) — ✅ Delivered (M17)

**Priority:** High (chat UX)  
**Status:** ✅ Delivered in **M17**  
**Related:** `static/js/chat.js`, `static/css/chat.css`, `templates/chat.html`

### Delivered

- Agents column with live SSE `agent_start` / `agent_done` events
- Rich `tool_call`, `reasoning`, and inline `todo_update` SSE activity events
- Collapse summary row (agents · steps · duration) with chevron expand
- `ChatAgenticUxIT` panel lifecycle assertions

---

## W-02: Markdown rendering in AI chat responses — ✅ Delivered (M17)

**Priority:** High (chat UX)  
**Status:** ✅ Delivered in **M17** (client); server-side allowlist deferred to **M18**  
**Related:** `templates/chat.html`, `static/js/chat.js`

### Delivered

- `marked` + DOMPurify client-side rendering
- `data-markdown` for historical assistant messages
- Streaming-safe re-render during SSE
- Automated XSS integration test (`ChatAgenticUxIT`)

### Remaining (M18)

- Optional server-side CommonMark allowlist for SSR

---

## W-03: *(placeholder for future items)*

Add new wishlist entries as `W-{NN}` with the same structure.
