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

# Security Check

## When to use

Load this skill **before** and **after** any work that touches:
- authentication or authorization (Spring Security, JWT, sessions, RBAC)
- public or external-facing APIs (REST controllers, GraphQL, A2A, webhooks)
- persistence (Flyway migrations, native SQL, JPA `nativeQuery`, Apache AGE/Cypher)
- secrets, credentials, keys, tokens, or `.env` files
- external input handling (user forms, uploaded documents, LLM tool inputs)
- infrastructure (Dockerfiles, `docker-compose*.yml`, CI/CD workflows, nginx, Grafana)
- new third-party dependencies (added to `pom.xml`)
- LLM prompts, tool definitions, or agent-skill schemas (prompt injection surface)

This skill is project-wide; it applies to every module in `medexpertmatch/`, to `docker/`, to `scripts/`, and to `.github/workflows/`.

## Description

A defensive review skill for AI-generated and human-written code. It does not replace a human security review, but it catches the most common classes of issues that AI agents introduce (or miss) before code reaches review or production. It is aligned with OWASP Top 10 and the project's specific compliance posture (HIPAA, PHI handling, no patient data in logs).

## Instructions

Run the following checks in order. For every finding, record: file path + line, severity (Critical / High / Medium / Low / Info), evidence, and a suggested fix. **Do not auto-fix.** Produce a report and escalate to a human reviewer.

### 1. Secrets Hygiene

- Search the diff and the changed files for: API keys, tokens, passwords, private keys, JWT signing secrets, database passwords, AWS/GCP credentials, OAuth client secrets.
- Check `.env`, `application*.yml`, `application*.properties`, `docker-compose*.yml`, `Dockerfile*`, `start-stack.sh`, `stop-stack.sh`, `scripts/**`.
- Verify that real secrets are never committed; only `.env.example` placeholders are allowed in git.
- Verify that logs do not print secrets (no `log.info("token={}", token)`).
- Verify that test fixtures do not contain real credentials.

### 2. Authentication and Authorization

- Every new REST endpoint has an explicit auth decision in its controller (or a documented "public" reason).
- Authorization is checked at the service or controller level — not only hidden in the UI.
- Spring Security configuration changes preserve the deny-by-default posture.
- No new endpoint trusts a client-supplied `userId` / `doctorId` / `facilityId` without re-checking the principal.
- Role/permission checks exist for any operation that reads or writes PHI.

### 3. Input Validation and Injection Prevention

- All `@RequestParam`, `@PathVariable`, `@RequestBody` fields are validated (`@Valid`, `@NotNull`, `@Size`, etc.) or sanitized.
- JPA repositories do not use string concatenation to build SQL; use named parameters or external `.sql` files.
- Apache AGE / Cypher queries: parameters are bound, never concatenated into the Cypher string.
- LLM prompt inputs are escaped / structured; no user input is concatenated directly into a prompt template without validation.
- File upload endpoints validate content type, size, and (where relevant) magic bytes.
- JSON parsing uses `FAIL_ON_UNKNOWN_PROPERTIES = false` only when explicitly safe.

### 4. Dependency and Supply-Chain Audit

- Every new dependency in `pom.xml` has a recent version, a maintained upstream, and a license compatible with the project.
- Run (or recommend running) `mvn dependency:tree` and OWASP dependency-check on the changed POMs.
- No dependency is pinned to a version with a known critical CVE.
- No new dependency duplicates a function already provided by Spring Boot starters, Spring AI, or Apache Commons.

### 5. Infrastructure and CI/CD

- Dockerfiles: non-root user, pinned base image tag (no `:latest` in prod), minimal attack surface, no secrets baked in.
- `docker-compose*.yml`: no host network in prod; secrets via env or Docker secrets, not literals.
- `.github/workflows/**`: no `pull_request_target` with checkout of untrusted code; secrets are scoped per-job; third-party actions are pinned by SHA.
- Nginx / reverse-proxy configs: TLS, HSTS, secure headers, no verbose error pages.

### 6. Data Protection and Privacy (HIPAA/PHI)

- No patient identifiers (name, MRN, DOB, address, phone, email, photo) in logs, errors, exceptions, or test fixtures.
- All patient data accessed through services is anonymized or de-identified before any non-production use.
- Audit logging is preserved for any PHI access; do not disable or weaken it.
- Backups and exported data are encrypted at rest; keys are not committed.

### 7. OWASP-Aligned Quick Review

Cross-check the diff against OWASP Top 10 (2021):
- A01 Broken Access Control
- A02 Cryptographic Failures
- A03 Injection
- A04 Insecure Design
- A05 Security Misconfiguration
- A06 Vulnerable and Outdated Components
- A07 Identification and Authentication Failures
- A08 Software and Data Integrity Failures
- A09 Security Logging and Monitoring Failures
- A10 Server-Side Request Forgery (SSRF)

For LLM features also check OWASP Top 10 for LLM Applications (prompt injection, sensitive information disclosure, excessive agency, etc.).

### 8. AI-Specific Red Flags

Watch for patterns AI agents commonly introduce:
- `String.format` / `+` concatenation of user input into SQL, Cypher, shell commands, or prompts.
- `Runtime.getRuntime().exec(...)` or `ProcessBuilder` with user-controlled arguments.
- New `permitAll()` / `csrf().disable()` blocks that broaden the previous security posture.
- New `@Transactional` on methods that accept untrusted input across module boundaries (timing / integrity surface).
- Logging entire request bodies or LLM responses containing PHI.

## Output Format

Produce a short report:

```markdown
## Security Check Report

- Scope: <files / diff / commit>
- Findings: <count by severity>
  - Critical: <n>
  - High: <n>
  - Medium: <n>
  - Low: <n>
  - Info: <n>

### Critical / High findings
- [file:line] <title> — <evidence> — <suggested fix>

### Other findings
- ...

### Pre-implementation risks (if run before coding)
- ...

### Verdict
- APPROVE / BLOCK / BLOCK_UNTIL_HUMAN_REVIEW
```

## Boundaries

- **Do not auto-fix vulnerabilities.** Report only; humans decide the fix.
- **Do not approve PRs.** This skill reports; humans approve.
- **Do not weaken HIPAA/PHI protections** for any reason, including "to make tests pass".
- **Do not log, return, or persist secrets or PHI** as part of a "debug" change.
- **Do not run dependency installs or network calls from this skill** in review mode — it is static analysis + grep + small `mvn` queries only.
- **Escalate all Critical and High findings to a human** before any commit or merge.
- This skill is not a substitute for penetration testing, SAST/DAST tooling, or a dedicated security review on regulated changes.

## Run Timing

- **Before implementation** — for risky work (auth, APIs, DB, secrets, external input, infra, new dependencies). Report risks first; let the human decide whether to proceed.
- **After implementation, before commit** — review the final diff using the checklist above.
- **On any change flagged by an external review** (Sonar, CodeQL, Dependabot, GitHub Advanced Security) — re-run the relevant sections.
