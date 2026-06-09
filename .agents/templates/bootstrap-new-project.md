# Bootstrap AI Context Strategy for a NEW Project

> **Use this template when starting a brand-new repository (or a repo that has no structured AI context yet).** It produces a standard context architecture with `AGENTS.md` at the root, `.agents/skills/` as the single source of truth, and optional adapters for IDE agents.

You are an infrastructure architect for AI coding agents (Cursor, Claude Code, OpenAI/Codex, GitHub Copilot Agents, etc.). Your goal is to create the structure described in `docs/ai-context-strategy.md` for a new project.

---

## 1. Analyze the repository and domain before changing anything

> **Do this FIRST. Do not write any file before completing this analysis.**

1. **Inspect the current project structure**:
   - List top-level directories and important subdirectories.
   - Identify application modules (e.g., `core`, `api`, `infra`, `frontend`, `backend`, `shared`).

2. **Analyze relationships between modules**:
   - Which modules depend on which others?
   - Which modules are "core" (domain logic), and which are "edge" (UI, infrastructure, integration)?
   - Identify clear boundaries (e.g., domain layer vs. application layer vs. infrastructure).

3. **Analyze domain models**:
   - Find main domain entities/value objects (e.g., `Order`, `User`, `Project`, `Task`).
   - Determine which modules own which domain models.
   - Note any anti-patterns (e.g., domain leaking into UI, cross-module coupling).

4. **Summarize (must be done BEFORE any file creation/modification)**:
   - Short textual description of the architecture (layers/modules).
   - Simple text-based diagram showing modules and their relationships.
   - Table of: `module -> responsibilities -> owned domain models -> dependencies`.

Only after this analysis is done, proceed to design the AI context structure.

---

## 2. Create the baseline directories (after analysis)

Design a minimal, tool-agnostic structure for AI context:

- Create a root-level `AGENTS.md`.
- Create a `.agents/skills/` directory for skills:
  - Each skill lives under `.agents/skills/<skill-name>/SKILL.md`.
- Do **not** add IDE-specific directories yet (no `.cursor/` or others at this step).

Planned tree in text form:

```text
.
├── AGENTS.md
├── .agents/
│   ├── plans/
│   │   ├── 00-index.md
│   │   └── m-04-runtime-platform-foundations.md
│   └── skills/
│       ├── core-architecture/
│       │   └── SKILL.md
│       ├── code-style/
│       │   └── SKILL.md
│       └── testing/
│           └── SKILL.md
└── src/...
```

Use `.agents/plans/` for plans. Use `M-NN` prefix for milestone naming: `M-NN-short-topic.md` (literal `M` prefix, NN = zero-padded milestone number, e.g. `M-04-runtime-platform-foundations.md`). Index at `00-index.md`. Completed plans are archived to `/plans/archive/`.

---

## 2.5 Add layered, module-level AGENTS.md (after analysis)

After finishing the module + domain analysis, create a **layered AGENTS.md layout**:

- Keep `AGENTS.md` at the repo root as the global foundation (compact, index-oriented).
- Create **2-5 nested `AGENTS.md` files** only at **major module boundaries** (NOT in every folder).

Choose module-level paths based on the real repo structure you found (examples only):

- `services/backend/AGENTS.md`
- `services/frontend/AGENTS.md`
- `modules/domain/AGENTS.md`
- `infra/AGENTS.md`
- `packages/shared/AGENTS.md`

**Rules:**
- Create a nested `AGENTS.md` only if that module has distinct conventions, stack, workflows, or boundaries.
- Each nested `AGENTS.md` must be short (target: **< 2-4 KB**) and must **not** duplicate root content.
- Each nested `AGENTS.md` must include:
  - module purpose and responsibilities,
  - module-owned domain models (if any),
  - module-specific commands (if different),
  - module-specific constraints/boundaries,
  - pointers to relevant skills in `.agents/skills`.

**Output requirement:**
- Include the full content of the root `AGENTS.md` AND every nested `AGENTS.md` you created.
- Also output a directory tree showing where the nested files live.

---

## 3. Design the initial skill set (aligned with modules and domain)

Propose an initial set of **4-7 skills** that make sense for this project:

- `core-architecture` — system structure, layers, module boundaries, and how they relate.
- `domain-modeling` — domain entities, aggregates, invariants, and ownership rules.
- `code-style` — naming, patterns, examples of idiomatic code for the chosen stack.
- `testing` — how to write and run tests, what "good tests" look like per module.
- `dev-workflow` — branching strategy, code review, CI basics.
- `db-migrations` — how to write and apply migrations (if a DB exists).
- `api-design` — conventions for REST/RPC/GraphQL, error handling, versioning.
- `security-check` — threat modeling, input validation, secrets hygiene, dependency audit, auth/authz boundaries, OWASP-aligned review for AI-generated code.
- `write-less-code` — minimal-diff thinking, simplification, reuse-first implementation, context hygiene.

