# Story 2.3: Assign roles to employees

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->
<!-- validate-create-story (checklist.md): passed with amendments 2026-03-30 — see Validation record at end. -->

## Story

As an **Org Admin**,
I want **to assign each employee a role (Admin or Technician)**,
so that **permissions match responsibilities (FR7)**.

**Implements:** FR7.

**Epic:** Epic 2 — Organization admin, roles & access control — builds on **Story 2.1** (RBAC enforcement, `FORBIDDEN_ROLE`) and **Story 2.2** (`OrganizationUsersController`, `User.role`, `EmployeeUserResponse`, tenant + Admin gates).

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.3])

| Epics.md (authoritative) | Covered in this story by |
|---------------------------|-------------------------|
| **Given** Admin **When** they update a user’s role **Then** role persists and subsequent JWTs or server-side checks reflect the change within defined session policy | Persist `User.role`; **server-side** checks already load user/JWT from DB on login/refresh — document **staleness** of current access JWT; new role on **next** `login` / `refresh` per [Source: `AuthService.java`]; optional note for mobile refresh after change (UI in 2.5) |
| **Given** last Admin demotion is attempted **When** validation runs **Then** request fails with clear Problem Details (org must retain at least one Admin) | **409** with stable `code` (pinned in AC4) — **not** a silent no-op |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **2.2** | Initial **role** at create/invite is done. This story is **only** **changing** role after the user exists. Reuse `UserRole`, DTO patterns, and controller/service layout from 2.2. |
| **2.4 Deactivate** | **Out of scope** — no `active` flag yet; last-admin **count** uses **`User.role == ADMIN`** for all rows in the org (see Tasks). When **2.4** adds deactivated users, revisit whether inactive admins should be excluded from the count. |
| **2.5 Admin settings UI** | **Out of scope** — API + tests + `docs/rbac-matrix.md` + OpenAPI; mobile UI is **2.5**. |

## Acceptance Criteria

1. **Given** an authenticated **Admin** JWT for organization **O**  
   **When** they send an **update-role** request for user **U** in **O** with `role` **`ADMIN`** or **`TECHNICIAN`**  
   **Then** **`users.role`** for **U** is updated, **scoped** to `organization_id = O` (no cross-tenant writes); response **`200 OK`** with **`EmployeeUserResponse`** body (same shape as **GET** one user — email, role, account status, ids as today); **`403` `TENANT_ACCESS_DENIED`** if path org ≠ JWT org, **`403` `FORBIDDEN_ROLE`** if caller is not Admin — same order as [Source: `OrganizationUsersController.java`] (tenant then role).

2. **Given** a successful role update  
   **When** the affected user later calls **`POST /api/v1/auth/refresh`** or **`POST /api/v1/auth/login`**  
   **Then** the new **access** JWT includes **`role`** from persisted `User.role` ([Source: `JwtService.issueAccessToken`], [Source: `AuthService.java`] — refresh/login paths already read the `User` entity).

3. **Given** **session policy (staleness)**  
   **When** role changes while the user still holds a **non-expired access JWT**  
   **Then** **authorization for that token** continues to reflect the **claims issued at token creation** until expiry or successful **refresh** — **document** this explicitly in OpenAPI/story notes (aligns with epics “within defined session policy”; no global token revocation list required for 2.3).

4. **Given** exactly **one** user in org **O** has **`UserRole.ADMIN`**  
   **When** an Admin attempts to change that user’s role to **`TECHNICIAN`**  
   **Then** the API returns **RFC 7807** Problem Details with a **stable `code`** **`LAST_ADMIN_REMOVAL`** and **HTTP 409 Conflict** (same class of response as **`USER_EMAIL_CONFLICT`** — business rule blocks the transition); **detail** must state the org must keep **at least one** Admin.

5. **Given** a **Technician** JWT  
   **When** they call the role-update endpoint for **their** org  
   **Then** **`403` `FORBIDDEN_ROLE`** — same as other Admin-only user routes ([Source: `docs/rbac-matrix.md`]).

