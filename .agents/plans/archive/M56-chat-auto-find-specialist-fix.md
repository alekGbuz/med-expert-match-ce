# M56: Chat Auto Find Specialist Fix — ✅ Complete

**Completed:** 2026-05-31 · Branch: `feat/m56-chat-find-specialist-fix`

## Problem

AI Chat in Auto mode with a pasted Find Specialist block (case ID + Abstract containing LLM chain-of-thought) failed to return doctor matches:

| # | Symptom | Root cause |
|---|---------|------------|
| 1 | SSE stream ignored harness | `streamMessage()` always used LLM; only sync `processMessage()` routed to harness |
| 2 | Abstract stored CoT (3k+ chars) | Embedding generation prompt allowed reasoning/sandbox text |
| 3 | Safety refusal instead of match | Auto orchestrator delegated via Task/TodoWrite on clinical paste |

## Scope

| # | Deliverable | Status |
|---|-------------|--------|
| 1 | Route `MATCH_DOCTORS` / `ROUTE_CASE` + case ID in `streamMessage()` to harness (`streamViaHarnessEngine`) | ✅ |
| 2 | `ChatUserContentSanitizer` — strip CoT from pasted `Abstract:` in chat input | ✅ |
| 3 | `EmbeddingDescriptionSanitizer` — strip CoT from generated embedding abstracts | ✅ |
| 4 | Prompt rules: no reasoning in `embedding-text-generation-system.st`; direct tools in orchestrator instructions | ✅ |
| 5 | `ChatToolContextHolder.setGoalType` + deny `task`/`todo_write` for non-general goals in Auto | ✅ |
| 6 | Unit tests: stream/sync harness routing, tool scope, sanitizers | ✅ |

## Delivered

- `ChatAssistantServiceImpl` — sanitize input, harness stream path, goal type on tool context
- `ChatUserContentSanitizer`, `EmbeddingDescriptionSanitizer`
- `MedicalCaseDescriptionServiceImpl` — sanitize after LLM abstract generation
- `ChatAgentToolScope` — orchestrator delegation guard by goal
- `chat-agent-orchestrator-instructions.st`, `embedding-text-generation-system.st`
