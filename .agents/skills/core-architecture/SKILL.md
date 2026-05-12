# Core architecture (Spring Modulith)

## Description

Explains how MedExpertMatch is structured as a **Spring Boot monolith** with **Spring Modulith** modules under `com.berdachuk.medexpertmatch`. Covers `core` as shared infrastructure, dependency rules in `package-info.java`, and where orchestration is allowed.

## When to use

- Refactors that touch more than one top-level package under `medexpertmatch`.
- Questions about **which module may depend on which** (see `allowedDependencies`).
- Adding a new module or moving classes across module boundaries.
- Using **GraphService** for Apache AGE (all Cypher goes through it; MERGE patterns).

## Instructions

- Open the target module’s `package-info.java` and list `allowedDependencies` before adding imports from another module.
- Treat **`core`** as intentional shared infrastructure (config, health, utilities, JDBC helpers), not a violation to depend on from domain modules.
- **Orchestration** that legitimately spans domains belongs in **`llm`** (and similar coordinators), not in repositories.
- **Graph**: use `GraphService.executeCypher` only; embed parameters per project rules; avoid raw JDBC for Cypher.
- **Spring Modulith verification**: project may disable strict verification tests; still follow declared dependencies in code reviews.

## Boundaries

- Do not relax module boundaries “just for this PR” without updating `package-info.java` and team agreement.
- Do not introduce circular dependencies between modules.
- Do not bypass `GraphService` for graph access.