6. **Given** **user U** does not exist in org **O** (wrong id or other org)  
   **When** Admin calls the role-update endpoint  
   **Then** **`404`** with existing **`NOT_FOUND`** pattern ([Source: `OrganizationUsersController.getUser`], `ResourceNotFoundException`).

7. **Given** **OpenAPI** (`/v3/api-docs`)  
   **When** the route is registered  
   **Then** it appears with **bearer-jwt**, **200** (response schema aligned with **`EmployeeUserResponse`**), **401/403/404/409** as applicable, and **employee**-oriented summary.

## Tasks / Subtasks

- [x] **Domain + persistence** (AC: 1, 4)
  - [x] Add a **controlled** way to mutate `User.role` (e.g. package-private `setRole` on `User` or `updateRole(UserRole)` — avoid public Lombok `@Setter` on the whole entity if that weakens invariants).
  - [x] Add **`UserRepository`** query method, e.g. **`long countByOrganization_IdAndRole(UUID organizationId, UserRole role)`** (Spring Data derived query) — **required** for last-admin check; does **not** exist in [Source: `UserRepository.java`] today.
  - [x] **No Liquibase** unless you add an audit column (not required by epics for 2.3) — role column already exists from earlier stories.

- [x] **Admin API** (AC: 1–7)
  - [x] **`PATCH /api/v1/organizations/{organizationId}/users/{userId}`** (or **`PUT`** if you prefer full replacement — **prefer PATCH** for single-field update) with JSON body `{ "role": "ADMIN" | "TECHNICIAN" }` and Bean Validation; **`200`** + **`EmployeeUserResponse`** on success (AC1).
  - [x] **`TenantPathAuthorization.requireJwtOrganizationMatchesPath`** then **`RoleAuthorization.requireAdmin`** on the handler.
  - [x] Service: **`@Transactional`** — run **count** + **conditional update** in the **same** transaction so two concurrent demotions cannot both commit and leave **zero** admins (race on last-admin guard).
  - [x] Service: load user by `organizationId` + `userId`; if missing → throw **`ResourceNotFoundException`**.
  - [x] **Last-admin guard:** when transitioning **from** `ADMIN` **to** `TECHNICIAN`, if **`countByOrganization_IdAndRole(O, ADMIN) == 1`** and that row is the target user, throw mapped **`LAST_ADMIN_REMOVAL`** (AC4). Count includes **all** account statuses (e.g. **`PENDING_INVITE`**) — same `role` column; **2.4** deactivate may refine “who counts” later.

- [x] **Exception + handler** (AC: 4)
  - [x] Add a small domain exception (e.g. `LastAdminRemovalException`) **or** validate in service and map in [Source: `ApiExceptionHandler.java`] with new `TYPE_*` URI + `code` property — mirror **`USER_EMAIL_CONFLICT`** style.

- [x] **Docs** (AC: 5–7)
  - [x] Update [Source: `docs/rbac-matrix.md`] — replace “Epic 2.3–2.4 (not implemented here)” for PATCH role with **Allow Admin / Deny Technician** + route line.
  - [x] OpenAPI annotations on controller method; document JWT staleness in **description** (AC3).

- [x] **Tests** (AC: 1–6)
  - [x] Extend [Source: `OrganizationUsersIT.java`] (or parallel IT): Admin patches role → **200** + body matches **GET** user; affected user **`POST /auth/refresh`** (or **login**) → new access token’s **`role`** claim matches DB (parse JWT payload in test — `TokenResponse` does not echo `role`; use **`JwtDecoder`** from the test context or decode middle segment with **`ObjectMapper`**).
  - [x] Technician → **403** on PATCH.
  - [x] Last admin demotion → **409** + **`LAST_ADMIN_REMOVAL`**.
  - [x] Wrong `userId` / wrong org → **404** / tenant **403** as applicable.

- [x] **CI** — `apps/api` tests green.

## Dev Notes

### Scope boundaries

- **In scope:** Org-scoped **role change**, **last-admin** protection, **Problem Details**, **RBAC matrix**, **IT** coverage, JWT behavior **documentation**.
- **Out of scope:** **Revoking** all sessions on role change (2.4+); **mobile** UI; **audit** table for role changes (optional follow-up — not in epics for 2.3).

