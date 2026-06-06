# M59: Chat Collapsible Reasoning and Follow-Up Routing

**Status:** Implemented

## Goal

Stop MedGemma chain-of-thought from appearing as the main chat answer; route elaboration follow-ups to case analysis instead of re-running doctor match; improve chat UX.

## Delivered

1. **Follow-up goal routing** — `provide more details`, `?`, Russian `подробнее` → `ANALYZE_CASE` after `MATCH_DOCTORS` (`GoalClassifier`, `GoalIntentPatterns`).
2. **Reasoning split** — `LlmResponseSanitizer.splitReasoningFromResponse` / `formatForChatDisplay`: detect `Strategizing complete`, numbered sections, skip checklist false positives.
3. **Collapsible UI** — Client-side `renderAssistantContent()` wraps planning in `<details class="llm-thinking">`; green **Response** label; history re-rendered from raw content on page load.
4. **Case analysis interpretation** — Dedicated `medgemma-case-analysis-interpretation-*.st` prompts and `interpretCaseAnalysisWithMedGemma()` (separate cache namespace).
5. **Chat UX** — Flat sidebar list, icon header actions, delete-all/default chat fix, Documents nav hidden.
6. **Docs** — Removed inline milestone tags from user-facing docs.

## Verification

- `MedicalAgentServiceImplTest` — sanitizer/split tests
- `ChatMarkdownRendererTest` — SSR reasoning wrap
- `GoalClassifierFollowUpTest` — elaboration routing
- `ChatServiceDeleteChatTest` — delete default chat
