# Module: `web`

## Purpose

**HTTP edge**: Spring MVC controllers, Thymeleaf views, and web-specific services that expose the application to browsers and HTTP clients. Declared dependency hub into `llm`, `medicalcase`, `doctor`, `retrieval`, `graph`, `ingestion`, and `core`.

## Owned artifacts

- Controllers and view models for UI and public HTTP entry points under `web` (and thin REST adapters if placed here by convention).

## Boundaries

- **No heavy domain logic** in controllers; delegate to module services.
- Do not bypass security or CORS policies defined for the app.
- Keep Thymeleaf templates and static assets in `src/main/resources/templates` and `static/`.

## Skills

- `.agents/skills/api-design/SKILL.md` — REST, OpenAPI, Thymeleaf layout.
- `.agents/skills/domain-modeling/SKILL.md` — DTOs shaped for UI/API.
