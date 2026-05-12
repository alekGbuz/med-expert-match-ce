# Module: `retrieval`

## Purpose

**Semantic graph retrieval** (project “SGR” in the `SgrService` sense): scoring and ranking for doctor–case matching, facility routing, and related retrieval using embeddings, relational data, and graph signals as implemented in this module.

## Owned domain

- Match and scoring types (e.g. `DoctorMatch`, `ConsultationMatch`, `ScoreResult`, routing options) under `retrieval/domain`.

## Dependencies (Modulith)

Allowed: `core`, `medicalcase`, `clinicalexperience`, `doctor`, `embedding`, `evidence`, `facility`, `graph`, `medicalcoding` — treat as **read/analytics** dependencies for scoring pipelines unless a design explicitly says otherwise.

## Boundaries

- Do not fold LLM **agent orchestration** here; that stays in `llm`.
- Do not call AGE Cypher except through **`GraphService`** from `graph` (or the project’s single graph abstraction as documented in skills).

## Skills

- `.agents/skills/core-architecture/SKILL.md` — graph access patterns.
- `.agents/skills/domain-modeling/SKILL.md` — match result semantics.
