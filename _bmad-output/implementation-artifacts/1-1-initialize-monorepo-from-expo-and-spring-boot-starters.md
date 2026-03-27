# Story 1.1: Initialize monorepo from Expo and Spring Boot starters

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide for implementation. -->

## Story

As a **developer**,
I want **the repository scaffolded from the approved starters with baseline tooling**,
so that **all later work shares one layout, build, and migration discipline**.

**Implements:** Architecture starter template; **FR28**, **FR29** (no billing, no third-party product integrations in scaffold).

**Epic:** Epic 1 — Bootstrap, tenancy & authentication (see `../planning-artifacts/epics.md`) — this is the **first** story; later stories add org/user models, auth API, mobile shell (Expo Router, Paper, Query), and sign-in UI.

## Acceptance Criteria

1. **Given** a clean repo root **when** the initializer runs per architecture **then** both apps build successfully locally and `README.md` documents how to run API and mobile dev servers.
2. **Given** the initializer **when** it runs **then** `apps/mobile` is created with Expo using the **blank TypeScript** template (`create-expo-app` / `blank-typescript` per architecture starter section “Mobile — Expo”).
3. **Given** the initializer **when** it runs **then** `apps/api` is a **Spring Boot 4.x** **Maven** project with **Java 25** and dependencies: **web**, **data-jpa**, **postgresql**, **security**, **validation**, **liquibase** (per architecture — **Liquibase**, not Flyway).
4. **Given** the API project **when** Liquibase is configured **then** `spring.jpa.hibernate.ddl-auto` is **`none`** or **`validate`** and a **minimal master changelog** exists so the app can start against PostgreSQL with migrations applied and **no Hibernate-owned schema drift**.
5. **Given** the repo **when** scaffold is complete **then** root contains `.gitignore`, `.editorconfig`, `.env.example` (variable **names** only, no secrets), and `.github/workflows/` runs **API build/test** and **mobile lint + typecheck** on PR.

## Tasks / Subtasks

- [x] **Monorepo layout** (AC: 1, 2, 3, 5)
  - [x] Create `apps/mobile` with `npx create-expo-app@latest` using `--template blank-typescript` (target path `apps/mobile` from repo root).
  - [x] Create `apps/api` via Spring Initializr (web UI or `curl` to `start.spring.io`) as **Maven** (`type=maven-project`), **Spring Boot 4.x**, **Java 25**, artifact under `apps/api`. **Do not** keep the Maven Wrapper: remove **`mvnw`**, **`mvnw.cmd`**, and **`.mvn/`** if the generator adds them; version control **`pom.xml`** and source only.
  - [x] Add root `README.md` with prerequisites (**JDK 25**, Node, PostgreSQL), how to run API (`mvn spring-boot:run` from `apps/api`; **local dev assumes Maven is already installed** — note that `mvn` must be on `PATH`), how to run mobile (`npx expo start` from `apps/mobile`), and that PostgreSQL must be reachable for API startup if datasource is configured.
- [x] **Liquibase baseline** (AC: 4)
  - [x] Add `src/main/resources/db/changelog/` with master changelog (YAML or XML) and at least one empty or minimal changeset if required for Liquibase to run cleanly.
  - [x] Set `spring.jpa.hibernate.ddl-auto=none` (or `validate`) in application config; document in README.