For each proposed skill, define:
- when it should be used,
- which modules and domain models it is most relevant to,
- what questions it answers,
- what it must **not** do (boundaries).

Then generate initial `SKILL.md` templates for each skill under `.agents/skills/`.

Each `SKILL.md` must include at least:

```markdown
# <Skill Name>

## Description
Short description of what this skill helps with and which modules/domain areas it covers.

## When to use
- List of situations or task types where this skill should be loaded.
- Reference specific modules or domain models where relevant.

## Instructions
- Concrete, actionable instructions for the agent.
- Include examples and "dos/don'ts" where helpful.
- Respect module boundaries and domain ownership identified in the analysis.

## Boundaries
- Things this skill must not change or decide on its own.
- Modules or domain areas it must not touch directly.
```

---

## 4. Create a root `AGENTS.md` tailored to this project

Generate an `AGENTS.md` that:

1. **Concise overview of the project**: purpose, main components/services, languages/stack.
2. **High-level architecture**: modules and their responsibilities, relationships between modules (who depends on whom and why), ownership of key domain models per module (high-level only; details go into nested module `AGENTS.md`).
3. **Project-wide rules**: coding conventions that apply everywhere, review/merge rules at a high level, risk boundaries (what must never be changed without explicit human approval).
4. **`.agents/skills` introduction**: explanation of how skills are organized, an index table of the initial skills, explicit triggers: when the agent should load which skill.
5. **Links to nested module-level `AGENTS.md` files**: short "Module guidance" section with paths to each nested `AGENTS.md`.

### Root `AGENTS.md` size + structure constraints (mandatory)

The root `AGENTS.md` must be **compact and index-oriented**:

- Target size: **<= 150 lines** (hard preference) and **<= 10-15 KB** (hard limit).
- Use bullets + short paragraphs only; avoid long prose.
- Do not paste large specs or docs into root `AGENTS.md`.
- Prefer **links and file paths** to canonical docs/code examples (e.g., `docs/...`, `<module>/AGENTS.md`, `.agents/skills/.../SKILL.md`).
- Put module-specific conventions into **nested module-level `AGENTS.md`** (2-5 files only).
- Put deep workflows/knowledge into `.agents/skills` and reference them from root via a Skills Index + explicit triggers.
- Root must include only essentials:
  - What the repo is (1-2 sentences)
  - Repo map (short tree / bullets)
  - Commands (build/test/lint)
  - Global boundaries (✅/⚠️/🚫)
  - Links: nested `AGENTS.md` + Skills Index

---

## 5. Prepare for IDE/agent adapters (but keep them optional)

Do **not** assume any specific IDE yet, but design the structure so it is easy to adapt later:

- The only canonical definitions of skills live in `.agents/skills/**/SKILL.md`.
- Any future adapter (for Cursor, Claude Code, etc.) should:
  - either read these files directly, or
  - generate its own config/skills by transforming `.agents/skills`.

Create a short architecture note as `docs/ai-context-strategy.md` that explains:
- the layer model (root `AGENTS.md` -> nested module `AGENTS.md` -> skills in `.agents/skills` -> optional adapters),
- how the module and domain model analysis feeds into this structure,
- how new skills should be added,
- how existing skills should be updated,
- how to keep everything in sync across tools.

---

## 6. Write Less Code — Simplicity in the AI Era

Writing less code was always a mark of mastery. In the AI agent era, it has become an undervalued virtue — a shield that now takes more effort to hold. Chaos spreads on its own. Order does not — it must be defended. And with an agent that machine-guns thousands of lines of code, this work is harder than ever.

### Add this skill definition

```yaml
---
name: write-less-code
description: Write less code and simplify — minimalism principles for the AI coding era
version: "1.0"
tags:
  - minimalism
  - simplification
  - quality
---
```

Then create `.agents/skills/write-less-code/SKILL.md` with the following content:

