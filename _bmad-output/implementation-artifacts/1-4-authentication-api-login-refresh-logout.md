# Story 1.4: Authentication API — login, refresh, logout

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide for implementation. -->

## Story

As an **employee**,
I want **to obtain and refresh a session via JWT access and refresh tokens**,
so that **I can use the API securely from the mobile app (FR4–FR6)**.

**Implements:** FR4, FR5, FR6; NFR-S1, NFR-S4 (baseline — TLS in deployed environments; no secrets in logs); Additional: Problem Details, OpenAPI, durable refresh storage.

**Epic:** Epic 1 — Bootstrap, tenancy & authentication — follows **1.3** (bootstrap admin + `User` with `email`, `password_hash`, `role`); **precedes** **1.5** (global JWT enforcement + tenant context on protected routes).

## Acceptance Criteria

1. **Given** valid user credentials  
   **When** `POST` **login** is called  
   **Then** the response returns **access** and **refresh** tokens (JSON camelCase), and **OpenAPI** documents the contract; **HTTP is TLS** in deployed environments (NFR-S1 — operational, not localhost-only).

2. **Given** a **valid** refresh token  
   **When** `POST` **refresh** is called  
   **Then** a **new access token** is issued and **refresh rotation policy** is **documented in code** (comments on service/class): e.g. old refresh token invalidated, new refresh token returned.

3. **Given** **logout**  
   **When** `POST` **logout** is called  
   **Then** refresh token(s) for that session are **revoked or invalidated** per the same documented policy (e.g. row marked revoked; refresh cannot be reused).

4. **Given** **invalid** credentials or **revoked / expired** refresh  
   **When** auth endpoints are used  
   **Then** the API returns **`application/problem+json`** with stable machine **`code`**, **`type`** (URI), **`title`**, **`status`**, **`detail`**, **`instance`**, and **no** token material or passwords in **logs**.

## Tasks / Subtasks

- [x] **Schema + Liquibase** (AC: 2, 3)
  - [x] Add changeset (e.g. `0004-refresh-tokens.yaml`) under `apps/api/src/main/resources/db/changelog/changes/` for table **`refresh_tokens`** per architecture naming (`snake_case`, plural). Include: `id` (UUID PK), FK to **`users`** (`user_id`), **`token_hash`** (store **hash only** of opaque refresh token — never raw token), **`expires_at`** (timestamptz), **`revoked_at`** (nullable), **`created_at`**; indexes as needed (e.g. lookup by hash).
  - [x] Include from `db.changelog-master.yaml`.
- [x] **Domain + persistence** (AC: 2, 3)
  - [x] Add JPA entity `RefreshToken` (or equivalent) under `com.mowercare.model`; map columns explicitly; FK to `User`.
  - [x] Add `RefreshTokenRepository` with queries needed for **validate / revoke / rotate** (no raw token in logs).
  - [x] Extend **`UserRepository`** with **`Optional<User> findByOrganizationIdAndEmail(UUID organizationId, String email)`** (or equivalent) — login must resolve user within tenant; **normalize email** (trim + lower case) in the **service** layer to match **1.3** bootstrap behavior.
- [x] **JWT + crypto configuration** (AC: 1, 2)
  - [x] Add **`@ConfigurationProperties`** for JWT (issuer, access TTL, refresh TTL, signing secret or key material) — **env-driven** (e.g. `MOWERCARE_JWT_*` names documented in `.env.example` **names only**); **no** secrets in repo.
  - [x] Implement **access JWT** creation with claims required by **architecture** and **1.5** prep: at minimum **`sub`** (user id), **`organizationId`** (UUID string), **`role`** (string aligned with `UserRole`), **`exp`/`iat`; use standard claim names or document custom ones in OpenAPI.
  - [x] **Opaque refresh tokens:** generate cryptographically strong random values; **persist only** **hash** (e.g. SHA-256) in DB; return raw token to client once on login/refresh.
- [x] **Auth service + controller** (AC: 1–4)
  - [x] **Login:** verify password with existing **`PasswordEncoder`** bean against `User.passwordHash`; reject invalid org/user/password with **401** Problem Details (stable `code`, no hint which field failed).
  - [x] **Refresh:** validate hash, not revoked, not expired; apply **rotation**; return new access + refresh pair.
  - [x] **Logout:** revoke refresh token (and document whether single token vs family — MVP: revoking presented refresh is enough).
  - [x] REST paths under **`/api/v1/`** — e.g. `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` — **camelCase** JSON bodies/responses.
