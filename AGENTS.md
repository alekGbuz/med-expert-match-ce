# MedExpertMatch — agent index

AI-assisted medical **expert–case matching**: Spring Boot monolith, PostgreSQL with pgvector and Apache AGE, Thymeleaf UI, Spring AI (OpenAI-compatible providers only).

## Repo map

- `src/main/java/com/berdachuk/medexpertmatch/<module>/` — Spring Modulith modules; boundaries in each `package-info.java` (`@ApplicationModule`).
- `src/main/resources/` — `application*.yml`, Flyway `db/migration`, Thymeleaf, static assets, **`prompts/*.st`** (LLM templates).
- `src/test/java/...` — unit tests (`*Test`); integration tests (`*IT`) use Testcontainers.
- `docs/` — English documentation; agent layering: [docs/ai-context-strategy.md](docs/ai-context-strategy.md).
- `.agents/skills/` — **canonical deep rules** for agents (load by task type).

## Architecture (modules)

**Infrastructure hub:** `core`. **HTTP/UI edge:** `web`. **LLM orchestration:** `llm`. **Semantic graph retrieval (SgrService):** `retrieval`. **Clinical and data domains:** `medicalcase`, `doctor`, `clinicalexperience`, `facility`, `medicalcoding`, `embedding`, `graph`, `evidence`, `caseanalysis`, `ingestion`, `system`.

Dependency edges are defined by `allowedDependencies` in each module’s `package-info.java` (for example `web` depends on `llm`, `medicalcase`, `retrieval`, and others; `retrieval` depends on many domain modules; most domain modules depend only on `core`).

## Commands

| Goal | Command |
|------|---------|
| Full build | `mvn clean install` |
| Skip tests | `mvn clean install -DskipTests` |
| Unit tests only | `mvn test -Dtest="!*IT"` |
| Integration + verify | `mvn verify` (requires **Docker** and the test Postgres image; see `scripts/`) |
| Run local | `mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local` |

## Module `AGENTS.md` (nested)

| Path | Role |
|------|------|
| [core/AGENTS.md](src/main/java/com/berdachuk/medexpertmatch/core/AGENTS.md) | Shared infrastructure |
| [llm/AGENTS.md](src/main/java/com/berdachuk/medexpertmatch/llm/AGENTS.md) | Agents, Spring AI, prompts |
| [web/AGENTS.md](src/main/java/com/berdachuk/medexpertmatch/web/AGENTS.md) | REST and Thymeleaf edge |
| [retrieval/AGENTS.md](src/main/java/com/berdachuk/medexpertmatch/retrieval/AGENTS.md) | Scoring and retrieval layer |

## Skills index (`.agents/skills/`)

| Skill directory | When to load |
|-----------------|--------------|
| `core-architecture` | Modulith boundaries, `GraphService`, module dependencies |
| `domain-modeling` | Types under `*/domain/`, PHI safety, ICD-10 |
| `code-style` | Java idioms, repositories and services, JavaDoc, Lombok |
| `testing` | Surefire vs Failsafe, Docker IT, `TestAIConfig` mocks |
| `db-migrations` | Flyway on primary DB, V1 consolidation |
| `api-design` | OpenAPI, controllers, Thymeleaf and static assets |

Each skill: `.agents/skills/<name>/SKILL.md` (single source of truth).

## Global boundaries

- **Allowed:** refactors inside one module respecting `allowedDependencies`; tests; English docs under `docs/`.
- **Careful:** cross-module wiring, anything that could log clinical narrative or identifiers.
- **Disallowed:** silent generic error swallowing; hardcoded LLM prompt strings in Java (use `prompts/*.st`); Flyway on a non-primary datasource; real patient data in repo; Ollama as a first-class provider.

## Stack pointers

- Dependency versions: root `pom.xml`.
- HTTP contract: `src/main/resources/api/openapi.yaml` when changing APIs.

Read [docs/ai-context-strategy.md](docs/ai-context-strategy.md) for how root, nested `AGENTS.md`, and skills relate.