```markdown
# Write Less Code — Simplicity in the AI Era

Writing less code was always a mark of mastery. In the AI agent era, it has become an undervalued virtue — a shield that now takes more effort to hold. Chaos spreads on its own. Order does not — it must be defended. And with an agent that machine-guns thousands of lines of code, this work is harder than ever.

## Why Minimalism Matters More Now

**Writing code became cheap; review became expensive.**
AI-generated code often increases review cost, readability issues, and maintenance burden.

**Less code = fewer problems.**
Less to support, less to read, less to debug, less to secure, and less to migrate.

**AI loves reinventing the wheel.**
Agents frequently generate custom solutions where a mature library or a small extension to existing code would be enough.

## How to Apply This

### Produce Minimum Diff

Before writing code, ask:
- Is this a one-liner?
- Can I solve this by modifying an existing function instead of adding a new one?
- Is there a library that already does 80% of this?
- Can I delete code instead of adding code?

### Use the `/simplify` Command

After implementation and before commit, run a simplification pass:
- reduce LoC,
- remove dead code,
- merge near-duplicate logic,
- flatten unnecessary abstraction,
- keep behavior unchanged.

### OSS Research Is Part of Coding

Always ask: **Is there a battle-tested library for this?**
A short dependency evaluation is often cheaper than custom implementation.

### Audit Your Context Files

For every rule in `AGENTS.md`, nested `AGENTS.md`, or skill files, ask:
- What concrete failure does this rule prevent?
- Is that failure still relevant?
- If not, delete or simplify the rule.

### Keep Context Clean

- Use sub-agents for focused sub-tasks when supported.
- Tag only relevant files/docs.
- Prefer short, high-signal specs over broad context dumps.

### Minimalism in Specs

Write specs that:
- fit on one screen,
- explain **what** and **why**,
- avoid over-specifying **how** unless necessary,
- stay current as decisions evolve.

### Minimalism in Features

Before adding a feature, verify:
- Is there an actual user need now?
- What is the maintenance cost?
- Does it increase domain complexity?
- Can the same value be achieved with a simpler change?

## The Core Belief

Simplicity in code does not come from a markdown rule alone — it comes from disciplined engineering judgment. Care about every line. Push back on bloat. Defend the order.
```

---

## 6.5 Add a security review skill

Add this skill to the initial set and generate `.agents/skills/security-check/SKILL.md`.

```yaml
---
name: security-check
description: Security review agent for AI-generated and human-written code
version: "1.0"
tags:
  - security
  - owasp
  - auth
  - secrets
  - dependencies
---
```

The skill must cover:
- secrets hygiene,
- authentication and authorization checks,
- input validation and injection prevention,
- dependency audit,
- infrastructure and CI/CD security review,
- OWASP-aligned quick review,
- blocking criteria for critical/high findings.

It must explicitly state:
- **do not auto-fix vulnerabilities**,
- **do not approve PRs**,
- escalate critical issues to humans,
- run both **before implementation** for risky work and **after implementation** before commit.

---

## 7. Output

At the end, you must output:

1. **The analyzed architecture summary**:
   - list of modules,
   - relationships between modules,
   - main domain models and their owning modules.

2. **The proposed directory tree**, including:
   - root `AGENTS.md` (should be compact),
   - all nested module-level `AGENTS.md` files,
   - `.agents/skills/**/SKILL.md`,
   - `docs/ai-context-strategy.md`.

3. **The full content for**:
   - `AGENTS.md` (root),
   - each nested module-level `AGENTS.md` you created,
   - each initial `.agents/skills/**/SKILL.md`,
   - `docs/ai-context-strategy.md`.

All filenames and paths must be correct and ready to be created in a real repository. Use clear, idiomatic English in all generated files.

---

## TDD Workflow (mandatory)

Always use TDD. Before implementing any functionality:

1. **Write the test first** — before any implementation code.
2. **Verify the test against the requirements** — use an internal review tool/skill (e.g. a code-review or testing skill, or a review subagent) to confirm the test truly encodes the requirement.
3. **Run security check before implementation** — load the `security-check` skill for any task touching auth, APIs, DB, secrets, external input, infrastructure, or new dependencies. Report risks before coding.
4. **Implement** the functionality — only after the test is written, verified, and security pre-check is complete.
5. **Re-run the test** (`mvn verify` or equivalent) — fix problems and iterate until it passes.
6. **Run security check again before commit** — review the final diff for secrets, missing auth, injection risks, vulnerable dependencies, or unsafe config.

---

## How to use this template

1. **Copy this file** into a new empty repository at `.agents/templates/bootstrap-new-project.md` (or wherever the team's convention is).
2. **Ask an AI agent (or the team lead)** to follow it step by step.
3. **Verify** the output before committing:
   - Does `AGENTS.md` exist and is it **< 150 lines / < 15 KB**?
   - Are there exactly **2-5 nested module-level `AGENTS.md`** files at the right module boundaries?
   - Do **all** the skills have a `SKILL.md` with the 4 required sections (Description, When to use, Instructions, Boundaries)?
   - Does `docs/ai-context-strategy.md` exist and explain how to add/update skills?
4. **Commit** with message: `chore: bootstrap AI context strategy (AGENTS.md + .agents/skills + adapters prep)`.
5. **Run the team for 1-2 weeks** and check whether the skills actually load when expected. Refine triggers as needed.

## See also

- `docs/ai-context-strategy.md` (the project's own strategy doc) for the layer model rationale
- `.agents/skills/` for examples of well-written `SKILL.md` files
- `.agents/plans/M{NN}-*.md` for examples of milestone plans that follow the `m-NN` naming convention
