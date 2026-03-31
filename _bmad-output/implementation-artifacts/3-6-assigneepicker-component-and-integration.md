# Story 3.6: AssigneePicker component and integration

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. validate-create-story (checklist.md): see Validation record at end. -->

## Story

As a **Technician or Admin**,
I want **a searchable picker to assign or reassign issues to team members**,
so that **handoffs are explicit and fast (FR16; UX-DR5)**.

**Implements:** **FR16**; **UX-DR5** (modal or bottom sheet, Paper List + Searchbar, loading / empty / Problem Details error).

**Epic:** Epic 3 — Issues — capture, triage, ownership & history.

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 3.6])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** assign action **When** picker opens **Then** org member list loads with search; loading/empty/error states follow Problem Details | **Mobile:** `AssigneePicker` surface (Paper `Searchbar` + scrollable list); **TanStack Query** for fetch; **loading** spinner/skeleton, **empty** copy, **error** with **Retry** using **`ApiProblemError`** |
| **Given** selection **When** user confirms **Then** assignment persists and list/detail reflect new assignee | **`patchIssue`** with **`assigneeUserId`**; **`invalidateQueries`** **`['issue', orgId, issueId]`** and **`['issues']`** (prefix) so list + detail refresh |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **3.5** | **Builds on** **`patchIssue`** + edit mode on **`[id].tsx`**. **3.5** shipped **Assign to me** / **Unassign**; **3.6** adds **full** searchable picker per **UX-DR5**. Reuse **`buildPatch`**, **`IssueUpdatePayload`**, mutation + snackbar patterns. |
| **3.7** | **History timeline** — out of scope; **`issue_change_events`** already append on assign. |
| **3.8** | List filters/sort — separate story. |

### RBAC gap (must resolve in this story)

[`docs/rbac-matrix.md`](../../docs/rbac-matrix.md) — **`GET /api/v1/organizations/{organizationId}/users`** is **Admin only**; **Technician** is **Deny**. Story **3.5** documented that technicians cannot list users for assignment. **Story 3.6** product intent (epics + UX) is a **searchable org member list** for **both** roles.

**Required direction — pick one approach and document in PR + OpenAPI:**

1. **Recommended:** Add **`GET /api/v1/organizations/{organizationId}/assignable-users`** (name negotiable: e.g. `assignable-members`) returning a **minimal** DTO (at least **`id`**, **`email`**; optionally **`role`**, **`accountStatus`**) including only users **valid to assign** (at minimum **`ACTIVE`**; exclude **`DEACTIVATED`**; product choice on **`PENDING_INVITE`** — default **exclude** until they complete invite). **Allow** **Admin** + **Technician**. Return list in a **deterministic order** (e.g. **`email` ascending**) so the picker is stable across reloads. Keeps full **`GET .../users`** Admin-only for **Team** management. Implement in **`OrganizationUserService`** + **`OrganizationUsersController`** (or **`IssueStubController`** if you prefer colocation — prefer **users** package for listing users). Mirror existing Springdoc **`@Tag`**, **`@SecurityRequirement(name = "bearer-jwt")`**, and **`@ApiResponse`** patterns from [`OrganizationUsersController`](../../apps/api/src/main/java/com/mowercare/controller/OrganizationUsersController.java). Update **`docs/rbac-matrix.md`** + **`RbacEnforcementIT`**.

2. **Alternative:** Change **`GET .../users`** to **Allow** Technician for **read-only** list — simpler code path but expands technician visibility to the **same** payload as admin list; only choose if product accepts that privacy trade-off.

**Do not** call Admin-only **`listEmployeeUsers`** from Technician flows without an API change — **403** today.

**Mobile — single code path:** Use the **same** assignable-list API for **Admin** and **Technician** in **`AssigneePicker`** (do **not** branch Admin → **`listEmployeeUsers`** and Technician → new endpoint); avoids divergent UX and duplicate caching logic.

## Acceptance Criteria

1. **API — assignable user list (Technician + Admin)**  
   **Given** valid JWT with org matching path  
   **When** the new **assignable-users** (or equivalent) **`GET`** is called  
   **Then** **200** returns a JSON array of members usable for issue assignment **And** response is org-scoped **And** **`DEACTIVATED`** users are not returned (and **`PENDING_INVITE`** policy is explicit in code comments or OpenAPI) **And** array order is **deterministic** (e.g. sorted by **`email`**) **And** **OpenAPI** documents the route **And** **`docs/rbac-matrix.md`** has a new row **And** **`RbacEnforcementIT`** covers **200** for **Admin** and for **Technician** (mirror existing issue-route tests in [`RbacEnforcementIT`](../../apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java) — bootstrap + `loginAccessToken` + `get(...)`).

