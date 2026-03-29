# Story 1.3: Bootstrap first organization and admin user

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide for implementation. -->

## Story

As an **implementer / first operator**,
I want **a controlled way to create the first organization and admin credentials**,
so that **the system can be deployed without a public signup that violates employee-only scope (FR27 foundation)**.

**Implements:** FR2 (enables org record); FR27 direction (employee-only bootstrap, not customer signup).

**Epic:** Epic 1 — Bootstrap, tenancy & authentication — follows **1.2** (organizations/users schema + JPA); **precedes** **1.4** (auth API — this story establishes the first admin user and hashed password storage login will use).

## Acceptance Criteria

1. **Given** a **secured** bootstrap path **when** it runs successfully **then** exactly **one** `Organization` and **one** `User` exist in the database, the user has role **Admin**, credentials are supplied via **environment / request** (not committed to git), and password material is stored only as a **strong hash** (e.g. BCrypt).
2. **Given** a second bootstrap attempt **when** an organization (or bootstrap completion marker — see Dev Notes) already exists **then** the operation is **rejected** with **RFC 7807 Problem Details** (`application/problem+json`), stable machine **`code`**, and **no secrets** in response body or logs.
3. **Given** integration tests **when** they run **then** they prove: successful bootstrap on empty DB; second call fails with expected Problem Details; tests do not embed real production secrets (use test-only token/password patterns).

## Tasks / Subtasks

- [x] **Schema + Liquibase** (AC: 1)
  - [x] Add changeset `0003-...` under `apps/api/src/main/resources/db/changelog/changes/` to extend `users` with fields required for login + role: at minimum **`email`** (unique where used for sign-in), **`password_hash`**, **`role`** (store as `varchar` with Java enum `ADMIN` / `TECHNICIAN` aligned with Epic 2 — bootstrap sets `ADMIN` only).
  - [x] Include from `db.changelog-master.yaml`.
  - [x] **Update** `OrganizationMembershipPersistenceTest` (and any fixture builders) so inserted users satisfy **new NOT NULL / uniqueness** rules — 1.2 tests must keep passing after migration.
- [x] **Domain model** (AC: 1)
  - [x] Extend `User` (and `Organization` if needed) in `com.mowercare.model`; add `UserRole` enum; map with `@Column(name = "...")` snake_case.
  - [x] Use **Spring Security** `PasswordEncoder` (BCrypt) for hashing bootstrap password — **do not** invent a custom hash.
- [x] **Bootstrap use case** (AC: 1, 2)
  - [x] Implement a **single** controlled entry point (recommended: **`POST /api/v1/bootstrap/organization`** — versioned REST per architecture) protected by a **shared secret** (e.g. header `X-Bootstrap-Token` must match env `MOWERCARE_BOOTSTRAP_TOKEN` or similar). Reject if token missing/wrong with **401** Problem Details (no hint whether DB is empty).
  - [x] Request body (JSON, camelCase): e.g. `organizationName`, `adminEmail`, `adminPassword` — validate with Bean Validation; **never** log password or token.
  - [x] Transaction: create `Organization`, then `User` with `role = ADMIN`, `password_hash = encode(adminPassword)`.
  - [x] **Idempotency:** if **any** organization row already exists (simplest guard), return **409** Problem Details with code such as `BOOTSTRAP_ALREADY_COMPLETED` (exact string per your `common` error catalog). Combine with **Concurrency and single-tenant bootstrap** in Dev Notes so the “empty DB” path cannot double-create under parallel requests.
- [x] **Problem Details + tests** (AC: 2, 3)
  - [x] Use **RFC 7807** shape consistent with architecture (`type`, `title`, `status`, `detail`, `instance`, **`code`**).
  - [x] Add integration tests (Testcontainers, reuse `AbstractPostgresIntegrationTest`) covering happy path + duplicate bootstrap + invalid/missing token as appropriate.
  - [x] Add **unit tests** per layer (`BootstrapService`, `BootstrapController` + standalone `MockMvc` with `ApiExceptionHandler`, `ApiExceptionHandler` direct); use **`given…_when…_then…`** method names and matching **`@DisplayName("given … when … then …")`**; optional extra IT for validation + blank configured token (see **Testing requirements**).

