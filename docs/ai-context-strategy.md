# AI context strategy (MedExpertMatch)

This document describes how coding agents should load context for this repository. It is tool-agnostic: Cursor, Claude Code, Copilot Agents, or others should treat these paths as canonical.

## Layer model

1. **Root `AGENTS.md`** (repository root)  
   Compact index: purpose, repo map, essential commands, global boundaries, links to nested guides and skills. Target: short enough to scan in under a minute.

2. **Nested `AGENTS.md`** (2–5 files under major Java module packages)  
   Module-specific stack slices, boundaries, and commands that differ from the rest of the app. They do not repeat the full stack documentation.

3. **`.agents/skills/<name>/SKILL.md`** (single source of truth for deep guidance)  
   Procedures, conventions, and examples that would bloat root or module files. Agents should load the relevant skill when the task matches its trigger list.

4. **Optional IDE adapters** (e.g. `.cursor/rules`, MCP, vendor-specific config)  
   Not required for correctness. If added later, they should **reference or generate from** `.agents/skills` instead of duplicating long rules.

## How module and domain analysis feeds the layout

Spring Modulith `@ApplicationModule` annotations in each module’s `package-info.java` define **allowed dependencies**. That graph drives:

- Where nested `AGENTS.md` files are placed (edge vs orchestration vs core vs retrieval).
- Which skills mention which modules and what cross-module work is allowed.
- Warnings in skills when touching `core` (shared infrastructure intentionally used broadly).

Domain entities live under `*/domain/` per module (e.g. `MedicalCase`, `Doctor`, `ConsultationMatch`). Skills reference **ownership**: only the owning module should define persistence and REST for that aggregate unless an orchestration module coordinates reads.

## Adding a new skill

1. Create `.agents/skills/<skill-name>/SKILL.md` using the standard sections: Description, When to use, Instructions, Boundaries.
2. Add a row to the Skills Index table in root `AGENTS.md`.
3. If the skill is module-specific, add one line under the relevant nested `AGENTS.md` “Skills” section pointing to it.

## Updating skills

- Prefer editing `.agents/skills/.../SKILL.md` over growing root `AGENTS.md`.
- When behavior changes (e.g. new integration test rule), update the skill and, if needed, one line in root or nested index.

## Keeping tools in sync

- **Canonical**: `.agents/skills/**/SKILL.md`, nested `AGENTS.md`, root `AGENTS.md`, this file.
- **Generated or mirrored configs**: document in the adapter’s README or comment that the source of truth is `.agents/skills` and give the generation command if any.

## Related project docs

- `docs/` for human-facing architecture and setup guides (English only).
- `src/main/resources/api/openapi.yaml` for HTTP contract.
