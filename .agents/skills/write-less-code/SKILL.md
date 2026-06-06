---
name: write-less-code
description: Write less code and simplify — minimalism principles for the AI coding era
version: "1.0"
tags:
  - minimalism
  - simplification
  - quality
---

# Write Less Code — Simplicity in the AI Era

Writing less code was always a mark of mastery. In the AI agent era, it has become an undervalued virtue — a shield that now takes more effort to hold. Chaos spreads on its own. Order does not — it must be defended. And with an agent that machine-guns thousands of lines of code, this work is harder than ever.

## When to use

- Before starting any non-trivial implementation — to evaluate whether the change is needed at all
- After implementation and before commit/PR — to run a simplification pass
- During code review — to push back on bloat and near-duplicate logic
- When auditing AI-generated diffs that look larger than the requirement
- When evaluating new dependencies — to confirm a battle-tested library beats custom code

This skill is project-wide; it applies to every module in `medexpertmatch/`.

## Why Minimalism Matters More Now

**Writing code became cheap; review became expensive.**
AI-generated code often increases review cost, readability issues, and maintenance burden.

**Less code = fewer problems.**
Less to support, less to read, less to debug, less to secure, and less to migrate.

**AI loves reinventing the wheel.**
Agents frequently generate custom solutions where a mature library or a small extension to existing code would be enough.

## Instructions

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

## Boundaries

- Must not override `code-style/SKILL.md` formatting/naming rules.
- Must not bypass mandatory patterns in `core-architecture/SKILL.md` (interface + impl, package-info `allowedDependencies`, Modulith boundaries).
- Must not weaken required tests in `testing/SKILL.md` (TDD is still mandatory).
- Must not remove HIPAA/PHI protections, error handling, or auth checks "for simplicity".
- Must not delete existing functionality or modules without an explicit plan in `.agents/plans/M{NN}-*.md`.
- Must not bypass the "Forbidden without explicit human approval" list in the root `AGENTS.md`.

## The Core Belief

Simplicity in code does not come from a markdown rule alone — it comes from disciplined engineering judgment. Care about every line. Push back on bloat. Defend the order.