- [x] **Root hygiene** (AC: 5)
  - [x] `.gitignore` covering Java/Maven (`target/`), Node (`node_modules/`, Expo artifacts), OS/editor junk, and `.env*`.
  - [x] `.editorconfig` for basic cross-editor consistency.
  - [x] `.env.example` listing e.g. `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (names only).
- [x] **CI** (AC: 5)
  - [x] Workflow: checkout → setup **JDK 25** → **Maven** available on the runner (e.g. install `mvn` or use a setup-maven action) → API `cd apps/api && mvn -B verify` → setup Node → mobile `npm ci` + `npm run lint` (add script if missing) + `tsc --noEmit` or `expo export` not required for PR — **lint + typecheck** minimum.

### Review Findings

- [x] [Review][Patch] Document that **Docker** must be available for `mvn verify` / tests — **Testcontainers** starts PostgreSQL in `ApiApplicationTests` — [README.md]
- [x] [Review][Patch] Clarify that **Spring Boot** reads variables from the **process environment**; a project-root `.env` file is **not** loaded automatically unless the developer uses `direnv`, shell `export`, IDE env injection, or an explicit `.env` library — [README.md]
- [x] [Review][Patch] Harden CI: verify **integrity** of the downloaded Maven distribution (e.g. official **SHA512** check) before extracting — [.github/workflows/ci.yml]
- [x] [Review][Defer] Initializr placeholder metadata in `pom.xml` (empty `license`, `developers`, `scm`) — deferred, pre-existing template noise — [apps/api/pom.xml]
- [x] [Review][Defer] Static `PostgreSQLContainer` field may conflict if multiple test classes run in parallel later — deferred until test suite grows — [apps/api/src/test/java/com/mowercare/ApiApplicationTests.java]

## Dev Notes

### Scope boundaries (do not overscope)

- **In this story:** Empty/minimal API that starts, connects to DB via configured datasource, runs Liquibase; minimal Expo app that bundles. **No** business endpoints beyond health/actuator optional stub; **no** Expo Router, TanStack Query, or React Native Paper — those are **Story 1.6+**.
- **No** billing or third-party SaaS integrations in scaffold (**FR28**, **FR29**).

### Architecture compliance

- **Spring Boot version:** Use **Spring Boot 4.x** on Initializr for `apps/api`. Planning/architecture docs may cite Spring Boot 3.x examples; **this story overrides** so the codebase starts on the 4.x line.
- **Build & JDK:** **Maven** via **`mvn` on `PATH`** (no wrapper). **Local:** Maven is **already installed** — README should not lead with full Maven install steps; a short “requires `mvn` on PATH” line is enough for other contributors. **`pom.xml` only** — **no** Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`). **Java 25** for compile, test, and CI — **this story overrides** generic “17+” wording elsewhere. **CI** must still expose `mvn` on the runner (install or action).
- **Repo layout:** Single repo `mowercare/` with **`apps/api`** and **`apps/mobile`**; planning stays under `_bmad-output/` — Source: `../planning-artifacts/architecture.md` (Project Structure & Boundaries)
- **API package root:** `com.mowercare` — extend in later stories per package table (`common/`, `organization/`, `auth/`, …).
- **Liquibase owns schema:** Never rely on `ddl-auto=update` in deployed environments; Hibernate **none** or **validate** only.
- **REST/OpenAPI, Problem Details, JWT:** Out of scope for 1.1; only structure and CI.

### Technical requirements

| Area | Requirement |
|------|-------------|
| API | **Spring Boot 4.x**, **Java 25**, **Maven**, Spring Web, Data JPA, PostgreSQL driver, Security, Validation, Liquibase |
| DB | PostgreSQL; Liquibase changelogs under `db/changelog/` per architecture naming |
| Mobile | Expo with **blank-typescript**; TypeScript strictness per template defaults |
| Secrets | None in repo; `.env.example` names only |

### Library / framework requirements

