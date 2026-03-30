# Story 1.8: Organization profile read/update for Admin

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As an **Org Admin**,
I want **to view and edit my organization’s profile fields supported in MVP**,
so that **the team name is correct in the product (FR2)**.

**Implements:** FR2; partial FR8 (Admin-only update rule here; full RBAC matrix lands in Epic 2 story 2.1).

**Epic:** Epic 1 — Bootstrap, tenancy & authentication — follows **1.7** (real sign-in, `lib/api` Bearer + tenant-scope); **precedes** Epic 2 user-admin stories.

## Acceptance Criteria

1. **Given** an authenticated user whose JWT **`organizationId`** matches **`{organizationId}`** in the path  
   **When** they call **`GET /api/v1/organizations/{organizationId}/profile`**  
   **Then** the response is **200** with **camelCase** JSON including at least **`id`** (UUID) and **`name`** (string), **`createdAt`** / **`updatedAt`** as **ISO-8601 UTC with `Z`** per architecture  
   **And** the handler loads the org row scoped to that tenant (no cross-org reads).

2. **Given** an authenticated **Admin** (JWT **`role`** claim **`"ADMIN"`** — [Source: `JwtService.java` — `issueAccessToken` uses **`UserRole.name()`**])  
   **When** they call **`PATCH /api/v1/organizations/{organizationId}/profile`** with body **`{ "name": "<non-empty string within length limits>" }`**  
   **Then** the organization **`name`** persists; **200** with **JSON body** matching the **GET** profile shape (same DTO — no **204**-only success, to keep mobile parsing uniform)  
   **And** validation errors return **400** `application/problem+json` with **`code`:** `VALIDATION_ERROR` (existing handler — [Source: `ApiExceptionHandler.java`]).

3. **Given** an authenticated **Technician** (`role` **`"TECHNICIAN"`**)  
   **When** they call **`PATCH`** on the same path with a valid body  
   **Then** the API returns **403** `application/problem+json` with a **stable top-level `code`** **`FORBIDDEN_ROLE`** **and** `title`/`detail` suitable for clients — add **`@ExceptionHandler`** + `urn:mowercare:problem:...` **type** URI following existing Problem Detail patterns in `ApiExceptionHandler`.

4. **Given** path **`organizationId`** ≠ JWT **`organizationId`**  
   **When** GET or PATCH is invoked  
   **Then** behavior matches existing tenant boundary: **403** `TENANT_ACCESS_DENIED` (reuse **`TenantAccessDeniedException`** or equivalent check **before** role check — same as [Source: `TenantScopeController.java`]).

5. **Given** the **mobile** app with a **signed-in** session (**1.7** patterns)  
   **When** an **Admin** opens the **organization profile** screen  
   **Then** the screen **loads** current org name via **GET** using **`authenticatedFetchJson`** + **`getSessionOrganizationId()`** for the path segment; **saves** edits via **PATCH** using **TanStack Query** `useMutation` with **MutationFeedback** (pending → success / error + Retry) [Source: `ux-design-specification.md` — MutationFeedback].

6. **Given** a signed-in **Technician**  
   **When** they open the same screen (or org section under Settings)  
   **Then** they **see** org name (**GET** allowed) but **cannot** edit — **no** primary save for profile, or controls **disabled** with explanatory copy (align with UX-DR12 single primary where applicable).

7. **OpenAPI:** **`/v3/api-docs`** describes new endpoints under the existing **Organizations** tag (same area as **`TenantScopeController`**), security (**bearer-jwt**), request/response schemas, and response **codes**: **401** (missing/invalid Bearer), **200** success, **400** validation, **403** `TENANT_ACCESS_DENIED` or **`FORBIDDEN_ROLE`**, as applicable.

8. **Tests:** API integration tests cover **Admin PATCH success**, **Technician PATCH → 403**, **wrong org path → 403 tenant**, **validation → 400**, **GET without auth → 401**; existing **`TenantScopeControllerTest`** / **`TenantScopeIT`** patterns are a model for JWT setup.

## Tasks / Subtasks

