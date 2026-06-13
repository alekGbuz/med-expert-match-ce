# Tech Context

## Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Build | Maven | 3.9+ |
| Framework | Spring Boot | 4.0.6 |
| Modularity | Spring Modulith | 2.0.7 |
| AI | Spring AI | 2.0.0-M8 |
| AI Session | Spring AI Session JDBC | 0.3.0 |
| Templates | Thymeleaf (SSR) | Spring Boot managed |
| Database | PostgreSQL | 17 |
| Graph DB | Apache AGE | 1.6.0 |
| Vector | pgvector (client) | 0.1.6 |
| Migrations | Flyway | V1 consolidation (single baseline) |
| Testing | JUnit 5, Testcontainers | 2.0.5 |
| Mocks | WireMock (standalone) | 3.9.2 |
| PDF | PDFBox | 3.0.7 |
| Browser test | Playwright | 1.60.0 |
| Container | Docker + Docker Compose | latest |
| Docs | MkDocs (Material theme) | via requirements-docs.txt |

## Commands

```bash
mvn spring-boot:run -Plocal          # Run app (local profile)
mvn clean install                    # Full build
mvn clean install -DskipTests        # Build without tests
mvn test                             # Unit tests (*Test.java, *Tests.java)
mvn verify                           # Integration tests (*IT.java) + package
mvn test -Dtest=DoctorRepositoryIT   # Single IT class
mvn clean verify sonar:sonar         # SonarQube/Cloud analysis
./scripts/build-test-container.sh    # Build custom Postgres+AGE+PgVector test image
./scripts/start-local-stack.sh       # Local stack: Postgres + mvn -Plocal + MkDocs
./scripts/restart-service-local.sh   # Restart local stack
```

## Infrastructure

- **Local dev:** `docker-compose.dev.yml` — PostgreSQL with AGE + pgvector
- **Test:** Testcontainers with custom Docker image (`medexpertmatch-postgres-test`)
- **CI:** GitHub Actions (`.github/workflows/ci.yml`)
- **Monitoring:** Spring Boot Actuator + Prometheus metrics + Grafana dashboards
- **Docs:** MkDocs with Material theme (`mkdocs.yml`, `docs/`, `site/`)
- **Browser:** Auto-launch on `local` profile startup via `LocalHomeBrowserLauncher`

## Key Configuration

- **Database:** `localhost:5433`, schema `medexpertmatch`
- **LLM endpoints:** 6 role-separated (`CLINICAL_HIGH`, `CLINICAL_LOW`, `UTILITY`, `TOOL_CALLING`, `EMBEDDING`, `RERANK`), each with env-configurable baseUrl, apiKey, and model via `spring.ai.custom.*`
- **Models:** MedGemma 1.5 4B (primary), MedGemma 27B (heavy analysis), FunctionGemma (tool-calling)
- **Feature flags:** `medexpertmatch.features.*` in `application.yml`
- **Retrieval weights:** Configurable vector (40%), graph (30%), historical (30%) with optional RRF fusion (k=60)
- **Security:** `medexpertmatch.auth.enabled` (defaults false for local dev; simulated roles for UI demo)
- **Session memory:** `SlidingWindowCompactionStrategy` (compact after 15 turns, max 30 events), JDBC-persisted
- **AutoMemory:** Filesystem-backed durable memory at `${user.home}/.medexpertmatch/automemory/`
