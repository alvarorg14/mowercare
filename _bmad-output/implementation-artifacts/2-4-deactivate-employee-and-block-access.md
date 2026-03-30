# Story 2.4: Deactivate employee and block access

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->
<!-- validate-create-story (checklist.md): passed with amendments 2026-03-30 — see Validation record at end. -->

## Story

As an **Org Admin**,
I want **to deactivate an employee**,
so that **they can no longer access org data (FR25, FR26)**.

**Implements:** FR25, FR26; NFR-S5 (auditable deactivation).

**Epic:** Epic 2 — Organization admin, roles & access control — builds on **2.1–2.3** (`OrganizationUsersController`, `User.accountStatus`, `AuthService` + `RefreshToken`, JWT access + opaque refresh, last-admin patterns).

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.4])

| Epics.md (authoritative) | Covered in this story by |
|---------------------------|-------------------------|
| **Given** Admin deactivates a user **When** the change commits **Then** user is marked inactive; refresh tokens are revoked; next API calls return `401`/`403` as documented | **`AccountStatus.DEACTIVATED`** (or equivalent); **delete or revoke all** `refresh_tokens` rows for that user; **request-time** check so a still-valid access JWT cannot call protected APIs after deactivation (see Tasks) |
| **Given** audit requirements **When** deactivation occurs **Then** an auditable record exists (who deactivated whom, when) | Persist **`deactivated_at`** + **`deactivated_by_user_id`** (nullable self-FK on `users`) **or** equivalent auditable row — must be queryable for support/compliance |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **2.3** | **Last-admin** logic must be **updated**: deactivation must not leave **zero** reachable Admins — mirror **`LAST_ADMIN_REMOVAL`** class of rule (new stable `code` if needed). **Admin count** for this guard should treat **`DEACTIVATED`** (and optionally invite-only rows per product choice) consistently — **prefer**: count **`UserRole.ADMIN`** among users who are **not** `DEACTIVATED` (so a deactivated user does not count as “the” admin). |
| **2.5** | **Out of scope** — mobile Settings UI; this story is **API + security + tests + rbac-matrix + OpenAPI**. |
| **2.6** | Employee-only product posture unchanged; no separate work unless copy references “customer portal”. |

## Acceptance Criteria

1. **Given** an authenticated **Admin** JWT for organization **O**  
   **When** they call the **deactivate-employee** endpoint for user **U** in **O**  
   **Then** **`U.account_status`** becomes **`DEACTIVATED`** (new enum value), **all** refresh token rows for **`U`** are removed or revoked so **refresh cannot mint new access tokens**, and the response is **`200 OK`** with **`EmployeeUserResponse`** reflecting the new status (and existing fields — email, role, ids, `createdAt` as today). Tenant vs role error **order** matches [Source: `OrganizationUsersController.java`] (**`403`** `TENANT_ACCESS_DENIED` then **`403`** `FORBIDDEN_ROLE`). **`404`** if **U** not in **O**.

2. **Given** user **U** was just deactivated  
   **When** **U** calls **`POST /api/v1/auth/refresh`** with **any** refresh token that existed before deactivation (or a still-cached token)  
   **Then** the API does **not** issue new tokens — **`401`** + **`AUTH_REFRESH_INVALID`** (same as [Source: `ApiExceptionHandler.invalidRefresh`]): either **`findByTokenHash`** misses after row deletion (**`InvalidRefreshTokenException`**) **or** the refresh path loads **`User`** and finds **`DEACTIVATED`** and throws **`InvalidRefreshTokenException`** — **both** map to **`code`** **`AUTH_REFRESH_INVALID`** (no second refresh `code` required for MVP).

3. **Given** user **U** is **DEACTIVATED**  
   **When** **U** calls **`POST /api/v1/auth/login`** with correct password  
   **Then** **`401`** invalid-credentials style response — **same class as** wrong password / pending invite (**do not** distinguish deactivated vs bad password in the HTTP response if that would leak account state — follow existing **`InvalidCredentialsException`** pattern for login).

