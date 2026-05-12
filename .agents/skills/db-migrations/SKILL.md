# Database and Flyway migrations

## Description

PostgreSQL schema evolution for this project: **Flyway** on the **primary** datasource only, consolidated **V1** rule for MVP, and coexistence with **Apache AGE** / **pgvector** in test containers.

## When to use

- Adding tables, columns, indexes, or constraints.
- Writing or reviewing Flyway scripts under `src/main/resources/db/migration`.
- Explaining why Flyway must not run against a secondary read-only datasource.

## Instructions

- **Primary DB only**: Flyway is wired to the main application `DataSource`; never point Flyway at external read-only sources.
- **MVP consolidation**: production/dev schema changes belong in **`V1__initial_schema.sql`** (single consolidated migration) per project policy; avoid V2+ until post-MVP unless policy changes.
- **Testcontainers**: integration tests use the custom image documented in `scripts/` and README; rebuild image when Dockerfile for tests changes.
- **AGE / graph**: schema must support graph usage expected by `GraphService` and repositories.

## Boundaries

- Do not add Flyway to a secondary datasource.
- Do not scatter one logical change across many versioned files while MVP single-file policy is in force.