- [x] **API — contract & persistence** (AC: 1–4, 7, 8)
  - [x] Add **`OrganizationProfileResponse`** (or reuse a small DTO) with camelCase JSON; **`OrganizationProfilePatchRequest`** with validated **`name`** (align with `@Column(length = 255)` — [Source: `Organization.java`]).
  - [x] Add controller under **`/api/v1/organizations/{organizationId}/profile`** (new class or extend org-scoped controller — keep **`TenantScopeController`** as the tenant probe; avoid duplicating tenant logic — extract a small **`TenantAssertions`** / method that matches **`TenantScopeController`** behavior).
  - [x] **GET:** `OrganizationRepository.findById` only after tenant match; return **404** vs **403** policy: if id is wrong tenant, **403** (not 404) to match tenant probe semantics.
  - [x] **PATCH:** After tenant match, require **`jwt.getClaimAsString("role")`** equals **`UserRole.ADMIN.name()`**; else throw mapped **403** Problem Detail.
  - [x] Register new exception type + **`ApiExceptionHandler`** entry with **`code`:** `FORBIDDEN_ROLE` (mirror **`TENANT_ACCESS_DENIED`** structure).
  - [x] **Liquibase:** no schema change if **`name`** already exists on **`organizations`** (Story 1.2) — confirm no migration needed.
- [x] **API — tests** (AC: 8)
  - [x] Integration tests with **`@SpringBootTest`** + security test helpers consistent with **`TenantScopeIT`** / **`AuthIT`** (issue JWT via login or test JWT builder if project uses one).
- [x] **Mobile — role + UI** (AC: 5, 6)
  - [x] Expose **`role`** from access JWT: extend **`lib/jwt-org.ts`** (or sibling) with **`getRoleFromAccessToken`:** returns **`ADMIN` | `TECHNICIAN` | null** — must match **`UserRole`** string names from API.
  - [x] Optional: store **role** in **`session.ts`** alongside org id when setting session (from login/refresh tokens) to avoid re-parsing on every render; keep **single source of truth** after token refresh.
  - [x] Add **Settings** (or **Organization**) screen route under **`app/(app)/`**: **RHF + Zod** for **`name`**; **`useQuery`** for GET; **`useMutation`** for PATCH; **Problem Details** for errors (**`ApiProblemError`** from **`lib/http.ts`**).
  - [x] On **PATCH** success: **`queryClient.invalidateQueries`** for the org-profile query key (or **`setQueryData`** from response) so **lists/headers** never show a stale name — [TanStack Query cache hygiene].
  - [x] **Navigation:** Minimal entry point from **`app/(app)/index.tsx`** (e.g. button “Organization” / “Settings”) — full tab IA (Issues / Notify / Settings) is **Epic 3+** per UX; **1.8** only needs a **reachable** screen.
  - [x] **CI:** `apps/mobile` **`npm run lint`** + **`npm run typecheck`**; **`apps/api`** tests pass.

## Dev Notes

### Scope boundaries

- **In scope:** Org **name** read/write for Admin; Technician **read-only**; tenant boundary + role check on API; mobile screen + honest mutation states.
- **Out of scope:** Full **Settings** tab bar, **user invites**, **role assignment** (Epic 2); **billing**; **logo** / extra profile fields beyond MVP **name** unless trivially added to same DTO (prefer **name-only** to limit scope).

### Architecture compliance

- **REST:** `/api/v1/organizations/{organizationId}/...` — **camelCase** path params and JSON [Source: `architecture.md` — Format patterns].
- **Errors:** RFC 7807 Problem Details, stable **`code`** top-level JSON field [Source: `architecture.md` — Format patterns → Errors].
- **Auth:** JWT **`organizationId`** + **`role`** claims [Source: `JwtService.java`].
- **Mobile:** TanStack Query + RHF + Zod + Paper; **no secrets** in repo; API base from **`getApiBaseUrl()`** [Source: `architecture.md` — Mobile structure].

### API shapes (concrete)

| Method | Path | Success | Notes |
|--------|------|---------|------|
| GET | `/api/v1/organizations/{organizationId}/profile` | **200** + profile JSON | Tenant must match JWT |
| PATCH | `/api/v1/organizations/{organizationId}/profile` | **200** + profile JSON (same shape as GET) | **Admin** only |

**PATCH body (MVP):** `{ "name": "string" }` — validate **not blank**, **max 255**.

### Problem Details (new)

| Status | `code` | When |
|--------|--------|------|
| 403 | `TENANT_ACCESS_DENIED` | Path org ≠ JWT org (existing) |
| 403 | `FORBIDDEN_ROLE` | Authenticated but **not Admin** on PATCH |
| 400 | `VALIDATION_ERROR` | Bean validation on PATCH body |