4. **Given** **U** holds a **still-valid access JWT** issued before deactivation  
   **When** **U** calls **any protected** API (e.g. org profile, tenant-scope probe)  
   **Then** the request is **rejected** with **`401`** **with a stable Problem Details `code`** **`ACCOUNT_DEACTIVATED`**, `type` URI **`urn:mowercare:problem:ACCOUNT_DEACTIVATED`** (new constant in [Source: `ApiExceptionHandler.java`]), **`title`**/**`detail`** consistent with other auth problems — **not** a silent success. This satisfies epics/UX “session blocked on **next** request” ([Source: UX-DR17 in `epics.md`]).

5. **Given** exactly **one** **`ACTIVE`** **`ADMIN`** remains in org **O** (per Tasks — **invite** `ADMIN` rows do **not** count)  
   **When** an Admin attempts to **deactivate** that user  
   **Then** **`409 Conflict`** + stable **`code`** **`LAST_ADMIN_DEACTIVATION`** + RFC 7807 Problem Details — **same spirit** as **`LAST_ADMIN_REMOVAL`**; **`detail`** must state the org must retain **at least one** Admin (mirror [Source: `ApiExceptionHandler.lastAdminRemoval`] wording for IT assertions).

6. **Given** **OpenAPI** (`/v3/api-docs`)  
   **When** the route is registered  
   **Then** it appears with **bearer-jwt**, **200**, **401** (missing/invalid bearer), **403/404/409** as applicable — align with [Source: `OrganizationUsersController.updateUserRole`] annotation style for **401**.

7. **Given** **audit**  
   **When** deactivation succeeds  
   **Then** persisted fields allow answering **who** (actor user id) **deactivated whom** (target) **when** (timestamp) — e.g. **`deactivated_by_user_id`** + **`deactivated_at`** on `users` (or dedicated audit table with the same facts).

8. **Given** a **Technician** JWT  
   **When** they call the deactivate endpoint  
   **Then** **`403`** `FORBIDDEN_ROLE`.

## Tasks / Subtasks

- [x] **Domain + Liquibase** (AC: 1, 7)
  - [x] Add **`AccountStatus.DEACTIVATED`** — extend [Source: `AccountStatus.java`].
  - [x] Add **`deactivated_at`** (timestamptz, nullable) and **`deactivated_by_user_id`** (UUID, nullable, FK → `users.id`) via Liquibase; backfill **NULL** for existing rows.
  - [x] Add **`User.deactivate(UUID actorUserId, Instant at)`** (or service-only mutation) — enforce idempotency: deactivating an **already** `DEACTIVATED` user returns **200** with same state (no error).

- [x] **Refresh token revocation** (AC: 2)
  - [x] Extend [Source: `RefreshTokenRepository.java`] with bulk delete or revoke-by-user, e.g. **`deleteByUser_Id`** / **`@Modifying`** query — call from deactivate flow inside **`@Transactional`** with user update.
  - [x] Ensure **`AuthService.refresh`** / **`issueTokensForUser`** cannot issue tokens for **`DEACTIVATED`** users (defense in depth after row deletion).

- [x] **AuthService** (AC: 2, 3)
  - [x] **`login`**: reject **`DEACTIVATED`** like **`PENDING_INVITE`** (invalid credentials).
  - [x] **`refresh`**: after loading **`User`**, if **`DEACTIVATED`**, throw **`InvalidRefreshTokenException`** (or consistent 401 path).

- [x] **Request-time access JWT enforcement** (AC: 4)
  - [x] Implement a **single** place (e.g. **`OncePerRequestFilter`** **after** resource-server JWT authentication so **`SecurityContext`** holds **`Jwt`**) that loads **`User`** by JWT **`sub`** and rejects **`DEACTIVATED`** with **`401`** + **`ACCOUNT_DEACTIVATED`** Problem Details.
  - [x] **Critical — filter vs `@RestControllerAdvice`:** Exceptions thrown **inside** servlet filters **do not** reliably reach [Source: `ApiExceptionHandler.java`]. Either **write** `application/problem+json` on the response in the filter (same JSON shape as handler: **`type`**, **`title`**, **`detail`**, **`code`**, **`instance`**) **or** use a supported pattern (e.g. **`HandlerExceptionResolver`**, **`DelegatingAuthenticationEntryPoint`**) — **do not** assume throwing from the filter will hit **`ApiExceptionAdvice`**.
  - [x] **Exclude** public routes: **`/api/v1/auth/**`**, **`/api/v1/bootstrap/**`**, **OpenAPI**, **swagger** — same as [Source: `SecurityConfig.java`].
  - [x] **Performance:** use a **read-only** lookup by id; consider **caching** in a later story if hot-path profiling requires it (not required for MVP).

- [x] **Admin API** (AC: 1, 5, 8)
  - [x] Add **`POST /api/v1/organizations/{organizationId}/users/{userId}/deactivate`** (no body or empty JSON) — **Admin only**, tenant gate first.
  - [x] **Last-admin guard:** before deactivating, if target **`role == ADMIN`** and **`countActiveAdminsInOrg`** would become **0**, throw **`409`** (new exception type mirroring **`LastAdminRemovalException`** pattern).
  - [x] **Self-deactivation:** allowed **only** if last-admin rule passes (Admin can deactivate themselves if another Admin exists).
  - [x] **PATCH role on deactivated user:** return **`409 Conflict`** with stable **`code`** (e.g. **`USER_DEACTIVATED`**) **or** **`404`** if product prefers “not manageable” — **pin one** in implementation and document in OpenAPI (avoid silent role change on inactive account).
  - [x] **Align story 2.3 last-admin count (recommended):** update **`OrganizationUserService.updateUserRole`** last-admin check to count only **`AccountStatus.ACTIVE`** **`ADMIN`** users (same rule as this story) so **2.3** and **2.4** cannot disagree — add **`countByOrganization_IdAndRoleAndAccountStatus`** (or equivalent) if needed.

- [x] **Exception + handler** (AC: 4, 5)
  - [x] Map **`ACCOUNT_DEACTIVATED`** and **`LAST_ADMIN_DEACTIVATION`** with **`type`** URIs consistent with existing **`TYPE_*`** constants.

- [x] **Docs** (AC: 6)
  - [x] Update [Source: `docs/rbac-matrix.md`] — replace “Epic 2.4 (not implemented here)” for deactivate with **Allow Admin / Deny Technician** + route.
  - [x] OpenAPI annotations on controller.

- [x] **Tests** (AC: 1–8)
  - [x] Extend [Source: `OrganizationUsersIT.java`]: Admin deactivates → **200** + DB state + **no** refresh rows for user; Technician → **403**; last admin → **409**; idempotent second POST → **200**.
  - [x] **Auth path:** login as user → deactivate via admin → **login** / **refresh** / **protected GET** with old access token → assert **401** + codes per AC.
  - [x] **`mvn test`** green for `apps/api`.

### Review Findings

- [x] [Review][Patch] FK `fk_users_deactivated_by_user_id` blocks deleting a user who is still referenced as `deactivated_by` on another row — `OrganizationUsersIT` fails in `@BeforeEach` with `DataIntegrityViolationException` when `userRepository.deleteAll()` runs. Prefer `ON DELETE SET NULL` on the FK (new Liquibase changeset) so audit references do not pin user rows forever. [`0006-user-deactivation-audit.yaml`, `OrganizationUsersIT.java:74`] — **Fixed:** Liquibase `0007-deactivated-by-fk-on-delete-set-null.yaml` recreates the FK with `ON DELETE SET NULL`.

- [x] [Review][Patch] `AccountStatusVerificationFilter` uses a private static `ObjectMapper` — risk of JSON shape drift vs `ApiExceptionHandler` / global beans. Consider injecting the application `ObjectMapper` (or a dedicated `@Bean` for Problem Details). [`AccountStatusVerificationFilter.java:37-44`] — **Fixed:** `JacksonObjectMapperConfig` exposes a `@Bean` `ObjectMapper` with `ProblemDetailJacksonMixin`; filter injects it.

- [x] [Review][Defer] Per-request DB lookup for deactivated status on every protected call — deferred; story explicitly accepts MVP tradeoff (future caching).

## Dev Notes

### Scope boundaries

- **In scope:** Deactivate API, **token** revocation, **login/refresh** blocks, **per-request** block for valid access JWTs, **audit** columns, **last-admin** on deactivate, **RBAC matrix**, **IT** coverage.
- **Out of scope:** **Reactivate** employee (new story if needed); **mobile** UI (**2.5**); **structured audit log** table for all admin actions beyond this row (**optional** follow-up).

### Architecture compliance

- **Tokens:** [Source: `architecture.md` — Auth] — refresh rotation/revocation for **deactivated users** (FR26).
- **Errors:** RFC 7807 + stable **`code`** — [Source: `architecture.md` — Format patterns].
- **Tenant + role:** [Source: `TenantPathAuthorization.java`], [Source: `RoleAuthorization.java`].

### Existing code to reuse

| Asset | Location |
|-------|----------|
| Admin + tenant gates | Same as PATCH role |
| **`UserRepository.findByOrganization_IdAndId`**, counts | Extend for **active** admin count |
| **`EmployeeUserResponse`** | Add display of new status; **`toListResponse`** mapper |
| **`RefreshToken`** / repository | Bulk delete by user |
| **`AuthService`** | Login + refresh guards |
| **Problem Details** | `ApiExceptionHandler` |

### Technical requirements

| Area | Requirement |
|------|-------------|
| Enum | **`DEACTIVATED`** — must appear in API JSON for `accountStatus` on list/get/deactivate responses. |
| Idempotency | Second **deactivate** on same user → **200**, no **409** unless last-admin logic somehow changes (it should not). |
| Concurrency | **`@Transactional`** deactivate: update user + delete refresh rows **atomically**. |
| Privacy | Login failure messages unchanged — **no** “account deactivated” string in **`401`** login body if that leaks enumeration (match **`InvalidCredentialsException`** behavior). **Protected** route with valid JWT may return **`ACCOUNT_DEACTIVATED`** (user already authenticated). |

### Library / framework requirements

| Layer | Notes |
|-------|-------|
| API | Spring Security OAuth2 resource server JWT — **no** new auth stack. |
| Tests | JUnit 5, `@SpringBootTest`, existing `OrganizationUsersIT` / `AuthIT` patterns. |

### File structure requirements

| Area | Guidance |
|------|----------|
| Controller | `OrganizationUsersController` — new `POST .../deactivate` |
| Service | `OrganizationUserService` — `deactivateUser(...)`; possibly **`AuthService`** edits |
| Security | New filter or small component under `com.mowercare.security` / `config` |
| Liquibase | New changelog under `db/changelog/changes/` |

### Testing requirements

- Prefer **integration tests** for full HTTP + DB + security filter behavior.
- Assert **Problem Details** `application/problem+json` + **`code`** for **409** / **401** (deactivated session) paths.

### Previous story intelligence (2.3)

- **409 / stable codes:** Mirror **`LastAdminRemovalException`** → **`LAST_ADMIN_REMOVAL`** handler style for **`LAST_ADMIN_DEACTIVATION`**.
- **Last-admin counting:** Story 2.3 used **`countByOrganization_IdAndRole`** including non-active invite rows; **this story** should **define** whether **`PENDING_INVITE` + ADMIN** counts for “org has an admin” — **recommend:** for **deactivation** guard, count only **`AccountStatus.ACTIVE`** **`ADMIN`** users (invite not yet accepted is not an operational admin). Document in Dev Notes if product disagrees.
- **PATCH role** doc said **2.4** may refine admin count — implement that refinement here and adjust **role-change** last-admin logic **if** story requires one consistent rule (optional follow-up: align **`updateUserRole`** count query to **ACTIVE** admins only — **scope explicitly** in tasks if you touch 2.3 code).

### Git intelligence (recent commits)

- **`4e4844c`** — Story 2.3: PATCH role, **`LastAdminRemovalException`**, pessimistic lock + count — **extend** patterns for deactivate + last admin.
- **`57f2692`** — Story 2.2: `OrganizationUsersController` / service — **extend** for deactivate route.

### Latest tech notes

- Spring Security 6 resource server: place account-status filter **after** JWT authentication so **`SecurityContext`** holds **`Jwt`**.

### Project context reference

- No **`project-context.md`** in repo; rely on **`architecture.md`** + this story.

## Dev Agent Record

### Agent Model Used

Cursor agent (GPT-5.1)

### Debug Log References

_(none)_

### Completion Notes List

- Implemented **`POST /api/v1/organizations/{organizationId}/users/{userId}/deactivate`**, **`AccountStatus.DEACTIVATED`**, Liquibase **`0006`** (+ post-review **`0007`** FK `ON DELETE SET NULL`), audit columns, **`RefreshTokenRepository.deleteByUser_Id`**, **`OrganizationUserService.deactivateUser`** with last-admin guard (**`LAST_ADMIN_DEACTIVATION`**), **`updateUserRole`** last-admin count restricted to **ACTIVE** **`ADMIN`**, **`USER_DEACTIVATED`** on PATCH for deactivated users, **`AuthService`** login/refresh/**`issueTokensForUser`** guards, **`AccountStatusVerificationFilter`** (writes **`ACCOUNT_DEACTIVATED`** via **`ApiProblemBodies`** + **`JacksonObjectMapperConfig`** `ObjectMapper`), **`ApiProblemBodies`** + **`ApiExceptionHandler`** for **`LastAdminDeactivationException`**. Integration tests in **`OrganizationUsersIT`**; unit tests in **`AuthServiceTest`**. **`docs/rbac-matrix.md`** updated. **`mvn test`** in **`apps/api`** passed.

### File List

- `apps/api/src/main/resources/db/changelog/changes/0006-user-deactivation-audit.yaml`
- `apps/api/src/main/resources/db/changelog/changes/0007-deactivated-by-fk-on-delete-set-null.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/java/com/mowercare/config/JacksonObjectMapperConfig.java`
- `apps/api/src/main/java/com/mowercare/model/AccountStatus.java`
- `apps/api/src/main/java/com/mowercare/model/User.java`
- `apps/api/src/main/java/com/mowercare/repository/UserRepository.java`
- `apps/api/src/main/java/com/mowercare/repository/RefreshTokenRepository.java`
- `apps/api/src/main/java/com/mowercare/exception/LastAdminDeactivationException.java`
- `apps/api/src/main/java/com/mowercare/exception/UserDeactivatedManagementException.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiProblemBodies.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/main/java/com/mowercare/security/AccountStatusVerificationFilter.java`
- `apps/api/src/main/java/com/mowercare/config/SecurityConfig.java`
- `apps/api/src/main/java/com/mowercare/service/AuthService.java`
- `apps/api/src/main/java/com/mowercare/service/OrganizationUserService.java`
- `apps/api/src/main/java/com/mowercare/controller/OrganizationUsersController.java`
- `apps/api/src/test/java/com/mowercare/controller/OrganizationUsersIT.java`
- `apps/api/src/test/java/com/mowercare/service/AuthServiceTest.java`
- `docs/rbac-matrix.md`

## Change Log

- **2026-03-30:** Story created (`bmad-create-story` workflow).
- **2026-03-30:** validate-create-story (checklist.md) — amendments applied; see Validation record.
- **2026-03-31:** Implemented Story 2.4 (deactivate, audit, filter, auth guards, tests, rbac-matrix); `mvn test` in `apps/api` passed.
- **2026-03-30:** Code review patches: Liquibase `0007` (`ON DELETE SET NULL` on `deactivated_by_user_id` FK); `JacksonObjectMapperConfig` + injected `ObjectMapper` in `AccountStatusVerificationFilter` (Problem Details JSON aligned with MVC); story marked **done**.

---

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-30 against `epics.md` Story 2.4, `architecture.md` (auth/tokens), `ApiExceptionHandler`, `SecurityConfig`, `AuthService`, story **2.3** patterns, and checklist.md gap analysis.

### Critical / traceability (addressed in file)

- **Filter vs global exception handler:** Documented that **`ApiExceptionHandler` does not catch** typical filter-thrown exceptions — implementer must **serialize Problem Details in-filter** or use an entry-point / resolver pattern; prevents **500** or empty bodies on **`ACCOUNT_DEACTIVATED`**.
- **Refresh `code` after deactivation:** Pinned to **`AUTH_REFRESH_INVALID`** only (delete rows or reject **`DEACTIVATED`** user in refresh) — removes ambiguity about a second refresh-specific code.
- **`ACCOUNT_DEACTIVATED` contract:** Pinned **`code`**, **`type`** URI, and handler alignment for AC4.
- **Last-admin AC5:** Pinned **`ACTIVE`** admins only + **`detail`** text requirement for IT (aligned with 2.3 review practice).
- **OpenAPI AC6:** Pinned explicit **401** (missing bearer) like **`updateUserRole`**.
- **2.3 / 2.4 consistency:** Added **recommended** task to align **`updateUserRole`** last-admin counting with **`ACTIVE`** **`ADMIN`** only.

### Enhancements applied

- **PATCH role vs deactivated user:** Added task to pin **409** vs **404** — prevents vague behavior when Admin retries management on inactive user.

### Residual risks (acceptable for ready-for-dev)

- **Per-request DB lookup** on every authenticated API call — acceptable for MVP; story already allows future caching.
- **Automated OpenAPI smoke** still optional (same posture as stories 2.2–2.3).

### Review Findings

- [x] [Critical] Filter / `ApiExceptionHandler` interaction documented.
- [x] [Enhancement] Refresh token **`code`** pinned to **`AUTH_REFRESH_INVALID`**.
- [x] [Enhancement] **`LAST_ADMIN_DEACTIVATION`** **`detail`** assertion guidance.
- [x] [Enhancement] **`updateUserRole`** active-admin alignment task.
- [x] [Enhancement] OpenAPI **401** on deactivate route.
