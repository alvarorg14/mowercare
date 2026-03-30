# Story 2.2: Invite or create employee user

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->
<!-- validate-create-story (checklist.md): passed with amendments 2026-03-30 — see Validation record at end. -->

## Story

As an **Org Admin**,
I want **to invite or create employee accounts for my organization**,
so that **my team can sign in without customer-facing signup (FR24, FR27)**.

**Implements:** FR24; FR27; NFR-PR1 (minimal PII in invites — document fields).

**Epic:** Epic 2 — Organization admin, roles & access control — builds on **Epic 1** (JWT `organizationId` + `role`, tenant checks) and **Story 2.1** (`RoleAuthorization`, `FORBIDDEN_ROLE`, `docs/rbac-matrix.md`, Admin-only stubs for admin actions).

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.2])

| Epics.md (authoritative) | Covered in this story by |
|---------------------------|-------------------------|
| **Given** Admin session **When** Admin creates/invites a user with email and role Technician or Admin **Then** user record is created in org scope; invite flow state (pending/accepted) is visible in API | Liquibase + `User` (or equivalent) lifecycle fields; **Admin-only** `POST` (create and/or invite); **GET** (list or get) exposing **status**; integration tests |
| **Given** the invitation or account creation **When** completed **Then** no end-customer portal exists and copy remains employee-only | API error messages and OpenAPI descriptions state **employee** context; no public self-registration routes; optional assertion in IT that bootstrap remains the only org-creation path |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **2.3 Assign roles** | 2.2 sets initial **role** at provisioning. **Changing** role after the fact is **2.3** — do not implement PATCH role here unless you intentionally collapse scope (prefer not). |
| **2.4 Deactivate** | **Out of scope** — no `active`/`deactivated` flag required for 2.2 unless you introduce a shared enum early; if you add `UserStatus` only for invite lifecycle, keep deactivate semantics for 2.4. |
| **2.5 Admin settings UI** | **Out of scope** — API + tests + docs + OpenAPI; mobile/settings UI is **2.5**. |
| **2.6 Employee-only guardrails** | Reinforce **employee** wording; full guardrail tests may land in **2.6**. |

## Acceptance Criteria

1. **Given** an authenticated **Admin** JWT for organization **O**  
   **When** they `POST` a **create** (or **invite**) request for **O** with **email** and **role** `ADMIN` or `TECHNICIAN`  
   **Then** a **user row** is persisted **scoped to** `organization_id = O`, **email** normalized with the **same rules** as login (`trim` + lower-case per locale — today implemented inside [Source: `AuthService.java`] `login`); **refactor** a small shared normalizer (e.g. `EmailNormalization`) or delegate from `AuthService` so provisioning and login cannot drift), **role** set, and **no cross-tenant** access (tenant check **before** `requireAdmin` — same order as [Source: `OrganizationProfileController.java`] `patchProfile`).

2. **Given** **create** semantics (admin provisions a **known** password)  
   **When** the request includes a valid **initial password** (or your chosen field name)  
   **Then** the user can **authenticate** via existing `POST /api/v1/auth/login` with the same **organizationId**, **email**, and **password** (password stored with existing [Source: `PasswordEncoder` / BCrypt]).

3. **Given** **invite** semantics (no initial password yet)  
   **When** the request does **not** establish a password (or explicitly selects invite mode)  
   **Then** the user record exists with **pending** invite state **and** the user **cannot** complete login until the invite is accepted (or equivalent) — **AuthService** must reject login with **401** `AUTH_INVALID_CREDENTIALS` (same as wrong password) **or** a documented dedicated code if you add a **clear** branch (prefer **not** leaking whether email exists). Document the chosen behavior in OpenAPI + `docs/`.

4. **Given** invite flow  
   **When** an invite is **accepted** (password set through your **accept-invite** or **complete-registration** endpoint)  
   **Then** status transitions to **accepted/active** and subsequent **login** succeeds per AC2.

5. **Given** **visibility** for admins  
   **When** a **GET** (list and/or single user) on `/api/v1/organizations/{organizationId}/...` runs  
   **Then** responses include **invite state** using a **single** clear lifecycle (recommended MVP: **`PENDING_INVITE`** until password is set, then **`ACTIVE`** — avoid a third `ACCEPTED` unless it adds meaning) and **do not** expose **password hashes** or **raw invite tokens** in list endpoints; if a token is returned **once** on create, document security expectations (single-use, admin-only channel).