## Dev Notes

### Scope boundaries

- **In scope:** Bootstrap path, schema for credentials + role, first admin user, Problem Details on failures, tests, `.env.example` **names** for new variables (no values).
- **Out of scope:** JWT issuance, refresh tokens, `POST /login` — **Story 1.4**. No public self-registration UI. No invite flows — **Epic 2**.
- **Mobile:** No Expo work unless you add a trivial dev-only screen (optional; not required by AC).

### Epic traceability (`epics.md` Story 1.3)

| Source acceptance line | Covered in this story |
|------------------------|----------------------|
| Secured path (env flag, one-time token, or seed script) | **Shared secret header + env** (documented in README / `.env.example` names); REST chosen over seed-only so AC can be integration-tested. |
| Exactly one org + one user, Admin role, credentials not in git | Tasks + AC1; hashing via `PasswordEncoder`. |
| Subsequent attempts when org exists → Problem Details | AC2; **409** + stable `code`. |

### Concurrency and single-tenant bootstrap (critical)

- **Risk:** Two parallel `POST` calls on an empty DB can both pass a “count organizations = 0” check and create **two** orgs — violates “exactly one organization”.
- **Require one of:** (a) transaction **`@Transactional(isolation = Isolation.SERIALIZABLE)`** on the bootstrap use case plus re-check of org count inside the transaction; (b) **PostgreSQL advisory lock** (`pg_advisory_xact_lock`) around the insert block; (c) a **unique** database invariant if you introduce a singleton pattern (e.g. single-row table) — avoid ad-hoc “check then insert” under **READ COMMITTED** default.
- Document the chosen approach in code comments or Dev Agent Record.

### Email and uniqueness

- **Prefer** composite **`UNIQUE (organization_id, email)`** — matches Epic 2 (users are per org) and avoids blocking two test orgs with different users that reuse email patterns.
- **Normalize** email to a single case in the service layer (e.g. lower trim) before persist and lookup so login in **1.4** is consistent.
- **Persistence tests** that insert multiple users (including `givenTwoOrganizations_whenOneUserEach`) must use **distinct emails** per `User` once `email` is enforced.

### HTTP security wiring (Story 1.5 precursor)

- **`SecurityConfig`** today uses **`permitAll()`** for all routes (scaffold). For this story, **do not** rely on Spring Security for the bootstrap secret — keep the **application-level** `X-Bootstrap-Token` check in controller/service as specified.
- Optionally add `requestMatchers("/api/v1/bootstrap/**").permitAll()` when you later tighten the chain in **1.5**; until then, explicit matchers are not strictly required if everything stays `permitAll()`.
- **`spring-boot-starter-webmvc-test`** is already available — use **`MockMvc`** (or `TestRestTemplate`) for bootstrap integration tests; **`@AutoConfigureMockMvc`** applies security filters consistent with `SecurityConfig`.

### Architecture compliance

- **Liquibase owns schema** — `ddl-auto` remains `none` / `validate`. [Source: `_bmad-output/planning-artifacts/architecture.md` — Data architecture]
- **REST + JSON:** `/api/v1/...`, camelCase JSON, Problem Details for errors. [Source: `architecture.md` — API & communication patterns, Format patterns]
- **Layering:** Controller → service → repository; no DB in controllers. [Source: `architecture.md` — Structure patterns → Backend]
- **Packages (by type, not by feature):** Controllers in `com.mowercare.controller`; services in `com.mowercare.service`; exceptions and `ApiExceptionHandler` in `com.mowercare.exception`; `@ConfigurationProperties` (e.g. `BootstrapProperties`) in `com.mowercare.config` alongside `SecurityConfig`; HTTP request/response records in `com.mowercare.model.request` / `com.mowercare.model.response`; JPA entities remain in `com.mowercare.model`; repositories in `com.mowercare.repository`.
- **Security:** No secrets in repo; structured logging without passwords/tokens. [Source: `architecture.md` — Logging, Secrets]
- **Tenant model:** Single org from bootstrap; `users.organization_id` remains the tenant key. [Source: `architecture.md` — Multi-tenancy]

