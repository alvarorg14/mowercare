# Story 2.1: Role model and RBAC enforcement hooks

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->
<!-- validate-create-story (checklist.md): passed with amendments 2026-03-30 — see Validation record at end. -->

## Story

As a **system**,
I want **Admin vs Technician permissions defined and enforced consistently on the API and documented for mobile**,
so that **FR8–FR10 and FR9 are implementable without ambiguity** and the architecture gap **“RBAC matrix”** is closed.

**Implements:** FR8, FR9, FR10 (baseline); closes architecture gap “RBAC matrix”.

**Epic:** Epic 2 — Organization admin, roles & access control — **first story** in epic (no prior Epic 2 story file). Builds on **Epic 1** (JWT `organizationId` + `role` claims, tenant checks, `FORBIDDEN_ROLE` for org profile PATCH).

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.1])

| Epics.md (authoritative) | Covered in this story by |
|---------------------------|-------------------------|
| **Given** a documented permission matrix (repo or OpenAPI) **When** issue/admin endpoints implemented or stubbed **Then** Spring Security annotations **and/or** explicit checks enforce Admin vs Technician **for each operation** | AC 1–2, Tasks: matrix doc + `RoleAuthorization` + per-route checks on stubs and refactored org profile |
| **Given** unit/integration tests **When** Technician calls Admin-only endpoint **Then** 403 Problem Details; **when** Admin calls Technician-allowed issue endpoints **Then** behavior matches matrix | AC 3–4, Tasks: `RbacEnforcementIT` (Technician → 403; **both** Admin and Technician → 2xx on shared stub; Admin must not get 403 on Technician-allowed operations) |

## Acceptance Criteria

1. **Given** a **documented permission matrix** (table in repo **and/or** OpenAPI `description` on secured operations)  
   **When** a developer reads `docs/` or `/v3/api-docs`  
   **Then** **Admin** vs **Technician** capabilities are explicit for **organization/admin actions** and **issue** actions at MVP level (aligned with [Source: `_bmad-output/planning-artifacts/prd.md` — RBAC matrix]).

2. **Given** **issue** and **admin** HTTP surfaces are **implemented or stubbed** under `/api/v1/organizations/{organizationId}/...`  
   **When** each operation runs  
   **Then** **tenant scoping** runs first (**`TenantPathAuthorization.requireJwtOrganizationMatchesPath`**) **and** **role rules** are enforced with **shared helpers** (avoid copy-pasting `jwt.getClaimAsString("role")` string compares in every controller — see [Source: `OrganizationProfileController.java`]).

3. **Given** **integration tests** (pattern: `@SpringBootTest`, JWT fixtures like **`TenantScopeIT`** / **`OrganizationProfileIT`**)  
   **When** a **Technician** JWT calls an **Admin-only** stub or endpoint  
   **Then** response is **403** `application/problem+json` with stable **`code`** (e.g. **`FORBIDDEN_ROLE`**) consistent with [Source: `ApiExceptionHandler.java`].

4. **Given** the same tests  
   **When** an **Admin** JWT calls an operation that the matrix marks as **allowed for both** roles (e.g. a read-only or shared “issue list stub”)  
   **Then** response is **success** (e.g. **200**), not **403**.

5. **Given** **OpenAPI** (`/v3/api-docs`)  
   **When** new stub routes are added  
   **Then** they appear with **bearer-jwt** security, documented **401/403** where applicable, and summaries reference the matrix or link to the doc.

## Tasks / Subtasks

- [x] **RBAC matrix artifact** (AC: 1, 5)
  - [x] Add **`docs/rbac-matrix.md`** (or **`docs/authorization/rbac-matrix.md`**) with a concrete MVP table: rows = **operations** (org profile GET/PATCH, representative issue list/create, **future** user-admin routes called out as “Epic 2+”), columns = **Admin / Technician** (**Allow / Deny**). Include one-line pointer to PRD table for product intent [Source: `prd.md` § RBAC matrix].
  - [x] Optionally add a **short** “Mobile” subsection: client uses JWT **`role`** claim + feature gating later; **no** secrets in app — [Source: `architecture.md` — Mobile / API patterns].

- [x] **Centralize role checks** (AC: 2)
  - [x] Introduce **`com.mowercare.security.RoleAuthorization`** (or equivalent) with methods such as **`requireRole(Jwt jwt, UserRole required)`** and **`requireAdmin(Jwt jwt)`**, throwing **`ForbiddenRoleException`** to match **`OrganizationProfileController`** behavior.
  - [x] Refactor **`OrganizationProfileController`** PATCH path to use the helper (behavior unchanged — regression-safe).