6. **Given** duplicate **email** within the same organization  
   **When** a second create/invite for the same normalized email is attempted  
   **Then** API returns **409** `application/problem+json` with a **stable `code`** (e.g. **`USER_EMAIL_CONFLICT`**) — add handler in [Source: `ApiExceptionHandler.java`] following existing `TYPE_*` + `code` pattern.

7. **Given** a **Technician** JWT  
   **When** they call any admin create/invite/list endpoint **for their org**  
   **Then** **403** `FORBIDDEN_ROLE` — **same** pattern as [Source: `docs/rbac-matrix.md` — User admin row].

8. **Given** **OpenAPI** (`/v3/api-docs`)  
   **When** routes are registered  
   **Then** they appear with **bearer-jwt**, **401/403/409** where applicable, **employee**-oriented summaries, and **PII** fields (email) documented per NFR-PR1 (minimal fields — **email** + role + status; no unnecessary profile fields).

## Tasks / Subtasks

- [x] **Data model + Liquibase** (AC: 1–5)
  - [x] Add lifecycle state for **invite vs active** (e.g. `account_status` enum column on `users`, or separate `invitations` table — **prefer** extending `users` if invite is always 1:1 with a future login identity).
  - [x] For **invite**: store **hashed** invite token + **expiry** (or one-time token table) if you need an unauthenticated **accept** endpoint; never persist plaintext tokens.
  - [x] New Liquibase changelog under [Source: `apps/api/src/main/resources/db/changelog/changes/`] + master include; **no** Flyway.

- [x] **Admin API** (AC: 1, 5–8)
  - [x] `POST /api/v1/organizations/{organizationId}/users` (or `/employees` — **pick one** and **stick** to REST naming; document in OpenAPI).
  - [x] Optional `GET` list + `GET` by id for **Admin** only — needed for “visible in API” (AC5).
  - [x] `TenantPathAuthorization.requireJwtOrganizationMatchesPath` then `RoleAuthorization.requireAdmin` on every method.
  - [x] Request/response DTOs with **Bean Validation** (`@Email`, `@NotNull`, role enum).

- [x] **Invite acceptance** (AC: 3–4)
  - [x] Unauthenticated endpoint (e.g. `POST /api/v1/auth/accept-invite`) **or** `organizationId` + token + new password — **must** validate token, expiry, and single-use if applicable.
  - [x] Transition `PENDING` → `ACTIVE` and set **password hash** via `PasswordEncoder`.

- [x] **Auth alignment** (AC: 3)
  - [x] `AuthService.login` **must** refuse users in **pending invite** state (unless you allow password login only after accept — same as “cannot login until accepted”).

- [x] **Conflict + validation** (AC: 6)
  - [x] Map unique constraint on `(organization_id, email)` to **409** with stable `code` (or `DataIntegrityViolationException` handler — **don’t** leak stack traces).

- [x] **Docs + RBAC matrix** (AC: 7–8)
  - [x] Update [Source: `docs/rbac-matrix.md`] — replace “Epic 2.2+ (not implemented here)” with concrete routes + Admin-only.
  - [x] Short note in `docs/` or story completion: **PII** fields stored for invites (**email**, timestamps) — **NFR-PR1**.

- [x] **Tests** (AC: 1–8)
  - [x] Integration tests: Admin → create user → login; Admin → invite → cannot login until accept → accept → login; Technician → 403; wrong tenant → `TENANT_ACCESS_DENIED`; duplicate email → 409; OpenAPI/Swagger presence optional smoke or manual checklist for AC8.
  - [x] Reuse JWT fixtures from [Source: `RbacEnforcementIT.java`] / [Source: `OrganizationProfileIT.java`].

- [x] **CI** — `apps/api` tests green.

### Review Findings

- [x] [Review][Patch] Duplicate org email detection — **fixed:** `DataIntegrityViolations` uses Hibernate `ConstraintViolationException.getConstraintName()` with message fallback [`DataIntegrityViolations.java`; `ApiExceptionHandler.java`; `OrganizationUserService.java`]
- [x] [Review][Patch] Non-duplicate `DataIntegrityViolationException` — **fixed:** `log.warn("Unhandled data integrity violation", ex)` before 500 [`ApiExceptionHandler.java`]
- [x] [Review][Patch] `GET /users/{userId}` 404 — **fixed:** `ResourceNotFoundException` + Problem Details `NOT_FOUND` [`OrganizationUsersController.java`; `ApiExceptionHandler.java`]
- [x] [Review][Patch] Missing organization in `createUser` — **fixed:** `ResourceNotFoundException` (404 Problem Details) [`OrganizationUserService.java`]
- [x] [Review][Patch] AC7 Technician **POST** `/users` — **fixed:** `givenTechnician_whenCreateUser_thenForbidden` in [`OrganizationUsersIT.java`]
- [x] [Review][Defer] AC8 automated OpenAPI presence smoke not implemented — story allows optional manual checklist — deferred, scope choice [`2-2-invite-or-create-employee-user.md`]