### Technical requirements

| Area | Requirement |
|------|-------------|
| API | Spring Boot **4.x**, **Java 25**, Maven (`mvn` on PATH) — same as **1.1** / **1.2** |
| DB | PostgreSQL; new Liquibase file under `db/changelog/changes/` |
| Passwords | **BCrypt** (or delegated to `PasswordEncoder` bean) |
| Errors | `Content-Type: application/problem+json` with stable `code` |

### Library / framework requirements

- **`spring-boot-starter-security`** — already on classpath; use `PasswordEncoder` bean and secure bootstrap endpoint (token check **before** body parsing if feasible to reduce attack surface; at minimum validate token first in controller/service).
- **Bean Validation** (`jakarta.validation`) on bootstrap DTO.

### File structure requirements

Extend **1.2** layout [Source: `1-2-organizations-and-user-membership-data-model.md` — File structure requirements]. **As implemented**, packages follow **type** (controller / service / exception / config / model DTOs), not a single `bootstrap` feature package:

```
apps/api/src/main/java/com/mowercare/
  controller/
    BootstrapController.java
  service/
    BootstrapService.java
  exception/
    ApiExceptionHandler.java
    BootstrapAlreadyCompletedException.java
    InvalidBootstrapTokenException.java
  config/
    SecurityConfig.java
    BootstrapProperties.java
  model/
    Organization.java
    User.java
    UserRole.java
    request/
      BootstrapOrganizationRequest.java
    response/
      BootstrapOrganizationResponse.java
  repository/
    OrganizationRepository.java
    UserRepository.java
apps/api/src/test/java/com/mowercare/
  controller/
    BootstrapControllerTest.java          # unit: MockMvc standalone + mocked service
    BootstrapOrganizationIT.java        # IT: Testcontainers + full context
    BootstrapOrganizationEmptyTokenIT.java   # IT: blank mowercare.bootstrap.token
  exception/
    ApiExceptionHandlerTest.java        # unit: direct handler invocations
  persistence/
    OrganizationMembershipPersistenceTest.java
  service/
    BootstrapServiceTest.java           # unit: Mockito
apps/api/src/main/resources/db/changelog/changes/
  0003-users-credentials-and-role.yaml
```

### Testing requirements

- **`mvn verify`** must pass in CI (same bar as **1.1** / **1.2**).
- **Integration tests (IT):** `@SpringBootTest` + `MockMvc` + Testcontainers (`AbstractPostgresIntegrationTest`) — full HTTP + DB + Liquibase; assert JSON paths compatible with Boot’s `ProblemDetail` serialization (e.g. `$.properties.code` where applicable).
- **Unit tests:** Fast, no Docker — **`BootstrapServiceTest`** (Mockito: token validation, conflict path, happy path with `ReflectionTestUtils` for ids); **`BootstrapControllerTest`** (standalone `MockMvc` + `ApiExceptionHandler`; mocked `BootstrapService`; validation and delegated exceptions; response body assertions may use **`containsString`** for stable `code` values because standalone serialization can differ slightly from full MVC); **`ApiExceptionHandlerTest`** (direct calls to `@ExceptionHandler` methods, including `MethodArgumentNotValidException` with `BeanPropertyBindingResult`).
- **Naming:** Prefer **`given…_when…_then…`** for test methods and parallel **`@DisplayName("given … when … then …")`** for readable reports.
- **Not unit-tested alone:** `BootstrapProperties` (record, no logic); empty exception classes — covered indirectly.

### Previous story intelligence

- **1.2** delivered: `organizations` / `users` tables, FK, `Organization` / `User` entities, `OrganizationRepository` / `UserRepository`, `AbstractPostgresIntegrationTest`, `OrganizationMembershipPersistenceTest`.
- **Explicit follow-up:** Adding `email` / `password_hash` / `role` **changes** the `User` row shape — update persistence tests and any builders to remain valid; prefer **one** bcrypt hash for test fixtures (fixed salt rounds, known password only in test code).
- Review findings from **1.2** (compiler plugin, package naming) — keep **consistent** `com.mowercare.model` / `com.mowercare.repository`.

