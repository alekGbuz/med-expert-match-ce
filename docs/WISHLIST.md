# Product Wishlist

Nice-to-have features and UX improvements not yet scheduled in an active milestone. Items here may graduate into `.agents/plans/M{NN}-*.md` when prioritized.

**Last updated:** 2026-05-31

---

## W-01: Real-time agent activity panel (Cursor-style) — ✅ Delivered (M17)

**Priority:** High (chat UX)  
**Status:** ✅ Delivered in **M17**  
**Related:** `static/js/chat.js`, `static/css/chat.css`, `templates/chat.html`

---

## W-02: Markdown rendering in AI chat responses — ✅ Delivered (M17 + M18)

**Priority:** High (chat UX)  
**Status:** ✅ Delivered in **M17** (client streaming) + **M18** (SSR allowlist)  
**Related:** `web/service/ChatMarkdownRenderer.java`, `static/js/chat.js`, `templates/chat.html`

### Delivered

- Client `marked` + DOMPurify for streaming tokens
- Server-side allowlist renderer for historical assistant messages
- JSON-wrapped SSE tokens preserving whitespace
- XSS + E2E smoke integration tests

---

## W-03: Playwright chat browser verification — 📋 Planned (M19)

**Priority:** Medium (CI confidence)  
**Status:** MockMvc smoke in M18; Playwright optional in **M19**  
**Related:** `.agents/plans/M19-chat-ops-and-interop.md`

---

## W-04: *(placeholder for future items)*

Add new wishlist entries as `W-{NN}` with the same structure.