- [x] **Problem Details + errors** (AC: 4)
  - [x] Extend **`ApiExceptionHandler`** (or dedicated handlers) for auth exceptions with **RFC 7807** shape consistent with **1.3** (`type`, `title`, `status`, `detail`, `instance`, **`code`**).
  - [x] **Never** log refresh tokens, access tokens, or passwords.
- [x] **OpenAPI** (AC: 1)
  - [x] Add **springdoc-openapi** (or equivalent) compatible with **Spring Boot 4.x** parent in `pom.xml`; expose **OpenAPI 3** document for **auth** endpoints + shared error schema; document request/response DTOs.
- [x] **Security filter chain** (scope boundary)
  - [x] Keep **`SecurityConfig`** compatible with **1.5:** **auth routes** (`/api/v1/auth/**`) **permitAll**; **do not** yet require JWT on all `/api/v1/**` — global JWT + tenant enforcement is **Story 1.5**. Explicitly **permit** auth endpoints if you add any `authorizeHttpRequests` rules before 1.5.
- [x] **Tests** (AC: 1–4)
  - [x] **Integration tests** (Testcontainers + `AbstractPostgresIntegrationTest` / `MockMvc`): happy paths login → refresh → logout; invalid password; bad refresh; revoked refresh after logout; expired refresh if testable with clock override or short TTL in test profile.
  - [x] **Unit tests** where valuable: token service (hashing/claims), auth service with mocked repos, handler tests for new exception types — follow **`given…_when…_then…`** + **`@DisplayName`** pattern from **1.3**.

### Review Findings

- [x] [Review][Decision] Logout idempotency — **Resolved (B):** keep **401** when refresh is already revoked/expired; no idempotent **204** for repeat logout.

- [x] [Review][Patch] JWT signing — **Fixed:** use `NimbusJwtEncoder.withSecretKey(SecretKey)` (Spring Security 7) for HS256 instead of raw `JWKSource` + `OctetSequenceKey`, which failed JWK selection. [`JwtConfig.java`]

- [x] [Review][Patch] `AuthIT` Problem Details — **Fixed:** assert `$.code` (serialized shape) instead of `$.properties.code`. [`AuthIT.java`]

- [x] [Review][Patch] `AuthController.login` — **Fixed:** `consumes = APPLICATION_JSON`. [`AuthController.java`]

- [x] [Review][Patch] `JwtService` — **Fixed:** inject `Clock` for `iat`/`exp`. [`JwtService.java`]

- [x] [Review][Defer] [`AuthService.java`] — Concurrent refresh with the same token can yield multiple valid refresh rows before rotation completes; acceptable MVP scope.

- [x] [Review][Defer] [`AuthService.java` / global handler] — Unique `token_hash` violation on insert (theoretical collision) surfaces as generic persistence error; defer unless observed.

## Dev Notes

### Scope boundaries

- **In scope:** Login, refresh (with rotation), logout (revocation), JWT access issuance, refresh **storage** in `refresh_tokens`, **OpenAPI** for these endpoints, Problem Details, tests, `.env.example` **names** for JWT/refresh secrets.
- **Out of scope:** **Enforcing** JWT on **all** protected routes (Story **1.5**); mobile UI (1.6–1.7); **RBAC** beyond carrying **role** in JWT for future enforcement; **deactivated user** blocking (Epic 2 / FR26) — **document** a TODO hook if you load `User` by id (optional soft-check if column exists later).
- **Bootstrap:** `POST /api/v1/bootstrap/organization` remains separate; **no** duplication of bootstrap logic.

### Architecture compliance

- **REST + JSON:** `/api/v1/...`, camelCase; **Problem Details** for errors. [Source: `architecture.md` — API & communication patterns]
- **JWT claims:** Include **organization id** for tenant context in **1.5**. [Source: `architecture.md` — Cross-component dependencies]
- **Table name:** **`refresh_tokens`** (plural, snake_case). [Source: `architecture.md` — Naming patterns → Database]
- **Liquibase owns schema:** `ddl-auto` **none** / **validate**. [Source: `architecture.md` — Data architecture]
- **Layering:** Controller → service → repository; **no** DB in controllers. [Source: `architecture.md` — Persistence boundary]
- **Package layout (codebase reality):** Stories **1.2–1.3** use **type-based** packages (`controller`, `service`, `exception`, `config`, `model`, `request`/`response`, `repository`) — **continue this** for new auth types unless you are doing a deliberate repo-wide migration to feature packages (`auth/`) — **not** required by this story.

