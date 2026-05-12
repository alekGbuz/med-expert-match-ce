# Testing

## Description

How tests are split between **Surefire** (unit) and **Failsafe** (integration `*IT`), use of **Testcontainers**, `BaseIntegrationTest`, and mock AI configuration.

## When to use

- Adding or fixing any test.
- Running the right Maven command for CI vs local quick feedback.
- Mocking Spring AI (`ChatModel`, `EmbeddingModel`) in the `test` profile.

## Instructions

- **Integration tests**: class name suffix `IT` or `ITCase`; extend `BaseIntegrationTest`; prepare data in `@BeforeEach` / dedicated helpers; use anonymized IDs.
- **Unit tests**: `*Test` / `*Tests`; no Docker required; keep pure logic and fast Spring slices here when possible.
- **Commands**: `mvn test -Dtest="!*IT"` for unit-only; `mvn verify` for full pipeline including IT (requires **Docker** and test Postgres image per `scripts/`).
- **AI in tests**: tests use mock providers; do not call real LLM endpoints from CI.
- **Test AI config**: mocked `ChatModel` beans must stub `getDefaultOptions()` when Spring AI builds prompts (see `TestAIConfig`).

## Boundaries

- Do not run integration tests in Surefire phase (naming and POM conventions enforce this).
- Do not commit real API keys or patient narratives.