2. **API — tenant + auth**  
   **Given** JWT org ≠ path **Then** **403** **`TENANT_ACCESS_DENIED`**. **Given** missing/invalid auth **Then** **401** per existing patterns.

3. **Mobile — `AssigneePicker` component**  
   **Given** UX-DR5 **[Source: `_bmad-output/planning-artifacts/ux-design-specification.md` — AssigneePicker]**  
   **When** implemented  
   **Then** use **React Native Paper** (**`Searchbar`** + **`List`** / **`List.Item`** or **`FlatList`**) inside a **modal** or **bottom sheet**-like surface (**`Portal`** + **`Modal`** is fine — no new dependency required unless you add `@gorhom/bottom-sheet` with justification) **And** **client-side filter** on loaded list by email (and optional display string) **And** **loading**, **empty** (“No team members match” / “No active users”), **error** + **Retry** **And** **accessibility:** search field and list items have sensible **`accessibilityLabel`** / roles where applicable (align **3.9** later for full pass).

4. **Mobile — integration on issue detail**  
   **Given** edit mode on [`apps/mobile/app/(app)/issues/[id].tsx`](../../apps/mobile/app/(app)/issues/[id].tsx)  
   **When** user opens assign flow  
   **Then** **Admin** and **Technician** can open **`AssigneePicker`**, select a member, and dismiss the picker **And** selection updates **`draft.assigneeUserId`** (or equivalent) **And** epics **“confirm”** maps to **persisting via existing edit flow**: user taps the app bar **Save** (**check**) so **`patchIssue`** runs (same as other field edits — no silent PATCH on row tap unless you add an explicit **Apply** button inside the picker and document it) **And** user can still use **Assign to me** / **Unassign** from **3.5** **And** on successful save, **`invalidateQueries`** **`['issue', orgId, issueId]`** and **`['issues']`** as today.

5. **Mobile — API client**  
   **Given** new endpoint  
   **When** implemented  
   **Then** add typed helper (e.g. **`listAssignableUsers()`**) in **`lib/`** next to existing API modules — reuse **`authenticatedFetchJson`**, **`getSessionOrganizationId`**, **`ApiProblemError`** — **And** **query key** e.g. **`['assignable-users', orgId]`** for TanStack Query cache.

6. **Tests / verification**  
   **Given** API IT patterns (**`OrganizationUsersIT`**, **`AbstractPostgresIntegrationTest`**)  
   **When** complete  
   **Then** integration tests prove **200** list shape, **403** tenant, **Admin** and **Technician** both **200** on assignable list **And** deactivated user excluded when seed data includes one **And** mobile **`npm run typecheck`** and **`npm run lint`** pass **And** **`mvn test`** in **`apps/api`** passes.

## Tasks / Subtasks

- [x] **Product/API — assignable list + RBAC** (AC: 1, 2, 6)
  - [x] Service method: query org users, filter assignable, map DTO.
  - [x] Controller route + OpenAPI.
  - [x] `docs/rbac-matrix.md` + `RbacEnforcementIT`.

- [x] **Mobile — API + hook** (AC: 5)
  - [x] `listAssignableUsers` + `useQuery` for picker open (or prefetch on edit).

- [x] **Mobile — `AssigneePicker` + detail wiring** (AC: 3, 4)
  - [x] New component under [`apps/mobile/components/`](../../apps/mobile/components/) (see **`IssueRow.tsx`** colocation).
  - [x] **Choose assignee** entry point in edit UI; wire confirm/cancel.

- [x] **Verification** (AC: 6)
  - [x] `mvn -q test` in `apps/api`
  - [x] `npm run typecheck` + `npm run lint` in `apps/mobile`

## Dev Notes

### UX-DR5 (authoritative component behavior)

[Source: `_bmad-output/planning-artifacts/ux-design-specification.md` — AssigneePicker]

- **Purpose:** Pick or reassign org member **within RBAC**.
- **Content:** Searchable list — Paper **List** + **Searchbar**.
- **States:** Loading, empty (no users), error (Problem Details).

Architecture **Component Implementation Strategy** suggests `features/issue/components/` — this repo currently uses **`apps/mobile/components/`** for **`IssueRow`**; place **`AssigneePicker`** alongside unless you introduce a `features/issue/` folder (either is fine; **stay consistent** with one approach).

### Previous story intelligence (3.5)