### UX-DR mapping

| ID | Intent |
|----|--------|
| UX-DR6 | MutationFeedback on PATCH |
| UX-DR9 | Map Problem Details to copy + Retry |
| UX-DR12 | Single primary **Save** for Admin edit |
| UX-DR13 | RHF + Zod aligned with API |

### Technical requirements

| Area | Requirement |
|------|-------------|
| Tenant check | Same logical rule as **`TenantScopeController.tenantScope`** — JWT `organizationId` must equal path param |
| Role check | **`UserRole.ADMIN.name()`** string compare to JWT `role` claim |
| Mobile HTTP | **`authenticatedFetchJson`** from **`lib/api.ts`** for GET/PATCH (inherits **401 → refresh** behavior from **1.7**) |
| Role in UI | If **`role`** is cached in **`session`**, update it whenever **`setSession`** runs after **login** or **refresh** so **`TECHNICIAN`** vs **`ADMIN`** never goes stale after token rotation |

### Library / framework requirements

| Layer | Notes |
|-------|--------|
| API | Spring MVC, **`@AuthenticationPrincipal Jwt`**, existing OAuth2 resource server |
| Mobile | Existing **Paper**, **TanStack Query v5**, **RHF**, **Zod**, **`lib/http`/`lib/api`** |

### File structure requirements (expected touchpoints)

```
apps/api/src/main/java/com/mowercare/
  controller/          # New org profile controller (or co-locate with tenant org package)
  model/request/       # Patch request DTO
  model/response/      # Profile response DTO
  exception/           # Optional: AdminOnlyException → 403 handler
  ... integration tests under src/test/...
apps/mobile/
  app/(app)/           # New screen route + link from home
  lib/jwt-org.ts       # (extend) role claim
  lib/auth/session.ts  # (optional) cache role
```

### Testing requirements

- **API:** Integration tests mandatory for authz matrix (Admin vs Technician vs wrong tenant).
- **Mobile:** Manual: Admin edits name → relaunch app → name still loaded; Technician sees name, cannot save.

### Previous story intelligence (1.7)

- **`authenticatedFetchJson`**, **`verifyTenantScope`**, **`getSessionOrganizationId`**, **`setSession`** after login/refresh — **reuse** for org id in URLs.
- **Do not** log tokens; **clear session** on refresh failure already handled in **`lib/api.ts`**.
- **Review learnings:** JWT **`organizationId`** for session must match token (**`getOrganizationIdFromAccessToken`**); tenant-scope probe pattern — profile GET should follow same org id source.
- **Deferred from 1.7:** fetch timeouts — still out of scope unless blocking.

### Git intelligence

- Recent work: **`413f944`** — mobile sign-in/sign-out/session (**1.7**); org profile builds on **`lib/api`** and session module.

### Latest tech notes

- Spring **`ProblemDetail#setProperty("code", ...)`** — already used project-wide; keep **consistent**.
- Mobile: prefer **`isPending`** on mutations (Query v5).

### Project context reference

- **`docs/project-context.md`** — not present; optional **Generate Project Context** after Epic 1 completes.

### References

- `_bmad-output/planning-artifacts/epics.md` — Story 1.8
- `_bmad-output/planning-artifacts/architecture.md` — REST, Problem Details, tenant isolation
- `_bmad-output/implementation-artifacts/1-7-mobile-sign-in-and-sign-out.md` — mobile API client patterns
- `apps/api/.../TenantScopeController.java` — tenant boundary reference implementation
- `apps/api/.../Organization.java` — `name` field

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2) — `bmad-dev-story` workflow

### Debug Log References

### Completion Notes List

- **API:** `OrganizationProfileController` — `GET/PATCH /api/v1/organizations/{organizationId}/profile`; `TenantPathAuthorization` shared with refactored **`TenantScopeController`**; **`ForbiddenRoleException`** + **`FORBIDDEN_ROLE`** in **`ApiExceptionHandler`**; **`OrganizationProfileIT`** covers admin/technician/tenant/unauth/validation cases.
- **Mobile:** `getRoleFromAccessToken` + **`session`** `userRole` + **`setSession`** fourth arg on login/refresh/sign-out; **`app/(app)/organization.tsx`** (RHF+Zod, Query+mutation, Snackbar, Technician read-only); **`lib/org-profile-api.ts`**, **`lib/auth/org-profile-schema.ts`**; home link **Organization**.