### Git intelligence

- Latest API work: `feat(api): Story 1.2 organizations and user membership data model` — schema + JPA + Testcontainers; **no** bootstrap or user credentials yet.

### Latest tech notes (verify at implementation time)

- **Spring Security 6 / Boot 4:** Register `PasswordEncoder` bean; use `BCryptPasswordEncoder` or `DelegatingPasswordEncoder` for future-proofing.
- **ProblemDetail:** Spring MVC `ResponseEntity<ProblemDetail>` with `Content-Type` `application/problem+json` — align with global exception handler if you introduce one in this story or defer to **1.4**.

### Project context

- **`docs/project-context.md`** — not present; optional later (**Generate Project Context** skill).

### References

- `_bmad-output/planning-artifacts/epics.md` — Story 1.3 (acceptance criteria)
- `_bmad-output/planning-artifacts/architecture.md` — REST, Problem Details, packages, Liquibase
- `_bmad-output/planning-artifacts/prd.md` — FR2, FR27
- `1-2-organizations-and-user-membership-data-model.md` — prior schema and test patterns

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2) — bmad-dev-story workflow

### Debug Log References

- `ProblemDetail.forStatusAndDetail` in Spring Boot 4 expects `HttpStatus` enum, not raw `int` values.

### Completion Notes List

- Liquibase `0003-users-credentials-and-role.yaml`: `email`, `password_hash`, `role` on `users`; unique `(organization_id, email)`.
- `UserRole` enum; `User` extended with email, password hash, role; composite unique in JPA `@Table`.
- `BootstrapProperties` in `com.mowercare.config` (`mowercare.bootstrap.token` ← `MOWERCARE_BOOTSTRAP_TOKEN`); `BootstrapController` in `com.mowercare.controller` / `BootstrapService` in `com.mowercare.service` — `POST /api/v1/bootstrap/organization`, `X-Bootstrap-Token`, Bean Validation on DTOs in `model.request` / `model.response`.
- Token validation via `MessageDigest.isEqual`; `PasswordEncoder` (BCrypt) bean in `SecurityConfig`.
- Concurrency: `pg_advisory_xact_lock(583921004)` before org count check + inserts (documented in `BootstrapService`).
- `ApiExceptionHandler` in `com.mowercare.exception`: 401 `BOOTSTRAP_UNAUTHORIZED`, 409 `BOOTSTRAP_ALREADY_COMPLETED`, validation → 400 `VALIDATION_ERROR`.
- `BootstrapOrganizationIT` under `com.mowercare.controller` (MockMvc + Testcontainers); DB cleanup `@BeforeEach`; persistence tests updated with distinct emails + encoded passwords.
- README bootstrap section; `.env.example` documents `MOWERCARE_BOOTSTRAP_TOKEN`.
- OpenAPI for bootstrap deferred to Story 1.4 (per story validation report).
- **Package refactor:** Replaced feature package `com.mowercare.bootstrap` and `com.mowercare.common.api` with type-based layout (`controller`, `service`, `exception`, `config`, `model.request` / `model.response`).
- **QA follow-up (Quinn / `bmad-agent-qa`):** Unit tests: `BootstrapServiceTest`, `BootstrapControllerTest`, `ApiExceptionHandlerTest`. IT extended: `BootstrapOrganizationIT` (given/when/then display names; validation cases for short password + invalid email); `BootstrapOrganizationEmptyTokenIT` (blank `mowercare.bootstrap.token` → 401). `@MockitoSettings(LENIENT)` on service tests where advisory-lock stubs are unused on early-exit paths.

### File List

