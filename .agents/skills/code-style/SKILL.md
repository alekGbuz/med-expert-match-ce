# Code style and implementation patterns

## Description

Java and Spring **idioms** for this repository: interfaces for services/repositories, Lombok, formatting, JavaDoc on interfaces, repository insert/update split, and prompt externalization.

## When to use

- Writing new Java classes in any module.
- Implementing repositories, services, REST controllers, or tests.
- Choosing between records vs classes, checked vs unchecked exceptions, logging style.

## Instructions

- **Interfaces**: define `*Service` / `*Repository` in API packages; implementations in `impl` subpackages with mappers where used.
- **Transactions**: `@Transactional` on service layer; read-only for queries.
- **Repositories**: separate `insert` and `update`; no combined `saveOrUpdate` at repository level.
- **SQL**: prefer external `.sql` files where the project already does; dedicated row mappers.
- **JavaDoc**: full JavaDoc on **interface** methods; avoid duplicating on impl unless adding implementation-specific throws.
- **Comments**: keep short (team rule: brief comments; avoid long blocks in code).
- **LLM prompts**: only `PromptTemplate` + files under `src/main/resources/prompts/*.st` (no large inline strings).
- **Formatting**: 4 spaces, 120 columns, standard brace style.

## Boundaries

- Do not add generic `catch (Exception)` or `printStackTrace`.
- Do not add silent fallbacks for errors (fail fast).
- Do not weaken medical-data logging rules to “make debugging easier.”