### File List

- `apps/api/src/main/java/com/mowercare/security/TenantPathAuthorization.java`
- `apps/api/src/main/java/com/mowercare/exception/ForbiddenRoleException.java`
- `apps/api/src/main/java/com/mowercare/model/request/OrganizationProfilePatchRequest.java`
- `apps/api/src/main/java/com/mowercare/model/response/OrganizationProfileResponse.java`
- `apps/api/src/main/java/com/mowercare/controller/OrganizationProfileController.java`
- `apps/api/src/main/java/com/mowercare/controller/TenantScopeController.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/test/java/com/mowercare/controller/OrganizationProfileIT.java`
- `apps/mobile/lib/jwt-org.ts`
- `apps/mobile/lib/auth/session.ts`
- `apps/mobile/lib/api.ts`
- `apps/mobile/lib/auth-context.tsx`
- `apps/mobile/lib/org-profile-api.ts`
- `apps/mobile/lib/auth/org-profile-schema.ts`
- `apps/mobile/app/(app)/organization.tsx`
- `apps/mobile/app/(app)/index.tsx`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-03-30 — Story 1.8: org profile API + mobile Organization screen + role in session; `OrganizationProfileIT`; sprint status → **review**.
- 2026-03-30 — Code review fixes: OpenAPI `@ApiResponse` on profile endpoints; `givenWrongOrg_whenPatchProfile_thenTenantDenied`; story **done**.

### Review Findings

- [x] [Review][Patch] OpenAPI — `OrganizationProfileController` lacks `@ApiResponse` annotations for **200**, **401**, **400**, and **403** (`TENANT_ACCESS_DENIED` / `FORBIDDEN_ROLE`) on GET/PATCH; **AC7** expects `/v3/api-docs` to describe these alongside **`TenantScopeController`** parity [`OrganizationProfileController.java`] — fixed: `@ApiResponse` on GET/PATCH + `@Schema` on path param
- [x] [Review][Patch] Integration test gap — add **PATCH** with wrong `organizationId` path expecting **403** `TENANT_ACCESS_DENIED` (GET wrong-org is covered; **AC8** calls out GET and PATCH for tenant boundary) [`OrganizationProfileIT.java`] — fixed: `givenWrongOrg_whenPatchProfile_thenTenantDenied`
- [x] [Review][Defer] `OrganizationProfileController` throws `IllegalStateException` if `findById` misses after tenant match — would surface as **500** without Problem Details; only plausible if DB is inconsistent with JWT [`OrganizationProfileController.java:50,68`] — deferred, pre-existing defensive gap

## Story completion status

- **Status:** done  
- **Note:** Code review patch items applied (`OrganizationProfileIT` passes).

---

## Story validation report (`validate`)

**Date:** 2026-03-30  
**Validator:** `bmad-create-story` checklist (systematic re-check vs `epics.md`, `architecture.md`, **1.7**, and `apps/api` / `apps/mobile` code)

| Category | Result |
|----------|--------|
| **Epic alignment** | **Pass** — ACs subsume `epics.md` Story 1.8 (Admin GET/PATCH + Technician blocked on update). **Interpretation documented:** epic text emphasizes **Admin** for GET/PATCH; this story allows **GET** for **any** org-scoped user so **Technicians** can read the org **name** for AC6 (read-only UI). Epic does not forbid member read access. |
| **Architecture / API patterns** | **Pass** — Tenant boundary, Problem Details `code`, camelCase JSON, `/api/v1/organizations/{organizationId}/...`, JWT claims match **`JwtService`**. |
| **Previous story (1.7)** | **Pass** — Reuses **`authenticatedFetchJson`**, **`getSessionOrganizationId`**, secure session; adds role decoding + cache invalidation guidance. |
| **Ambiguity reduction** | **Fixed in validate** — PATCH success pinned to **200 + JSON** (removed 204 option). OpenAPI bullet expanded with **401**. Typo fixed in AC2 (`role.name()` markdown). |
| **Enhancements applied** | **Query invalidation** after PATCH; **role** must stay in sync with **`setSession`** after refresh; **GET unauth** test; **Organizations** OpenAPI tag consistency. |

**Outcome:** **Pass** — story updated in place; ready for `bmad-dev-story`.
