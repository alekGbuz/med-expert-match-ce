# M92: Session Memory Compaction and Document RAG Pipeline

**Status:** Active (planned 2026-06-10)
**Created:** 2026-06-10
**Depends on:** M91 (archived — clinicalExperienceMs wired, @EnableScheduling deduped)

## Problem Statement

Two architectural gaps prevent the system from reaching production quality for long-running agent workflows and local evidence retrieval:

1. **Session Memory Compaction not wired**: `docs/AGENTIC_PATTERNS_IMPROVEMENTS.md` marks the Session API as Partial ("deps present, advisor not wired"). The `spring-ai-session-jdbc` dependency is in `pom.xml`, `application.yml` configures `agent.session.max-turns: 20` and `agent.session.max-tokens: 4000`, but `SessionMemoryAdvisor` is never constructed or registered on the chat client. Long workflows (queue prioritization, multi-step case analysis) grow unbounded context, risking token overflow.

2. **Document chunk embeddings are NULL**: The `documents/` and `chunking/` modules were created, but `docs/improvements-from-docu-rag.md` explicitly states chunk embeddings are NULL. Without embeddings, vector search on `document_chunk` is not possible. The RAG loop is not closed — ingested documents cannot actually be retrieved or used by the agent.

## Goal

1. Wire `SessionMemoryAdvisor` with `TurnCountTrigger` + `TokenCountTrigger` into `MedicalAgentConfiguration`, register as default advisor on the chat client.
2. Build chunk embedding pipeline: batch-embed chunks with NULL embedding via `EmbeddingService`.
3. Implement `DocumentSearchService` with PgVector similarity search on `document_chunk.embedding`.
4. Wire local document search into the `evidence-retriever` skill so the agent queries local documents alongside PubMed.
5. Add IT for session compaction (>20 turns → compaction fires).
6. Add IT for document search (ingest → embed → search → results).

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | TDD: write `SessionCompactionIT` (red) | Pending |
| 2 | Wire `SessionMemoryAdvisor` + `CompositeCompactionTrigger` in `MedicalAgentConfiguration` | Pending |
| 3 | TDD: write `DocumentSearchServiceTest` (red) | Pending |
| 4 | Implement `DocumentSearchService` with PgVector similarity search | Pending |
| 5 | Build chunk embedding pipeline in `DocumentServiceImpl` (batch-embed NULL chunks) | Pending |
| 6 | Wire `DocumentSearchService` into `evidence-retriever` skill | Pending |
| 7 | `mvn verify` green | Pending |
| 8 | Archive this plan | Pending |

## Acceptance Criteria

- [ ] Session compaction fires after >20 turns in a chat session (IT proves it)
- [ ] Document chunks with NULL embedding get batch-embedded on ingest
- [ ] `DocumentSearchService.search(query)` returns relevant chunks via PgVector COSINE_DISTANCE
- [ ] Evidence retriever returns local document hits alongside PubMed results
- [ ] `mvn verify` exits 0

## References

- `docs/AGENTIC_PATTERNS_IMPROVEMENTS.md:36,127-139` — Session API partial status
- `docs/improvements-from-docu-rag.md:287-294` — Remaining RAG work
- `src/main/resources/application.yml:15-19` — session config
- `src/main/java/.../documents/` — document module
- `src/main/java/.../chunking/` — chunking module
- `src/main/java/.../embedding/service/EmbeddingService.java` — embedding service