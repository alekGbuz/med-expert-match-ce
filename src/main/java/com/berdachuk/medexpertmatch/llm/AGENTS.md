# Module: `llm`

## Purpose

**AI orchestration**: medical agent workflows, tools, REST for agent operations, and coordination across **doctor**, **medicalcase**, **retrieval**, **graph**, **caseanalysis**, **embedding**, and related modules per `package-info.java`.

## Owned domain

- Job/status types under `llm/domain` (e.g. match, route, prioritize, analyze job status).
- Agent-facing DTOs and controllers under `llm/rest` and `llm/service`.

## Spring AI (this repo)

- **Providers**: OpenAI-compatible only; configure via `spring.ai.*` and `spring.ai.custom.*` (see `SpringAIConfig` and `application.yml`).
- **Prompts**: `PromptTemplate` + `src/main/resources/prompts/*.st` only.
- **Tests**: `TestAIConfig` supplies mocks; stub `getDefaultOptions()` on mocked `ChatModel` when Spring AI builds merged prompts.

## Boundaries

- Prefer **orchestration** here over duplicating domain persistence owned by other modules.
- Respect **Modulith `allowedDependencies`** for `llm` (wide but explicit list in `package-info.java`).
- Never log PHI; include medical disclaimers in AI-facing templates per project policy.

## Skills

- `.agents/skills/domain-modeling/SKILL.md` — clinical entities referenced by agents.
- `.agents/skills/code-style/SKILL.md` — prompts and service patterns.
- `.agents/skills/testing/SKILL.md` — mock LLM and IT behavior.