## Dev Notes

### Scope boundaries

- **In scope:** Org-scoped **create** + **invite** + **visible state** + **accept** path + **Liquibase** + **OpenAPI** + **matrix** + **IT** coverage.
- **Out of scope:** **SMTP** / email delivery (no requirement to send mail in 2.2); **mobile UI**; **role change** after create (2.3); **deactivate** (2.4); **audit** table for deactivation (2.4 mentions audit) — **optional** minimal “created_by” only if you need it for support; otherwise **YAGNI**.

### Architecture compliance

- **Package layout:** Architecture doc suggests `user/` package [Source: `_bmad-output/planning-artifacts/architecture.md` — Project Structure]. The **current** codebase uses **flat** `com.mowercare.controller`, `service`, `model` (see 2.1). **Either** introduce `com.mowercare.user.*` and migrate gradually **or** stay flat for consistency with **2.1** — **do not** mix styles in one PR without reason.
- **Tenant first:** [Source: `TenantPathAuthorization.java`].
- **Errors:** RFC 7807 Problem Details + stable **`code`** — [Source: `architecture.md` — Format patterns].
- **Authorization:** [Source: `RoleAuthorization.java`].

### Existing code to reuse

| Asset | Location |
|-------|----------|
| Admin gate | `RoleAuthorization.requireAdmin` |
| Tenant gate | `TenantPathAuthorization.requireJwtOrganizationMatchesPath` |
| Email normalization | **Must** share one implementation with `AuthService.login` — `normalizeEmail` is **private** today; **extract** (e.g. `EmailNormalization` in `common/` or package-local util) **before** duplicating rules in a new service |
| Password hashing | `PasswordEncoder` bean [Source: `SecurityConfig.java`] |
| User persistence | `User`, `UserRepository`, `UserRole` |
| JWT issuance | `JwtService` — unchanged for 2.2 |

### Technical requirements

| Area | Requirement |
|------|-------------|
| Role at create | **ADMIN** or **TECHNICIAN** only (MVP); **validate** enum. |
| Invite token | Cryptographically random; store **hash** only; **TTL** (e.g. 7–14 days) documented in OpenAPI. |
| Email uniqueness | DB constraint already [Source: `User` / `uq_users_organization_id_email`] — surface **409** on conflict. |
| Employee-only copy | Error messages and OpenAPI text must **not** imply customer signup. |

### Library / framework requirements

| Layer | Notes |
|-------|-------|
| API | Spring Boot 3.x, existing JPA + Security — **no** new auth stack. |
| Tests | JUnit 5, `@SpringBootTest`, existing security test patterns. |

### File structure requirements

| Area | Guidance |
|------|----------|
| Controllers | `apps/api/src/main/java/com/mowercare/controller/` (or new `user/` subpackage per architecture) |
| Services | `apps/api/src/main/java/com/mowercare/service/` |
| Liquibase | `apps/api/src/main/resources/db/changelog/changes/` |
| Tests | `apps/api/src/test/java/com/mowercare/controller/` or `.../integration/` mirroring existing IT layout |

### Testing requirements

- **Integration tests** over **unit-only** for HTTP + Problem Details **`code`** + tenant + role.
- Cover **positive** Admin paths and **negative** Technician + duplicate email.

### Previous story intelligence (2.1)

- **RBAC matrix** at `docs/rbac-matrix.md` — **must** be updated when routes ship.
- **Technician** must **not** hit Admin-only user management routes — mirror `RbacEnforcementIT` patterns.
- **`FORBIDDEN_ROLE`** / **`TENANT_ACCESS_DENIED`** — keep **consistent** with existing handlers.
- **Issue stubs** (`IssueStubController`) are unrelated — **do not** refactor for 2.2.

### Git intelligence (recent commits)

- **`a9bd531`** — Story 2.1: `RoleAuthorization`, `IssueStubController`, `docs/rbac-matrix.md`, `RbacEnforcementIT`.
- **`b223548`** — Org profile API + mobile; reference for **IT** + Problem Details.

### Latest tech notes

