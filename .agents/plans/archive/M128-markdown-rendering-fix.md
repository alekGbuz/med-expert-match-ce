# M128: Fix Markdown Rendering in Chat

**Status:** Archived (2026-06-19)
**Created:** 2026-06-19

## Problem Statement

Markdown formatting in chat assistant responses was not rendering correctly:
- Bold (`**text**`) and italic (`*text*`) appeared as literal `&ast;&ast;text&ast;&ast;` instead of `<strong>`/`<em>` tags
- Content appearing before the `<details>` reasoning block was never converted from markdown
- Chat message bubbles had inconsistent max-width constraints

## Root Cause

`ChatMarkdownRendererImpl.convertMarkdown()` called `escapeHtml()` **before** `applyInlineFormatting()`, so the bold/italic regex patterns never matched the escaped input. Additionally, `renderPreparedAssistant()` only converted markdown after `</details>`, ignoring content before the reasoning block.

## Changes

| File | Change |
|------|--------|
| `ChatMarkdownRendererImpl.java` | Reordered pipeline: inline formatting → placeholder tokens → HTML escape → restore placeholders. Fixed `renderPreparedAssistant()` to handle content before `<details>` block |
| `chat.css` | Unified message bubble `max-width: 90%` for both user and assistant rows |
| `.agents/plans/M77-stories.json` | Deleted (unused) |
| `.agents/plans/archive/M79-ralph-loop-pilot-m77.md` | Removed all references to M77-stories.json |
| `.agents/plans/archive/M90-implement-m77-feature.md` | Removed all references to M77-stories.json |
| `.agents/memory-bank/activeContext.md` | Updated current focus |
| `.agents/memory-bank/progress.md` | Added M128 entry |

## Verification

- `curl` confirms `<strong>` tags render for `**bold**` markdown
- `<ul><li>` renders for `- item` lists
- Content before `<details>` reasoning block is now converted to markdown
- All existing tests pass