### Architecture compliance

- **Tenant first, then role:** [Source: `TenantPathAuthorization.java`], [Source: `RoleAuthorization.java`].
- **Errors:** RFC 7807 Problem Details + stable **`code`** — [Source: `architecture.md` — Format patterns].
- **Package layout:** Stay consistent with **2.1 / 2.2** flat `controller` / `service` / `model` unless a planned migration exists.

### Existing code to reuse

| Asset | Location |
|-------|----------|
| Admin + tenant gates | `TenantPathAuthorization`, `RoleAuthorization` |
| User loading | `UserRepository.findByOrganization_IdAndId` |
| Admin count | New **`countByOrganization_IdAndRole`** on [Source: `UserRepository.java`] for last-admin guard |
| Responses | `EmployeeUserResponse` — return from PATCH **200**; **list/get** already map `User.role` |
| JWT role claim | `JwtService.issueAccessToken` — **no change** if refresh/login already pass `user.getRole()` |

### Technical requirements

| Area | Requirement |
|------|-------------|
| Allowed roles | **`ADMIN`** and **`TECHNICIAN`** only — reuse **`UserRole`** enum. |
| Last-admin rule | Apply when transitioning **from** `ADMIN` **to** `TECHNICIAN` and **count(ADMIN in org) == 1** and that user is the target. |
| Concurrency | **`@Transactional`** + single transaction for count + update — prevents two requests from demoting the last two admins concurrently into zero admins. |
| Idempotency | **PATCH** to same role should succeed (**200**) — avoid accidental 409 on no-op. |

### Library / framework requirements

| Layer | Notes |
|-------|-------|
| API | Spring Boot 3.x, existing Security — **no** new auth stack. |
| Tests | JUnit 5, `@SpringBootTest`, existing patterns from `OrganizationUsersIT`. |

### File structure requirements

| Area | Guidance |
|------|----------|
| Controller | `OrganizationUsersController` — add PATCH mapping |
| Service | `OrganizationUserService` — add `updateUserRole(...)` |
| Request DTO | e.g. `UpdateEmployeeUserRoleRequest.java` with `@NotNull` on `role` |
| Tests | `OrganizationUsersIT` |

### Testing requirements

- Prefer **integration tests** asserting HTTP status + **`application/problem+json`** `code` + tenant/role negatives.
- **Refresh claim test:** After PATCH, call **`POST /api/v1/auth/refresh`** with refresh token from **`TokenResponse`** ([Source: `OrganizationUsersIT.java`] — extend helpers to capture **`refreshToken`** from login when testing refresh). Decode **`accessToken`** JWT and assert **`role`** claim — [Source: `JwtService.java`] sets claim name **`role`**.

### Previous story intelligence (2.2)

- **409 / stable codes:** Follow **`USER_EMAIL_CONFLICT`** handler pattern in [Source: `ApiExceptionHandler.java`].
- **404:** `ResourceNotFoundException` → **`NOT_FOUND`** already wired.
- **Duplicate-email / DataIntegrity:** N/A for role patch; do not reuse **`USER_EMAIL_CONFLICT`** for last-admin — use a **dedicated** `code`.
- **OpenAPI:** Story 2.2 deferred automated OpenAPI smoke — optional here too; manual verification acceptable for AC7.

### Git intelligence (recent commits)

- **`57f2692`** — Story 2.2: `OrganizationUsersController`, `OrganizationUserService`, invite/create flows — **extend** this controller/service for PATCH role.
- **`a9bd531`** — Story 2.1: `RoleAuthorization`, RBAC matrix baseline.

### Latest tech notes

- Spring Security OAuth2 JWT resource server unchanged.
- **No** need to version JWTs for 2.3; staleness is acceptable until refresh (AC3).

### Project context reference

- No **`project-context.md`** in repo; rely on **`architecture.md`** + this story.

## Dev Agent Record

### Agent Model Used

Cursor agent

### Debug Log References

_(none)_

### Completion Notes List