### Login identity (tenant + user)

- Users are unique per **`(organization_id, email)`** — **login request must include `organizationId`** and `email` and `password` (or an equivalent unambiguous tenant key). Do **not** assume a single global org in code without an explicit product decision; bootstrap may have created one org, but the API should remain **multi-tenant-correct**.

### Technical requirements

| Area | Requirement |
|------|-------------|
| API | Spring Boot **4.x**, **Java 25**, Maven — same as **1.1–1.3** |
| DB | PostgreSQL; new Liquibase file under `db/changelog/changes/` |
| Passwords | Verify with existing **`PasswordEncoder`** (BCrypt) — **no** custom password verification |
| Errors | `Content-Type: application/problem+json` with stable **`code`** |

### Library / framework requirements

- **`spring-boot-starter-security`** — already present; use for `PasswordEncoder` and filter chain configuration.
- **JWT signing:** Prefer **`spring-security-oauth2-jose`** **`JwtEncoder`** / Nimbus with **symmetric** or **asymmetric** key from config — **or** another well-supported library **pinned** in `pom.xml` with version; **avoid** unmaintained JWT stacks.
- **OpenAPI:** **springdoc-openapi** (webmvc) version compatible with **Spring Boot 4.0.x** — verify release notes when adding.

### File structure requirements (illustrative)

```
apps/api/src/main/java/com/mowercare/
  controller/
    AuthController.java
  service/
    AuthService.java
    JwtService.java            # or TokenService — issue/parse access JWT
    RefreshTokenService.java   # optional split if AuthService grows
  config/
    SecurityConfig.java        # update: permit /api/v1/auth/**
    JwtProperties.java         # @ConfigurationProperties
  model/
    RefreshToken.java
  model/request/
    LoginRequest.java
    RefreshRequest.java
    LogoutRequest.java
  model/response/
    TokenResponse.java         # accessToken, refreshToken, tokenType, expiresIn
  repository/
    UserRepository.java        # + findByOrganizationIdAndEmail
    RefreshTokenRepository.java
  exception/
    ApiExceptionHandler.java   # extend
    ... (auth-specific exceptions as needed)
apps/api/src/main/resources/db/changelog/changes/
  0004-refresh-tokens.yaml
apps/api/src/test/java/com/mowercare/
  controller/
    AuthIT.java                # or split ITs
  ...
```

### Testing requirements

- **`mvn verify`** green in CI.
- **IT:** Reuse **`AbstractPostgresIntegrationTest`** pattern; seed **Organization + User** via existing bootstrap endpoint **or** test fixtures (consistent with **1.3** tests — **do not** embed production secrets).
- **Assertions:** JSON paths for Problem Details (`$.properties.code` / `code` per Boot serialization); **never** assert full JWT in logs.
- **Naming:** `given…_when…_then…` + matching `@DisplayName` where **1.3** established that pattern.

### Previous story intelligence

- **1.3** delivered: `User` with `email`, `password_hash`, `role`; `POST /api/v1/bootstrap/organization`; `ApiExceptionHandler` with **urn:mowercare:problem:** `type` URIs; **`X-Bootstrap-Token`**; `MessageDigest.isEqual` for token compare; **email normalization** in `BootstrapService` — **mirror for login** lookup.
- **1.3** `UserRepository` is minimal — **extend** with `findByOrganizationIdAndEmail` (or query) for login.
- **SecurityConfig** is **permitAll** globally until **1.5** — **1.4** auth endpoints must **not** require a Bearer token.

### Git intelligence

- Recent API work: **1.3** bootstrap (org + admin + credentials); **1.2** schema — auth builds on **`User`** + **`PasswordEncoder`**.

### Latest tech notes (verify at implementation time)

- **Spring Boot 4** / **Spring Security 6.x:** Use supported dependency versions from BOM; **JwtEncoder** for access tokens is a common pattern.
- **Springdoc:** Pick a **Spring Boot 4–compatible** release; if a milestone is required, document in Dev Agent Record.

### Project context

- **`docs/project-context.md`** — not present; optional later (**Generate Project Context** skill).

### References

