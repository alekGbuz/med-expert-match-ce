# Module: `core`

## Purpose

Shared **infrastructure** used across the application: configuration, health checks, JDBC utilities, security-related cross-cutting helpers, exception model, and other non-domain-specific building blocks.

## Owned artifacts

- No clinical domain aggregates; may define cross-cutting types (errors, health, generic utilities).
- SQL helpers under `core/repository/sql` and related infrastructure.

## Boundaries

- Other modules **may depend on `core`**; `core` should not depend on clinical domain modules (`medicalcase`, `doctor`, etc.).
- Keep domain rules out of `core`; only generic technical policies belong here.

## Commands

Same as repository root (`mvn clean install`, `mvn verify`). No separate build.

## Skills

- `.agents/skills/core-architecture/SKILL.md` — Modulith graph and `GraphService` usage.
- `.agents/skills/code-style/SKILL.md` — implementation patterns for infra code.