- `apps/api/src/main/java/com/mowercare/ApiApplication.java`
- `apps/api/src/main/java/com/mowercare/controller/BootstrapController.java`
- `apps/api/src/main/java/com/mowercare/service/BootstrapService.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/main/java/com/mowercare/exception/BootstrapAlreadyCompletedException.java`
- `apps/api/src/main/java/com/mowercare/exception/InvalidBootstrapTokenException.java`
- `apps/api/src/main/java/com/mowercare/config/BootstrapProperties.java`
- `apps/api/src/main/java/com/mowercare/config/SecurityConfig.java`
- `apps/api/src/main/java/com/mowercare/model/Organization.java`
- `apps/api/src/main/java/com/mowercare/model/User.java`
- `apps/api/src/main/java/com/mowercare/model/UserRole.java`
- `apps/api/src/main/java/com/mowercare/model/request/BootstrapOrganizationRequest.java`
- `apps/api/src/main/java/com/mowercare/model/response/BootstrapOrganizationResponse.java`
- `apps/api/src/main/java/com/mowercare/repository/OrganizationRepository.java`
- `apps/api/src/main/java/com/mowercare/repository/UserRepository.java`
- `apps/api/src/main/resources/application.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/resources/db/changelog/changes/0003-users-credentials-and-role.yaml`
- `apps/api/src/test/java/com/mowercare/controller/BootstrapControllerTest.java`
- `apps/api/src/test/java/com/mowercare/controller/BootstrapOrganizationIT.java`
- `apps/api/src/test/java/com/mowercare/controller/BootstrapOrganizationEmptyTokenIT.java`
- `apps/api/src/test/java/com/mowercare/exception/ApiExceptionHandlerTest.java`
- `apps/api/src/test/java/com/mowercare/service/BootstrapServiceTest.java`
- `apps/api/src/test/java/com/mowercare/persistence/OrganizationMembershipPersistenceTest.java`
- `.env.example`
- `README.md`

### Change Log

- 2026-03-29 — Story 1.3: Bootstrap schema (email, password hash, role), `POST /api/v1/bootstrap/organization` with shared token, Problem Details, advisory lock, integration + persistence tests, README / `.env.example`.
- 2026-03-29 — API package layout: type-based packages (`controller`, `service`, `exception`, `config`); HTTP DTOs under `model.request` / `model.response`; `ApiExceptionHandler` under `exception`; integration test co-located with `BootstrapController` in `controller` test package.
- 2026-03-29 — Test coverage: unit tests for `BootstrapService`, `BootstrapController`, `ApiExceptionHandler`; expanded IT (`BootstrapOrganizationIT`, `BootstrapOrganizationEmptyTokenIT`); given/when/then naming with `@DisplayName`.

---

## Story validation report

**Validated:** 2026-03-29 — `bmad-create-story` validate (checklist-driven gap pass).

**Checks performed:** Re-read `epics.md` Story 1.3 AC; `pom.xml` test stack (`spring-boot-starter-webmvc-test`, validation, security test); `OrganizationMembershipPersistenceTest` for post-migration fixture impact; `SecurityConfig` permit-all behavior.

**Gaps addressed in this file:** Concurrency / TOCTOU on first bootstrap; email uniqueness strategy aligned with per-org users and multi-org test data; explicit note that bootstrap secret stays **application-level** until global API security lands in **1.5**; MockMvc dependency confirmation; epic traceability table.

**Residual risks (acceptable / follow-up):** OpenAPI document for bootstrap endpoint can stay minimal or be deferred to **1.4** if you prefer one OpenAPI pass — call out in Dev Agent Record if omitted.

**Story completion status:** done — Code review patches applied; `mvn verify` green.

### Review Findings

- [x] [Review][Patch] RFC 7807 responses omit `instance` and use `about:blank` for `type` — align with architecture Problem Details (`type`, `title`, `status`, `detail`, `instance`, `code`) [ApiExceptionHandler.java] — fixed: stable `urn:mowercare:problem:*` types and `instance` from request URI
- [x] [Review][Patch] Validation handler surfaces only the first field error in `detail` — clients may miss concurrent validation failures [ApiExceptionHandler.java:47-50] — fixed: all field errors joined in `detail`
- [x] [Review][Defer] `SecurityConfig` permits all routes and disables CSRF — pre-existing scaffold; global API security deferred to Story 1.5 [SecurityConfig.java:25-26]