- **`patchIssue`**, **`IssueUpdatePayload`**, **`buildPatch`** — assignment is **`assigneeUserId`** string UUID or empty → **`null`**.
- **Query keys:** **`['issue', orgId, issueId]`**, **`['issues']`** invalidation on success.
- **Session:** **`getSubjectFromAccessToken`** for **Assign to me** — keep.
- **MutationFeedback:** Snackbar + retry pattern for **`patchIssue`** — picker should not bypass; failed assign selection before save is local state only; failed **save** uses existing mutation error handling.
- **Files:** [`issue-api.ts`](../../apps/mobile/lib/issue-api.ts), [`[id].tsx`](../../apps/mobile/app/(app)/issues/[id].tsx).

### Architecture compliance

| Topic | Source |
|-------|--------|
| REST + JSON + Problem Details | [`architecture.md`](../planning-artifacts/architecture.md) |
| TanStack Query for server state | [`architecture.md`](../planning-artifacts/architecture.md) |
| Mobile Paper + tokens | [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md) |

### Assignee validation (server)

[`IssueService.resolveAssigneeOrNull`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) loads user by org + id; does **not** currently enforce **`ACTIVE`**. **Picker** should only offer assignable users; optional follow-up: reject **`DEACTIVATED`** in **`resolveAssigneeOrNull`** with a clear **`400`** — **out of scope** unless you hit inconsistent states in testing.

### Reuse

| Asset | Notes |
|-------|-------|
| [`organization-users-api.ts`](../../apps/mobile/lib/organization-users-api.ts) | Pattern for **`authenticatedFetchJson`** + org id — new helper parallel to **`listEmployeeUsers`** (Admin-only) |
| [`team.tsx`](../../apps/mobile/app/(app)/team.tsx) | **Query** + **error + Retry** pattern reference |
| [`IssueStubController`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java) | PATCH already supports **`assigneeUserId`** — no change required for assignment persistence |

### File structure (guidance)

| Area | Guidance |
|------|----------|
| API | `OrganizationUsersController` + `OrganizationUserService` (or small dedicated facade) |
| API DTO | `com.mowercare.model.response` — e.g. **`AssignableUserResponse`** |
| Mobile component | `apps/mobile/components/AssigneePicker.tsx` (or `features/issue/components/`) |
| Mobile API | `apps/mobile/lib/assignable-users-api.ts` or extend `organization-users-api.ts` with clear separation |

### Testing requirements

- **API:** New IT methods for assignable list — **Admin** + **Technician** **200**, wrong org **403**, list excludes **DEACTIVATED** when seeded **And** optional JSON assertions on sort order if you fix **email** ordering in service.
- **RbacEnforcementIT:** Add **`GET`** tests for the new path (same style as **`givenAdmin_whenGetIssues_thenOk`** / technician issue GET tests).
- **Mobile:** Manual smoke: open picker, search, select, **Save** app bar, list row + detail show assignee **email** label as today.

### Latest tech stack (pin in repo)

- **Expo** ~55, **React Native Paper** ^5.15 — use **`Searchbar`** from **`react-native-paper`**; no version bump required for this story unless a bug blocks.

### Git intelligence (recent commits)

- **`49bf354`** — Story **3.5** PATCH + mobile edit ( **`[id].tsx`**, **`issue-api.ts`** ).
- **`6c49deb`** — Story **3.4** issue detail GET.
- **`b761be6`** — Story **3.3** issues list + query keys.

### Project context reference

No **`project-context.md`** in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md).

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2) agent

### Debug Log References

None.

### Completion Notes List

- **`GET /api/v1/organizations/{organizationId}/assignable-users`** — **`OrganizationUserService.listAssignableUsers`**: **ACTIVE** only, email order; **`RoleAuthorization.requireEmployee`** (Admin + Technician).
- **IT:** **`OrganizationUsersIT`** (assignable sorted, invite excluded, technician sees admin+tech); **`RbacEnforcementIT`** (admin + technician **200**, tenant **403**).
- **Mobile:** **`assignable-users-api.ts`**, **`AssigneePicker`** (Modal + Searchbar + FlatList + Retry); **`[id].tsx`** **Choose assignee** updates **`draft.assigneeUserId`**; save still via app bar **`patchIssue`**.

### File List

