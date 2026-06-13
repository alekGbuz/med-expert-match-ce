# Active Context

## Current Focus

Aligning the new `.agents/memory-bank/` files with canonical documentation (`docs/ARCHITECTURE.md`, `docs/PRD.md`, `docs/decisions/`, `AGENTS.md`) to fix factual errors and structural misalignments.

## Current Milestone

The active plan is **M97** (`M97-document-rag-embedding-backfill-and-deprecation-cleanup.md`) with 4 tasks:
1. Document `medexpertmatch.documents.backfill.*` in `application.yml` — **pending**
2. Add admin endpoint for on-demand embedding backfill — **pending**
3. Remove `@Deprecated primaryChatModel()` and `LlmClientType.CHAT` — **pending**
4. Extract inline summarization prompts to `.st` files — **pending**

**Deferred:** M60 (FunctionGemma fine-tune — needs GPU capacity, stakeholder sign-off)

## Completed Recently

- **M96** — Russian route-case keywords, chat mode cleanup, response sanitizer fixes
- **M95** — Prompt simplification, ICD-10 validation, parallel description generation
- **M94** — Session ID fix in advisor context, data-sizes.csv updates
- **M93** — Production readiness: embed scheduler, backfill, 549 ITs green

## Open Questions

- When will GPU capacity become available for M60?
- Should `main` branch be synced with `develop` (~10 commits behind)?
- When should the `.agents/plans/M95-*.md` stale file be cleaned up? (Already in archive)
- Is the Document RAG pipeline (NULL embeddings) blocking any user-facing feature?

## Active Risks

- **Integration tests fail locally** — requires `./scripts/build-test-container.sh` first; not a code regression
- **Document RAG embeddings are NULL** — chunks lack vectors; backfill runs only at 2 AM; no on-demand trigger
- **`main` branch is stale** — last synced at M93; subsequent M94/M95/M96/M97 work is only on `develop`

## Next Steps

1. Finish memory-bank alignment with canonical docs
2. Implement M97 phases in order (backfill config → admin endpoint → deprecation removal → prompt extraction)
3. Run `mvn verify` to ensure green suite
4. Consider syncing `main` with `develop` after M97 completion