- Spring Security 6 + OAuth2 resource server JWT unchanged.
- **`DataIntegrityViolationException`:** map `uq_users_organization_id_email` to **409** in `ApiExceptionHandler` or catch in service; align `title`/`detail` with existing Problem Detail style.

### Project context reference

- No **`project-context.md`** in repo; rely on **`architecture.md`** + this story.

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

_(none)_

### Completion Notes List

- Added `AccountStatus` (`PENDING_INVITE` / `ACTIVE`), `users.account_status`, `invite_token_hash`, `invite_expires_at` (Liquibase `0005`), unique `invite_token_hash`.
- Shared `EmailNormalization` + `AuthService.login` blocks pending invites; `OrganizationUserService` handles create/invite, list/get, accept-invite (opaque token hashed like refresh tokens).
- `POST /api/v1/organizations/{organizationId}/users`, `GET` list + by id, `POST /api/v1/auth/accept-invite`; Problem Details `USER_EMAIL_CONFLICT` (409), `INVITE_TOKEN_INVALID` (400); `DataIntegrityViolationException` mapped for duplicate email race.
- Docs: `docs/rbac-matrix.md`, `docs/account-data-pii.md`. `mvn test` in `apps/api` passed.

### File List

- `apps/api/src/main/java/com/mowercare/common/EmailNormalization.java`
- `apps/api/src/main/java/com/mowercare/model/AccountStatus.java`
- `apps/api/src/main/java/com/mowercare/model/User.java`
- `apps/api/src/main/java/com/mowercare/model/request/AcceptInviteRequest.java`
- `apps/api/src/main/java/com/mowercare/model/request/CreateEmployeeUserRequest.java`
- `apps/api/src/main/java/com/mowercare/model/response/CreateEmployeeUserResponse.java`
- `apps/api/src/main/java/com/mowercare/model/response/EmployeeUserResponse.java`
- `apps/api/src/main/java/com/mowercare/config/InviteProperties.java`
- `apps/api/src/main/java/com/mowercare/ApiApplication.java`
- `apps/api/src/main/java/com/mowercare/repository/UserRepository.java`
- `apps/api/src/main/java/com/mowercare/service/AuthService.java`
- `apps/api/src/main/java/com/mowercare/service/OrganizationUserService.java`
- `apps/api/src/main/java/com/mowercare/controller/OrganizationUsersController.java`
- `apps/api/src/main/java/com/mowercare/controller/AuthController.java`
- `apps/api/src/main/java/com/mowercare/exception/UserEmailConflictException.java`
- `apps/api/src/main/java/com/mowercare/exception/InviteTokenInvalidException.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/main/resources/db/changelog/changes/0005-user-account-status-and-invite.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/resources/application.yaml`
- `apps/api/src/test/java/com/mowercare/controller/OrganizationUsersIT.java`
- `apps/api/src/test/java/com/mowercare/service/AuthServiceTest.java`
- `docs/rbac-matrix.md`
- `docs/account-data-pii.md`

## Change Log

- **2026-03-30:** Story created (create-story workflow).
- **2026-03-30:** Implemented Story 2.2 employee invite/create API, Liquibase, tests, docs.

---

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-30 against `epics.md` Story 2.2, `architecture.md` (user admin, Liquibase, Problem Details), `AuthService` / `OrganizationProfileController`, and checklist.md gap analysis.

### Critical / traceability (addressed in file)

- **Email normalization:** Story previously cited `AuthService.normalizeEmail` as a [Source] even though it is **private** — amended AC1 and “Existing code to reuse” to require a **shared** normalizer so login and provisioning cannot diverge.
- **Lifecycle enum wording:** AC5 listed `PENDING_INVITE` / `ACTIVE` / `ACCEPTED` — amended to recommend **two** MVP states to avoid ambiguous “accepted vs active.”
- **Test scope:** Tasks referenced AC 1–5 only — amended to **AC 1–8** so conflict (6), Technician (7), and OpenAPI (8) are explicitly in test/verification scope.

### Enhancements applied

- Pinned controller ordering reference to **`patchProfile`** (tenant then Admin), matching [Source: `OrganizationProfileController.java`].
- OpenAPI verification called out (manual or light smoke) for AC8.

### Residual risks (acceptable for ready-for-dev)

- **SMTP** still out of scope; “invite” is API/token acceptance, not email delivery.
- **409 handler:** Implementer must add `urn:mowercare:problem:USER_EMAIL_CONFLICT` (or chosen URI) **consistent** with existing `TYPE_*` constants in [Source: `ApiExceptionHandler.java`].
