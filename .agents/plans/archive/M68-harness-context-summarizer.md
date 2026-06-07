# M68: Harness Context Summarizer (M64 Phase 3)

**Status:** Archived (2026-06-08) — Phases 1–5 implemented  
**Created:** 2026-06-07  
**Depends on:** M67 (archived) — clinical/utility endpoint separation

## Problem Statement

FULL-tier harness still passes verbose tool/GraphRAG payloads toward `clinicalChatModel`. M67 split endpoints but
not payload size. MedGemma should receive structured summaries, not raw PubMed lists or full doctor candidate dumps.

## Goal

Code-first `HarnessContextSummarizer` shapes harness output into compact structured JSON before T3 clinical interpretation.

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | Whitelist contract | `HarnessContextWhitelist` preserved fields | **Done** |
| 2 | Deterministic summarizers | Doctor/evidence/routing/generic summarizers | **Done** |
| 3 | Wire into harness engines | `MedicalAgentLlmSupportServiceImpl` interpretation + routing | **Done** |
| 4 | Regression tests | `context-summarizer-cases.jsonl` + `HarnessContextSummarizerEvalTest` | **Done** |
| 5 | Docs | `docs/HARNESS.md`, `docs/eval/cost-model.md`, M64 ADR Phase 3 | **Done** |

## Acceptance criteria

- [x] Clinical prompts never include raw full candidate lists when structured summary suffices
- [x] Verify/policy fields preserved in 100% of eval scenarios (`context-summarizer-cases.jsonl`)
- [x] `mvn verify` green; harness ITs unchanged behavior on synthetic cases
- [ ] Prometheus token metrics show reduced input size (deferred — `llm.tokens.total` wiring pending)

## Artifacts

| Artifact | Location |
|----------|----------|
| Interface | `llm/harness/HarnessContextSummarizer.java` |
| Impl | `llm/harness/HarnessContextSummarizerImpl.java` |
| Whitelist | `llm/harness/HarnessContextWhitelist.java` |
| Eval JSONL | `src/test/resources/eval/context-summarizer-cases.jsonl` |
| Docs | `docs/HARNESS.md` — Context summarizer; `docs/eval/cost-model.md` |

## Follow-up

M64 ADR Phases 4–6 remain backlog: draft-and-refine, cache tuning, retry-aware execution state.

## References

- M64 ADR: `docs/decisions/M64-cost-quality-tier-routing.md`
- M67: `archive/M67-llm-role-endpoint-separation.md`
