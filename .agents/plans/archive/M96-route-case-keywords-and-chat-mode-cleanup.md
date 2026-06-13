# M96: Route-Case Keywords, Chat Mode Cleanup, and Response Sanitizer Fixes

**Status:** Complete (implemented 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M95 (archived)

## Problem Statement

Three unaddressed issues remained after M95:

1. **Chat mode selector still visible** — "Quick question" mode was redundant now that Expert match (harness) was the only supported mode. The selector UI and cost hint added visual clutter.
2. **Russian-language route-case keywords missing** — `GoalIntentPatterns` had English-only route-case keyword detection. Russian-speaking users couldn't use natural-language route/redirect queries.
3. **LLM response sanitizer incomplete** — Tool results were echoed back to users as raw JSON; markdown headers weren't detected as structured output; JSON code blocks weren't stripped from LLM responses.

## Goal

1. Remove Quick question chat mode — always use Expert match (harness).
2. Fix LLM response sanitizer — strip tool results echo, detect markdown headers, remove JSON code block extraction.
3. Add Russian route-case keyword patterns to GoalClassifier.
4. Add 'find case information' and 'case details' to ANALYZE_CASE keyword patterns.
5. Clean up chat.html — remove chat-mode-group badge and cost hint.

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Remove Quick question chat mode — always Expert match | Complete |
| 2 | Fix LLM response sanitizer — strip echo, headers, JSON blocks | Complete |
| 3 | Add 'find case information'/'case details' to ANALYZE_CASE | Complete |
| 4 | Add Russian route-case keyword patterns | Complete |
| 5 | Remove chat-mode-group badge + cost hint from chat.html | Complete |
| 6 | mvn compile green | Complete |
| 7 | Archive plan | Complete |

## Acceptance Criteria

- [x] Chat template has no chat mode selector (always hidden `expert_match`)
- [x] `GoalIntentPatterns` detects Russian route-case keywords (в какую клинику, куда отправить/направить, etc.)
- [x] `GoalClassifier` checks route-case keywords before elaboration follow-up
- [x] `ResponseSanitizer` strips tool results echo in markdown format
- [x] `ResponseSanitizer` detects markdown headers as structured output
- [x] `ResponseSanitizer` removes JSON code block extraction blocks
- [x] `mvn compile` exits 0

## References

- `src/main/java/.../llm/chat/GoalIntentPatterns.java` — added `ROUTE_CASE_KEYWORDS_RU` pattern, updated `matchesRouteCaseKeywords()`
- `src/main/java/.../llm/chat/GoalClassifier.java` — added route-case check before elaboration follow-up
- `src/main/java/.../llm/chat/ResponseSanitizer.java` — strip tool results echo, header detection, JSON block removal
- `src/main/resources/templates/chat.html` — removed chat-mode-group div