- **Expo:** `npx create-expo-app@latest` with `--template blank-typescript`. Resolve **SDK version** from generated `package.json` / `app.json`; document in README. If CLI defaults differ from docs, **follow generated project** as source of truth.
- **Spring Boot:** Generate from [start.spring.io](https://start.spring.io/) as **Maven** (`type=maven-project`) with **Spring Boot 4.x**, **Java 25**, and dependencies: `web`, `data-jpa`, `postgresql`, `security`, `validation`, `liquibase`. Pin the Boot version in the **parent POM** to **4.x** (match Initializr’s 4.x line at generation time). Strip wrapper artifacts after unzip; build with **`mvn`** only.
- **Java:** Use **25** for local dev, CI, and `maven-compiler-plugin` / toolchain settings so the API always targets one JDK line.
- **Known pitfall:** Spring Boot + Liquibase + PostgreSQL — use normal lowercase DB names in dev; if you hit driver/Liquibase edge cases, check managed versions (PostgreSQL driver **42.7.6+** mentioned in community reports for certain Liquibase scenarios).

### File structure requirements

Align with architecture doc — Top-level layout & app folders (`../planning-artifacts/architecture.md`):

```
.github/workflows/     # CI
apps/api/                # pom.xml, src/main/java/com/mowercare/... (no mvnw / .mvn)
apps/mobile/             # Expo app (package.json, app.json, ...)
docs/                    # optional; may stay empty
_bmad-output/            # existing planning — do not delete
README.md
.gitignore
.editorconfig
.env.example
```

### Testing requirements

- **API:** Default Initializr test (`@SpringBootTest` context load) passes; add **Testcontainers PostgreSQL** only if quick to wire — otherwise document “requires PostgreSQL” for local run and ensure CI uses **service container** or **Testcontainers** so PR proves migrations run. Minimum bar: **CI runs `mvn verify`** (from `apps/api`) under **JDK 25** with **system Maven** successfully.
- **Mobile:** `npm run lint` and TypeScript check in CI; no E2E required for 1.1.

### Previous story intelligence

- **None** — first story in Epic 1. No prior implementation artifact in this repo for mowercare app code.

### Git intelligence

- Recent commits are planning/BMad only (`Add BMad module, planning artifacts, and Cursor skills`). **No existing `apps/api` or `apps/mobile`** — greenfield scaffold.

### Latest tech notes (verify at implementation time)

- **Expo:** Prefer official [create-expo-app](https://docs.expo.dev/more/create-expo/) docs; template name **`blank-typescript`**.
- **Spring:** Use Initializr with **Spring Boot 4.x**, **Maven** (`mvn`), **Java 25**; confirm exact patch version against [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html). No wrapper — **local Maven is already installed**; for **CI**, ensure `mvn` is on the runner ([Maven install](https://maven.apache.org/install.html) if you need a reference for the workflow).

### Project context

- **`project-context.md`** not present under `docs/` — optional follow-up: run **Generate Project Context** after scaffold exists.

### References

- `../planning-artifacts/epics.md` — Story 1.1
- `../planning-artifacts/architecture.md` — Starters, monorepo layout, Liquibase rules
- `../planning-artifacts/prd.md` — FR28/FR29 MVP boundaries

## Dev Agent Record

### Agent Model Used

Cursor agent — Dev Story workflow

### Debug Log References

- Spring Initializr emits `spring-boot-starter-parent` version `4.0.5.RELEASE`; Maven Central publishes **4.0.5** (no `.RELEASE` suffix). Parent was set to **4.0.5** so the build resolves.
- Testcontainers: use **`org.testcontainers:testcontainers-postgresql`** (2.0.2) with `org.testcontainers.postgresql.PostgreSQLContainer`; container started from `@DynamicPropertySource` (no separate JUnit extension dependency).

### Completion Notes List

- Scaffolded `apps/mobile` with **Expo SDK 55** / `blank-typescript`; added `lint` (Expo ESLint) and `typecheck` (`tsc --noEmit`).
- Generated `apps/api` with Spring Boot **4.0.5**, Java **25**, required starters; removed Maven Wrapper; Liquibase master + baseline changeset; `spring.jpa.hibernate.ddl-auto=none` in `application.yaml`; permissive `SecurityConfig` until later epic stories.
- API tests: `@SpringBootTest` + **Testcontainers** (`testcontainers-postgresql` 2.0.2) + `@DynamicPropertySource` so `mvn verify` runs migrations against a real Postgres.
- Root: `README.md`, `.gitignore`, `.editorconfig`, `.env.example`; `.github/workflows/ci.yml` runs API `mvn verify` and mobile lint/typecheck.

### File List

- `.editorconfig`
- `.env.example`
- `.github/workflows/ci.yml`
- `.gitignore`
- `README.md`
- `apps/api/pom.xml`
- `apps/api/src/main/java/com/mowercare/ApiApplication.java`
- `apps/api/src/main/java/com/mowercare/config/SecurityConfig.java`
- `apps/api/src/main/resources/application.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/resources/db/changelog/changes/0001-baseline.yaml`
- `apps/api/src/test/java/com/mowercare/ApiApplicationTests.java`
- `apps/mobile/` (Expo project: `app.json`, `package.json`, `package-lock.json`, `eslint.config.js`, `index.ts`, `tsconfig.json`, assets, etc.)

### Change Log

- 2026-03-27 — Story 1.1: Expo blank-typescript mobile app; Spring Boot 4.0.5 / Java 25 API with Liquibase baseline, Testcontainers-backed tests, root hygiene, and CI (API + mobile).
- 2026-03-27 — Code review: README (Docker for tests, Spring env vs `.env`); CI Maven install SHA512 verification.

---

**Story completion status:** done — Code review complete; review patches applied.