- Implemented **`PATCH /api/v1/organizations/{organizationId}/users/{userId}`** with **`UpdateEmployeeUserRoleRequest`**, **`LastAdminRemovalException`** → **409** `LAST_ADMIN_REMOVAL`, pessimistic lock on admin rows + count for last-admin guard, **`User.updateRole`**, repository **`countByOrganization_IdAndRole`** + **`lockByOrganizationIdAndRole`**. Updated **`docs/rbac-matrix.md`**. Integration tests: refresh **`role`** claim via **`JwtDecoder`**, two-admins demotion, last-admin, Technician, 404, tenant 403.

### File List

- `apps/api/src/main/java/com/mowercare/model/User.java`
- `apps/api/src/main/java/com/mowercare/model/request/UpdateEmployeeUserRoleRequest.java`
- `apps/api/src/main/java/com/mowercare/repository/UserRepository.java`
- `apps/api/src/main/java/com/mowercare/service/OrganizationUserService.java`
- `apps/api/src/main/java/com/mowercare/controller/OrganizationUsersController.java`
- `apps/api/src/main/java/com/mowercare/exception/LastAdminRemovalException.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/test/java/com/mowercare/controller/OrganizationUsersIT.java`
- `docs/rbac-matrix.md`

## Change Log

- **2026-03-30:** Code review: OpenAPI **401** on PATCH, idempotent PATCH IT, **409** `detail` assertion.
- **2026-03-30:** Story created (create-story workflow).
- **2026-03-30:** validate-create-story (checklist.md) — amendments applied; see Validation record.
- **2026-03-30:** Implemented Story 2.3 (PATCH role, last-admin, tests, docs); `mvn test` in `apps/api` passed.

---

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-30 against `epics.md` Story 2.3, `architecture.md` (Problem Details, tenant + RBAC), `AuthService` / `JwtService`, `UserRepository`, `OrganizationUsersIT`, and checklist.md gap analysis.

### Critical / traceability (addressed in file)

- **409 vs 422:** Pinned to **409 Conflict** + **`LAST_ADMIN_REMOVAL`** — aligns with existing **`USER_EMAIL_CONFLICT`** pattern and removes implementer ambiguity.
- **PATCH response contract:** Added **200** + **`EmployeeUserResponse`** — was unspecified; required for OpenAPI and client parity with GET.
- **`UserRepository`:** Documented new **`countByOrganization_IdAndRole`** — repository had no count method; last-admin guard is not implementable without it.
- **Race / last admin:** Added **`@Transactional`** and explicit **count + update** in one transaction — checklist regression risk for concurrent demotions.
- **JWT refresh test:** Clarified **`TokenResponse`** does not expose `role`; tests must decode JWT (`JwtDecoder` or parse) and align claim name **`role`** with [Source: `JwtService.java`].
- **Account status + last admin:** Clarified count includes **all** statuses with **`ADMIN`** role; **2.4** may refine later.

### Enhancements applied

- OpenAPI AC7 updated to include **200** + schema reference.
- Cross-story **2.4** note added for future deactivated-user exclusion from admin count.

### Residual risks (acceptable for ready-for-dev)

- **OpenAPI** automated smoke still optional (same as 2.2 story).
- **NFR-S5** auditability for role-change events not required by epics for 2.3 — optional follow-up.

### Review Findings

- [x] [Review][Patch] OpenAPI: add `@ApiResponse` for **401** on `PATCH .../users/{userId}` — AC7 calls for 401 where applicable; other routes document missing/invalid Bearer token. [`OrganizationUsersController.java` ~96–108]
- [x] [Review][Patch] Add an integration test that **PATCH** with the **same** role as the current user returns **200** (idempotency in Dev Notes / story).
- [x] [Review][Patch] Extend last-admin IT to assert Problem **`detail`** text matches AC4 (e.g. org must retain at least one Admin), not only `code` `LAST_ADMIN_REMOVAL`.
- [x] [Review][Defer] `lockByOrganizationIdAndRole` loads all Admin rows for pessimistic lock — acceptable MVP; revisit if orgs with very large admin sets become realistic. — deferred, design choice