- `apps/api/src/main/java/com/mowercare/model/response/AssignableUserResponse.java`
- `apps/api/src/main/java/com/mowercare/security/RoleAuthorization.java`
- `apps/api/src/main/java/com/mowercare/service/OrganizationUserService.java`
- `apps/api/src/main/java/com/mowercare/controller/OrganizationUsersController.java`
- `apps/api/src/test/java/com/mowercare/controller/OrganizationUsersIT.java`
- `apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java`
- `docs/rbac-matrix.md`
- `apps/mobile/lib/assignable-users-api.ts`
- `apps/mobile/components/AssigneePicker.tsx`
- `apps/mobile/app/(app)/issues/[id].tsx`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/3-6-assigneepicker-component-and-integration.md`

## Change Log

- **2026-03-31:** Story created — `bmad-create-story` auto-discover (first backlog: **3-6-assigneepicker-component-and-integration**). Ultimate context engine analysis completed — comprehensive developer guide created.
- **2026-03-31:** `validate-create-story` — checklist pass; AC6 Admin coverage; deterministic list order; epic **confirm** ↔ app bar Save; single mobile API path; Springdoc parity note; testing notes.
- **2026-03-31:** Implemented Story **3.6** — assignable-users API, **`AssigneePicker`**, issue detail wiring; `mvn test`, mobile `typecheck`/`lint` green; status → **review**; sprint **3-6** → **review**.
- **2026-03-31:** Code review patches applied (sprint timestamp, deactivated assignable-users IT, `FORBIDDEN_ROLE` detail for employee routes); `mvn test` green; status → **done**; sprint **3-6** → **done**.

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-31 against `checklist.md`, `epics.md` Story **3.6**, [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md), [`RbacEnforcementIT`](../../apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java).

### Critical (addressed in file)

| Issue | Resolution |
|-------|------------|
| **AC6** only called out **Technician** | Expanded to **Admin** + **Technician** **200** and deactivated exclusion in IT |
| **Epics “When user confirms”** vs **3.5** edit flow | **AC4** clarifies: selection updates **draft**; **confirm** = user **Save** (check) unless an in-picker **Apply** is added and documented |
| **Reinvention / dual client** | **RBAC gap** section: **same** assignable API for Admin + Technician in picker — no Admin-only **`listEmployeeUsers`** branch |
| **Ambiguous API contract** | **AC1** + RBAC recommended path: **deterministic** sort (**email** asc); Springdoc parity with **`OrganizationUsersController`** |

### Enhancements applied

| Addition | Benefit |
|----------|---------|
| **`RbacEnforcementIT`** pointer + mirror existing GET issue tests | Faster, consistent test wiring |
| **Sort order** | Stable picker UX; fewer “flaky” list order issues |

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — **`patchIssue`**, **`invalidateQueries`**, new read-only GET |
| Technical / RBAC | Pass — matrix row + IT; tenant **403** |
| Regression | Pass — **3.5** flows preserved (**Assign to me** / **Unassign**) |
| Epic alignment | Pass — FR16, UX-DR5, epics AC verbatim intent |

### Residual risks (acceptable for ready-for-dev)

- **Exact path name** (`assignable-users` vs `assignable-members`) — lock when implementing OpenAPI.
- **Bottom sheet** vs **Modal** — either acceptable; avoid new deps unless justified.

### Review Findings

- [x] [Review][Patch] Sprint metadata regression — `sprint-status.yaml` `last_updated` moved backward (e.g. `22:35` → `18:30:00Z`), which breaks audit ordering; restore a current timestamp when syncing. [`_bmad-output/implementation-artifacts/sprint-status.yaml`] — **Fixed 2026-03-31:** `last_updated` corrected; synced again on story completion.
- [x] [Review][Patch] AC6 / Testing requirements: add an integration test that seeds a **DEACTIVATED** org user and asserts `GET .../assignable-users` does **not** return them (story and Validation record claim this coverage; the diff does not add it). [`OrganizationUsersIT.java`] — **Fixed 2026-03-31:** `givenAdmin_whenDeactivateTech_thenAssignableExcludesDeactivated`.
- [x] [Review][Patch] `RoleAuthorization.requireEmployee` throws `ForbiddenRoleException(UserRole.TECHNICIAN)`, and `ApiExceptionHandler` builds detail text as “requires the TECHNICIAN role,” which is **false** for this endpoint (Admin **or** Technician). Fix exception payload/detail so clients see a correct requirement (e.g. support multiple allowed roles or a dedicated message). [`RoleAuthorization.java`](../../apps/api/src/main/java/com/mowercare/security/RoleAuthorization.java), [`ApiExceptionHandler.java`](../../apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java) — **Fixed 2026-03-31:** `ForbiddenRoleException` optional detail override + `getDetailForProblem()`; employee gate uses “ADMIN or TECHNICIAN” message; unit tests in `RoleAuthorizationTest`.
