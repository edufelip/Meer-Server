<h1 align="start">Meer API</h1>

<p align="start">
  <a href="https://medium.com/@eduardofelipi"><img alt="Medium" src="https://img.shields.io/static/v1?label=Medium&message=@edu_santos&color=gray&logo=medium"/></a>
  <a href="https://www.youtube.com/channel/UCYcwwX7nDU_U0FP-TsXMwVg"><img alt="Profile" src="https://img.shields.io/static/v1?label=Youtube&message=edu_santos&color=red&logo=youtube"/></a>
  <a href="https://github.com/edufelip"><img alt="Profile" src="https://img.shields.io/static/v1?label=Github&message=edufelip&color=white&logo=github"/></a>
  <a href="https://www.linkedin.com/in/eduardo-felipe-dev/"><img alt="Linkedin" src="https://img.shields.io/static/v1?label=Linkedin&message=edu_santos&color=blue&logo=linkedin"/></a>
  <a href="http://localhost:8080/swagger-ui/index.html"><img alt="Swagger UI" src="https://img.shields.io/badge/docs-Swagger%20UI-brightgreen?logo=swagger"/></a>
  <a href="http://localhost:8080/v3/api-docs"><img alt="OpenAPI" src="https://img.shields.io/badge/OpenAPI-JSON-blue?logo=openapiinitiative"/></a>
</p>

<p align="start">
  Java 17 Spring Boot REST API powering the Meer app.
</p>

## Prerequisites
- Java 17+ (set `JAVA_HOME`; Temurin 17 is known-good).
- Gradle 8+ (use the bundled `./gradlew`).

## Clone
```
$ git clone <your-repository-url>
```

## Spring Boot API (Java)
Location: `springboot/`

- Environment
  - DB vars: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DEVDB_NAME`, `DB_USER`, `DB_PASSWORD`.
  - JWT: `SECURITY_JWT_SECRET` (>=32 bytes), `SECURITY_JWT_ACCESS_TTL_MINUTES`, `SECURITY_JWT_REFRESH_TTL_DAYS`.
  - Google login: `GOOGLE_ANDROID_CLIENT_ID`, `GOOGLE_IOS_CLIENT_ID`, `GOOGLE_WEB_CLIENT_ID`.
  - Copy `springboot/.env.example` → `springboot/.env` and fill the values (never commit secrets).
  - Profiles: `default` (remote/cloud Postgres), `local-db` (dev Postgres), `prod` (production), `local` (H2 sandbox). Set via `SPRING_PROFILES_ACTIVE`.
- Run
  - `cd springboot && ./run-local.sh` — loads `.env`, defaults to `SPRING_PROFILES_ACTIVE=prod`; override with `SPRING_PROFILES_ACTIVE=local-db ./run-local.sh` to hit the dev DB.
  - `SPRING_PROFILES_ACTIVE=local-db ./gradlew :springboot:bootRun` — run from repo root.
  - `SPRING_PROFILES_ACTIVE=local ./gradlew :springboot:bootRun` — in-memory H2.
- Tests
  - `cd springboot && ./run-tests.sh` — runs with `.env` loaded (defaults to `local-db` profile).
  - `./gradlew :springboot:test` — JUnit 5 suite from repo root.

## Security Model
- Public routes (no JWT required): `/auth/login`, `/auth/signup`, `/auth/google`, `/auth/apple`, `/auth/refresh`, `/auth/forgot-password`.
- Protected routes pass through `RequestGuardsFilter`:
  - `X-App-Package` must match `security.appPackage` when `SECURITY_REQUIRE_APP_HEADER=true`.
  - Optional Firebase App Check header `X-Firebase-AppCheck` (`SECURITY_REQUIRE_APP_CHECK=true`).
  - JWT access token in `Authorization: Bearer <token>` unless `SECURITY_DISABLE_AUTH=true`.
- Token endpoints return `{ token, refreshToken, user }`; failures return 401/403 with `{ "message": "Invalid or expired token" }`.

## Architecture Overview
- Controllers: `springboot/src/main/java/com/edufelip/meer/web/` — HTTP only, delegate to use cases.
- Use cases: `springboot/src/main/java/com/edufelip/meer/domain/` — business logic.
- Repositories: `springboot/src/main/java/com/edufelip/meer/domain/repo/` — Spring Data JPA.
- Entities: `springboot/src/main/java/com/edufelip/meer/core/**`.
- DTOs & mappers: `springboot/src/main/java/com/edufelip/meer/dto/` and `springboot/src/main/java/com/edufelip/meer/mapper/`.
- Config: `springboot/src/main/resources/application.yml`.

## Code Map (Key Paths)
- Auth flow: `web/AuthController.java`, use cases in `domain/auth/*`, token provider in `security/token/`.
- Stores & content: `web/ThriftStoreController.java`, `domain/*ThriftStore*`, `domain/*GuideContent*`.
- Categories: `web/CategoryController.java`, repo in `domain/repo/CategoryRepository.java`.
- Filters/guards: `security/RequestGuardsFilter.java`, guard classes in `security/guards/`.

## Infrastructure Notes
- Remote Postgres is the default; for the dev database set `SPRING_PROFILES_ACTIVE=local-db` and point `DB_*` to the dev instance.
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`; OpenAPI JSON at `/v3/api-docs`.

## Contributing
1. Create a feature branch.
2. Run `./gradlew :springboot:test` (and `./gradlew :springboot:clean build` before opening a PR).
3. Commit with `type(scope): summary`.
4. Open a PR with description, linked issue, and commands run.