- `_bmad-output/planning-artifacts/epics.md` — Story 1.4 (acceptance criteria)
- `_bmad-output/planning-artifacts/architecture.md` — Authentication & security, JWT claims, `refresh_tokens`, API boundaries
- `_bmad-output/planning-artifacts/prd.md` — FR4–FR6
- `1-3-bootstrap-first-organization-and-admin-user.md` — bootstrap + User shape + exception patterns

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2) — bmad-dev-story workflow

### Debug Log References

- Spring Boot 4 + **springdoc-openapi-starter-webmvc-api** `3.0.0` for OpenAPI 3 (`/v3/api-docs`).
- JWT access tokens via **`NimbusJwtEncoder`** + **`JwtClaimsSet`** (`sub`, `organizationId`, `role`, `iat`/`exp`).

### Completion Notes List

- Liquibase `0004-refresh-tokens.yaml`: `refresh_tokens` with FK to `users`, unique `token_hash`, `idx_refresh_tokens_user_id`.
- `JwtProperties` + `JwtConfig` (`Clock`, `JwtEncoder` HS256), `JwtService`, `OpaqueTokenService` (SHA-256 hex for opaque refresh), `AuthService` with rotation + logout documented in class Javadoc.
- `AuthController`: `POST /api/v1/auth/login|refresh|logout`; `TokenResponse` (camelCase); springdoc `@OpenAPI` annotations on controller.
- `ApiExceptionHandler`: `AUTH_INVALID_CREDENTIALS`, `AUTH_REFRESH_INVALID`.
- `SecurityConfig`: explicit `permitAll` for `/api/v1/auth/**` (rest still `permitAll` until 1.5).
- IT: `AuthIT`; unit: `AuthServiceTest`, `ApiExceptionHandlerTest` extended; `AbstractPostgresIntegrationTest` sets JWT test properties; `BootstrapOrganizationIT` clears `refresh_tokens` first.
- `.env.example`: `MOWERCARE_JWT_*` variable names.

### File List

- `apps/api/pom.xml`
- `apps/api/src/main/java/com/mowercare/ApiApplication.java`
- `apps/api/src/main/java/com/mowercare/config/JwtConfig.java`
- `apps/api/src/main/java/com/mowercare/config/JwtProperties.java`
- `apps/api/src/main/java/com/mowercare/config/SecurityConfig.java`
- `apps/api/src/main/java/com/mowercare/controller/AuthController.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/main/java/com/mowercare/exception/InvalidCredentialsException.java`
- `apps/api/src/main/java/com/mowercare/exception/InvalidRefreshTokenException.java`
- `apps/api/src/main/java/com/mowercare/model/RefreshToken.java`
- `apps/api/src/main/java/com/mowercare/model/request/LoginRequest.java`
- `apps/api/src/main/java/com/mowercare/model/request/LogoutRequest.java`
- `apps/api/src/main/java/com/mowercare/model/request/RefreshRequest.java`
- `apps/api/src/main/java/com/mowercare/model/response/TokenResponse.java`
- `apps/api/src/main/java/com/mowercare/repository/RefreshTokenRepository.java`
- `apps/api/src/main/java/com/mowercare/repository/UserRepository.java`
- `apps/api/src/main/java/com/mowercare/service/AuthService.java`
- `apps/api/src/main/java/com/mowercare/service/JwtService.java`
- `apps/api/src/main/java/com/mowercare/service/OpaqueTokenService.java`
- `apps/api/src/main/resources/application.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/resources/db/changelog/changes/0004-refresh-tokens.yaml`
- `apps/api/src/test/java/com/mowercare/controller/AuthIT.java`
- `apps/api/src/test/java/com/mowercare/controller/BootstrapOrganizationIT.java`
- `apps/api/src/test/java/com/mowercare/exception/ApiExceptionHandlerTest.java`
- `apps/api/src/test/java/com/mowercare/service/AuthServiceTest.java`
- `apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java`
- `.env.example`

### Change Log

- 2026-03-30 — Story 1.4: JWT login/refresh/logout, `refresh_tokens` table, springdoc OpenAPI, Problem Details for auth, integration + unit tests.
- 2026-03-30 — Code review follow-up: `NimbusJwtEncoder.withSecretKey` for HS256; `AuthIT` assert `$.code`; login `consumes` JSON; `JwtService` uses `Clock`; logout stays strict 401 (decision B).

## Story validation report

_(Optional: run `bmad-create-story` validate after implementation planning.)_