- [x] **Stub endpoints** (AC: 2, 4, 5)
  - [x] Add a small **`IssueStubController`** (name flexible) under **`/api/v1/organizations/{organizationId}/issues`** with **`@Tag`** (e.g. **Issues** or **Issues (stub)**) so Swagger groups correctly — [Source: existing controllers use `@Tag` on `OrganizationProfileController`].
  - [x] **Minimal** stubs, e.g.:
    - **`GET .../issues`** → **200** with empty list or placeholder JSON — **both** **ADMIN** and **TECHNICIAN** allowed (tenant + authenticated).
    - **`POST .../issues`** (stub body ignored or validated trivially) → **201** or **200** placeholder — **both** roles allowed (aligns with FR11 direction for future Epic 3).
    - **One Admin-only** stub, e.g. **`POST .../issues/_admin/reassign`** or **`/users/_stub`** → **204**/**200** — **ADMIN** only; **TECHNICIAN** → **403** **`FORBIDDEN_ROLE`**.
  - [x] Keep stubs **read-only** on DB if possible (no issue tables yet — Epic 3); no Flyway — **Liquibase** only when schema is needed.

- [x] **Tests** (AC: 3, 4)
  - [x] New IT class (e.g. **`RbacEnforcementIT`**) covering: Technician → Admin-only stub **403**; **Admin** → shared Technician-allowed stub **2xx**; Technician → same shared stub **2xx**; wrong path `organizationId` → **403** **`TENANT_ACCESS_DENIED`** before role logic.
  - [x] Reuse JWT issuance patterns from **`OrganizationProfileIT`** / **`AuthIT`**.
  - [x] Optional: **unit tests** for **`RoleAuthorization`** pure helpers (epics allow **unit/integration**); IT remains mandatory for HTTP + Problem Details **`code`**.

- [x] **CI** — `apps/api` tests green; formatting consistent with existing Java style.

### Review Findings

- [x] [Review][Patch] Add `RbacEnforcementIT` covering wrong `organizationId` on `POST .../issues/_admin/reassign` (`TENANT_ACCESS_DENIED`), symmetric with GET issues — [RbacEnforcementIT.java]
- [x] [Review][Defer] Same change set mixes sprint-status updates (Epic 1 closure, retrospective, Epic 2 story state) with RBAC implementation — [sprint-status.yaml] — deferred, PR hygiene / atomic commits
- [x] [Review][Defer] `docs/rbac-matrix.md` relative links into `_bmad-output` may break if docs are published outside the repo tree — [docs/rbac-matrix.md] — deferred until a docs site exists

## Dev Notes

### Scope boundaries

- **In scope:** Documented matrix, shared enforcement hooks, **stub** routes proving the matrix, tests, OpenAPI visibility.
- **Out of scope:** Real issue aggregate, persistence, mobile UI (Epic 3); user invite APIs (2.2); changing JWT shape (claims already set in [Source: `JwtService.java`]).

### Architecture compliance

- **Authorization:** [Source: `architecture.md` — Authentication & security] — Spring Security JWT + explicit checks; **`@PreAuthorize`** optional if you enable method security — if you add it, document in story completion notes; otherwise **imperative helpers** are consistent with current codebase.
- **Tenant first:** Always **`TenantPathAuthorization`** before role — [Source: `TenantPathAuthorization.java`].
- **Errors:** RFC 7807 Problem Details + stable **`code`** — [Source: `architecture.md` — Format patterns].

### Existing code to reuse

| Asset | Location |
|-------|----------|
| `UserRole` enum | `com.mowercare.model.UserRole` — `ADMIN`, `TECHNICIAN` |
| JWT `role` claim | `JwtService.issueAccessToken` — `role.name()` |
| Tenant match | `TenantPathAuthorization.requireJwtOrganizationMatchesPath` |
| 403 role mapping | `ForbiddenRoleException` + `ApiExceptionHandler.forbiddenRole` — detail: *“This action requires the {ROLE} role.”*; top-level **`code`:** **`FORBIDDEN_ROLE`**; **`type`:** `urn:mowercare:problem:FORBIDDEN_ROLE` |
| Prior art (Admin vs Technician) | `OrganizationProfileController.patchProfile` |

### Technical requirements

| Area | Requirement |
|------|-------------|
| Role claim parsing | Parse with **`UserRole.valueOf(claim)`** inside try/catch or switch on known names; **unknown** string → do not throw raw `IllegalArgumentException` to clients — map to **403** **`FORBIDDEN_ROLE`** (caller lacks valid role for action) **or** **401** **`AUTH_INVALID_TOKEN`** if treating malformed issuer claims as bad token — **prefer 403** when JWT is otherwise valid (subject/org present) so clients get a consistent forbidden story. **Missing/blank** `role` after tenant pass → align with **`InvalidAccessTokenClaimsException`** (**401** **`AUTH_INVALID_TOKEN`**) if you centralize “required claims” validation; document the chosen rule in **`RoleAuthorization` Javadoc**. |
| Matrix doc | Single source of truth in **`docs/`**; OpenAPI summaries stay short and reference it. |
| Stubs | No phantom dependencies; delete or evolve in Epic 3 when real controllers land. |

### Library / framework requirements

| Layer | Notes |
|-------|-------|
| API | Spring Boot 3.x, existing OAuth2 resource server JWT stack — no new auth library. |
| Tests | JUnit 5, Spring Boot test, existing security test utilities. |

### File structure requirements

| Area | Guidance |
|------|----------|
| New security helper | `apps/api/src/main/java/com/mowercare/security/RoleAuthorization.java` |
| Stub controller | `apps/api/src/main/java/com/mowercare/controller/` — feature-named class |
| Docs | `docs/rbac-matrix.md` (create `docs/` if absent) |
| Tests | `apps/api/src/test/java/com/mowercare/controller/` or `.../integration/` mirroring existing IT layout |

### Testing requirements

- **Integration tests** over **unit-only** for RBAC — must assert HTTP status + Problem Details **`code`**.
- Cover **positive** and **negative** paths for **both** roles on the **shared** stub.

### Previous story intelligence (Epic 1 — 1.8)

- **Org profile PATCH** already enforces **Admin** via manual role check; **2.1** should **centralize** that pattern, not duplicate new one-off checks for each Epic 2/3 controller.
- **`FORBIDDEN_ROLE`** and **`TENANT_ACCESS_DENIED`** are established — keep **consistent** titles/detail shapes.

### Git intelligence (recent commits)

- **`b223548`** — Organization profile API + mobile org screen: patterns for **IT**, **Problem Details**, **tenant + role**.

### Latest tech notes

- Spring Security 6 resource server JWT: continue using **`@AuthenticationPrincipal Jwt`**; method security requires **`@EnableMethodSecurity`** if you adopt **`@PreAuthorize`** — only add if justified.

### Project context reference

- No **`project-context.md`** in repo; rely on **`architecture.md`** + this story.

## Change Log

- **2026-03-30:** Implemented `RoleAuthorization`, `IssueStubController`, `docs/rbac-matrix.md`, `RbacEnforcementIT`, `RoleAuthorizationTest`; refactored org profile PATCH to use `requireAdmin`. Full `mvn test` in `apps/api` passed.

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

_(none)_

### Completion Notes List

- RBAC matrix documented at `docs/rbac-matrix.md` with org/issue stub operations and mobile pointer.
- `RoleAuthorization.requireRole` / `requireAdmin` centralizes JWT `role` parsing: blank/missing → `InvalidAccessTokenClaimsException`; mismatch or unknown enum string on check → `ForbiddenRoleException(required)`.
- Issue stubs: `GET/POST .../issues` for both roles; `POST .../issues/_admin/reassign` Admin-only; no DB writes for issues.
- `OrganizationProfileIT` behavior preserved (PATCH still 403 for Technician via shared helper).

### File List

- `docs/rbac-matrix.md`
- `apps/api/src/main/java/com/mowercare/security/RoleAuthorization.java`
- `apps/api/src/main/java/com/mowercare/controller/IssueStubController.java`
- `apps/api/src/main/java/com/mowercare/model/response/IssueListStubResponse.java`
- `apps/api/src/main/java/com/mowercare/model/response/IssueStubCreatedResponse.java`
- `apps/api/src/main/java/com/mowercare/controller/OrganizationProfileController.java`
- `apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java`
- `apps/api/src/test/java/com/mowercare/security/RoleAuthorizationTest.java`

---

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-30 against `epics.md` Story 2.1, `architecture.md` auth/error patterns, and `ApiExceptionHandler` / `OrganizationProfileController` sources.

**Critical / traceability (addressed in file):**

- Linked epic-level **Given/When/Then** to concrete AC and tasks (including **Admin** success on Technician-allowed routes).
- Clarified **role claim** parsing and **401 vs 403** for missing/unknown role strings so implementers do not leak `valueOf` stack traces or pick ad hoc status codes.
- Documented **Problem Detail** shape for **`FORBIDDEN_ROLE`** to match existing handler.
- Added **@Tag**, optional **unit** tests for helpers, and explicit **tenant-before-role** IT case.

**Enhancements applied:** Swagger grouping, epic traceability table, optional `RoleAuthorization` unit tests.

**Residual risks (acceptable for ready-for-dev):**

- Real **issue** semantics (assignee scope, “Mine” queue) land in Epic 3; stubs only **prove** RBAC wiring.
- **`requireRole`** API may need a second parameter later (e.g. “any of”) — YAGNI for 2.1.
