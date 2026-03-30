# Story 2.5: Admin settings UI (invites, roles, deactivate)

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->
<!-- validate-create-story (checklist.md): passed with amendments 2026-03-30 — see Validation record at end. -->

## Story

As an **Org Admin**,
I want **settings screens to manage users**,
so that **I can run the shop without developer help (UX-DR17)**.

**Implements:** UX-DR17; **FR24–FR26** on the **client** (admin employee management UI). **FR27** (employee-only product posture) is **primarily 2.6** — this story must not add customer registration; RBAC gating here supports the same model.

**Epic:** Epic 2 — Organization admin, roles & access control — **mobile UI only**; builds on [`2-4-deactivate-employee-and-block-access.md`](./2-4-deactivate-employee-and-block-access.md) (API + security), [`2-3-assign-roles-to-employees.md`](./2-3-assign-roles-to-employees.md), [`2-2-invite-or-create-employee-user.md`](./2-2-invite-or-create-employee-user.md).

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.5])

| Epics.md | Covered in this story |
|----------|------------------------|
| **Given** Admin navigates to Settings **When** they invite, change role, or deactivate **Then** flows use Paper components, confirm destructive actions with Dialog, and show MutationFeedback | **Settings** entry + **Team / Users** flows: Paper `List` / `Card` / forms; **Dialog** before **deactivate** (and optionally before destructive role change if product adds it); **pending → success/error** feedback (Snackbar + disabled buttons while mutating — same *honest feedback* intent as UX **MutationFeedback**; dedicated shared component not required if pattern matches [Source: `ux-design-specification.md` — Feedback Patterns]) |
| **Given** Technician **When** they open Settings admin sections **Then** UI hides or disables with explanation per RBAC | **Technician** sees **no** invite/role/deactivate affordances; either **hidden routes** + substitute screen, or **read-only** explanation (align with [`organization.tsx`](../../apps/mobile/app/(app)/organization.tsx) Technician copy pattern) |

### Cross-story boundaries

| Story | Relationship |
|-------|--------------|
| **2.2–2.4** | **Consume** existing REST contract — do **not** change API behavior unless a **blocking** mobile contract gap is found (then minimal OpenAPI/doc follow-up). |
| **2.6** | **Out of scope** — global “employee-only” product guardrails and route absence tests are **2.6**; this story only **RBAC-gates admin UI** using JWT `role` already in session. |

## Acceptance Criteria

1. **Settings entry (Admin + Technician)**  
   **Given** a signed-in user  
   **When** they open the in-app **Settings** area (new screen or stack entry — see Tasks)  
   **Then** they can reach **Organization** (existing profile) and **Team** (users) navigation in a coherent layout; **Admin** and **Technician** both see Settings, but admin-only actions differ per AC2–3.

2. **Admin — list employees**  
   **Given** JWT role **ADMIN** (`getSessionRole() === 'ADMIN'`)  
   **When** they open **Team** / **Users**  
   **Then** the app calls `GET /api/v1/organizations/{organizationId}/users` via `authenticatedFetchJson`, shows **loading** and **error + Retry** on failure, and lists **email**, **role**, **accountStatus** (reuse API field names / camelCase JSON as today). Empty list: helpful **EmptyState** copy + primary **Invite** action.

3. **Admin — invite or create user**  
   **Given** Admin on invite/create flow  
   **When** they submit **email**, **role** (`ADMIN` \| `TECHNICIAN`), and optionally **initial password** (omit → invite per API)  
   **Then** `POST /api/v1/organizations/{organizationId}/users` runs with **React Hook Form + Zod** validation (email format, password min length if provided); **success** refreshes list and shows brief success feedback; **409** + stable **`code`** **`USER_EMAIL_CONFLICT`** maps to user-visible message via `ApiProblemError` `detail`/`title`; invite response may include **`inviteToken`** once — **do not** log to persistent insecure storage; UX: show token once in a **Dialog**; optional **Copy** requires adding **`expo-clipboard`** (not currently in [`package.json`](../../apps/mobile/package.json)) or another supported clipboard API.

4. **Admin — change role**  
   **Given** Admin selects an **active** employee row  
   **When** they change role (e.g. **Menu**, **SegmentedButtons**, or **Dialog** with role picker)  
   **Then** `PATCH .../users/{userId}` with `{ "role": "..." }` runs; list refreshes on success; **409** **`LAST_ADMIN_REMOVAL`** shows **detail** from Problem Details (not generic “Error”); **403** `FORBIDDEN_ROLE` should not occur for Admin — if it does, show message.  
   **Given** the row is **`DEACTIVATED`**  
   **When** Admin attempts role change  
   **Then** API returns **409** + **`USER_DEACTIVATED`** ([Source: `OrganizationUsersIT` — PATCH role on deactivated user]); UI should show **`detail`** and avoid offering role change on deactivated rows (preferred) or handle this error if the row is tappable.

5. **Admin — deactivate**  
   **Given** Admin initiates deactivate  
   **When** they confirm in a **Paper `Dialog`** (destructive styling per UX — [Source: `ux-design-specification.md` — Button Hierarchy])  
   **Then** `POST .../users/{userId}/deactivate` runs; success refreshes list / row state; **409** `LAST_ADMIN_DEACTIVATION` shows clear copy; idempotent **200** on already-deactivated user should still show success feedback without crashing.

6. **Technician — blocked from admin actions**  
   **Given** JWT role **TECHNICIAN**  
   **When** they navigate to Settings  
   **Then** they **cannot** perform invite, role change, or deactivate (no submit paths); **Team** screen explains that only **Org Admins** manage users (consistent tone with Organization profile read-only hint).

7. **Accessibility & Paper**  
   **Given** the flows above  
   **When** rendered  
   **Then** controls have **accessibilityLabel** where icons are icon-only; touch targets align with UX baseline; **one primary** destructive action per confirm surface.

8. **No secrets in client**  
   **Given** invite token or passwords  
   **When** displayed or entered  
   **Then** passwords use **secure text entry**; tokens are not written to logs or AsyncStorage beyond normal session handling.

## Tasks / Subtasks

- [x] **Information architecture** (AC: 1, 6)
  - [x] Add a **Settings** hub screen under `app/(app)/` (e.g. `settings.tsx`) listing **Organization** (link to existing `organization`) and **Team** (new route).
  - [x] From [`app/(app)/index.tsx`](../../apps/mobile/app/(app)/index.tsx), replace or supplement the raw “Organization” button with **Settings** as the main entry (or add both — prefer **Settings** as primary per UX “admin flows under Settings” [Source: `ux-design-specification.md` — Navigation Patterns]).

- [x] **API client module** (AC: 2–5)
  - [x] Add [`apps/mobile/lib/organization-users-api.ts`](../../apps/mobile/lib/organization-users-api.ts) (or `features/...`) with typed helpers: `listEmployeeUsers`, `createEmployeeUser`, `updateEmployeeRole`, `deactivateEmployeeUser`, using [`authenticatedFetchJson`](../../apps/mobile/lib/api.ts) and paths matching [`OrganizationUsersController`](../../apps/api/src/main/java/com/mowercare/controller/OrganizationUsersController.java).
  - [x] Mirror TypeScript types to API: `EmployeeUserResponse`, `CreateEmployeeUserRequest` / `CreateEmployeeUserResponse` (`inviteToken` optional), `UpdateEmployeeUserRoleRequest`, enums for `role` and `accountStatus` as string unions aligned with Jackson naming (`ADMIN`, `TECHNICIAN`, `ACTIVE`, `PENDING_INVITE`, `DEACTIVATED`, …).

- [x] **Team list screen** (AC: 2, 6, 7)
  - [x] New route e.g. `app/(app)/team.tsx` or `team/index.tsx` inside Stack.
  - [x] `useQuery` for list; `useMutation` + `queryClient.invalidateQueries` for create / patch / deactivate.
  - [x] **Technician:** early return with explanation **or** hide **Team** list item in Settings and show not-accessible — pick one pattern and document in Dev Notes.

- [x] **Invite / create modal or screen** (AC: 3, 8)
  - [x] Form: email, role selector, optional password fields; Zod schema; submit → POST; handle **201** + invite token presentation (`inviteToken` in JSON when invite).
  - [x] If implementing Copy: add **`expo-clipboard`** dependency (not in tree today) or document manual copy-only UX.

- [x] **Role change UI** (AC: 4)
  - [x] Per-row or detail action; PATCH; surface `LAST_ADMIN_REMOVAL` from `ApiProblemError.detail`.

- [x] **Deactivate confirmation** (AC: 5)
  - [x] `Dialog` with confirm/cancel; POST deactivate; handle `LAST_ADMIN_DEACTIVATION`.

- [x] **Feedback patterns** (AC: 2–5)
  - [x] Pending: disable primary buttons, show `ActivityIndicator` or `Button` `loading` — [Source: `ux-design-specification.md` — Feedback Patterns].
  - [x] Errors: `Snackbar` or inline text with **Retry** where applicable; map `code` when helpful.

- [x] **Docs** (AC: 6)
  - [x] If UI introduces new user-visible capability, add a short note to [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md) **Mobile** subsection (client gating mirrors API).

- [x] **Tests** (recommended)
  - [x] If the repo has mobile E2E or integration tests, add minimal coverage for Settings → Team list (Admin); otherwise manual test checklist in Completion Notes.

## Dev Notes

### Scope boundaries

- **In scope:** Expo Router screens, TanStack Query, RHF+Zod, Paper, **client-side RBAC** using `getSessionRole()` + optional refetch after role change (remember: **access JWT role claim may lag** until refresh — document expectation: after changing **own** role, user may need to re-login or wait for refresh path; changing **another** user’s role updates list from API response).
- **Out of scope:** New backend endpoints; **2.6** global product guardrails; **reactivation** of deactivated users.

### Architecture compliance

- **Mobile stack:** [Source: `architecture.md` — Expo Router, TanStack Query, RHF + Zod, react-native-paper].
- **HTTP:** [Source: `apps/mobile/lib/api.ts`] — reuse refresh-on-401 for authenticated calls.
- **Errors:** RFC 7807 Problem Details → [`ApiProblemError`](../../apps/mobile/lib/http.ts).

### Existing code to reuse

| Asset | Use |
|-------|-----|
| [`organization.tsx`](../../apps/mobile/app/(app)/organization.tsx) | Appbar + SafeArea + ScrollView + Snackbar + loading/error patterns |
| [`org-profile-api.ts`](../../apps/mobile/lib/org-profile-api.ts) | Query key pattern, `authenticatedFetchJson` |
| [`session.ts`](../../apps/mobile/lib/auth/session.ts) | `getSessionOrganizationId`, `getSessionRole` |

### Technical requirements

| Area | Requirement |
|------|-------------|
| Tenant | Always use **path** `organizationId` from session (`getSessionOrganizationId()`), aligned with JWT — same as org profile. |
| Role lag | After PATCH role, **JWT may still show old role** until token refresh; UI should show **server response** from mutation as source of truth for **that** user’s row. If Admin demotes **self**, expect **403** on subsequent admin calls — product may require **sign out** (document in Completion Notes if encountered). |
| Deactivate | Confirm in **Dialog**; use **destructive** button styling for confirm. |
| List UX | Show **DEACTIVATED** users clearly (badge or caption); **disable** role change (and ideally **deactivate**) for **`DEACTIVATED`** rows — API returns **409** **`USER_DEACTIVATED`** on `PATCH` role if management is attempted anyway ([Source: `OrganizationUserService.updateUserRole`]). **Deactivate** is idempotent **200** if already deactivated. |
| PATCH idempotency | Same role as current → **200** with unchanged user ([Source: `OrganizationUserService.updateUserRole`]) — no error; UI may skip network call if no change. |

### Library / framework requirements

| Layer | Notes |
|-------|-------|
| UI | `react-native-paper` (already in project) |
| Forms | `react-hook-form`, `@hookform/resolvers/zod`, `zod` |
| Data | `@tanstack/react-query` |

### File structure requirements

| Area | Guidance |
|------|----------|
| Routes | `apps/mobile/app/(app)/` — keep stack `headerShown: false` pattern; use **Appbar** inside screens like `organization.tsx`. |
| Lib | New API module under `apps/mobile/lib/`; optional `components/` for shared **EmptyState** if extracted. |

### Testing requirements

- Manual: Admin — list, invite (with and without password), change role, deactivate (and cancel dialog); Technician — no admin actions.
- Automated: Add tests only if project already has a runner pattern for RN (do not introduce a new framework in this story unless trivial).

### Previous story intelligence (2.4)

- API returns **`EmployeeUserResponse`** including **`accountStatus`** — UI must display deactivation state.
- **Stable error codes:** `LAST_ADMIN_DEACTIVATION`, `LAST_ADMIN_REMOVAL`, `USER_EMAIL_CONFLICT`, `FORBIDDEN_ROLE`, `TENANT_ACCESS_DENIED` — map `detail` for admins.
- **Filter note:** Story 2.4 **AccountStatusVerificationFilter** — deactivated users get **401** `ACCOUNT_DEACTIVATED` on protected routes; not central to this story but explains weird session errors if testing with deactivated accounts.

### Git intelligence (recent commits)

- **`f8238b9`** — Story **2.4**: deactivate endpoint, refresh revocation, `AccountStatusVerificationFilter` — **no** mobile changes.
- **`4e4844c`** — **2.3** PATCH role, last-admin **409**.
- **`57f2692`** — **2.2** create/invite API.
- **`b223548`** — **1.8** org profile mobile — **template** for Paper + query + mutation.

### Latest tech notes

- Expo SDK / React Native Paper versions: follow **existing** `package.json` — no major upgrades in this story.

### Project context reference

- No `project-context.md` in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + linked sources.

### Team / Technician IA (implementation)

- **Technician:** **`Team`** route uses **early return** — explanation only, **no** user list query (no admin API calls). Settings still lists **Team** for both roles so navigation stays coherent.

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

None.

### Completion Notes List

- Implemented **Settings** hub (`/settings`), **Team** management (`/team`), **`organization-users-api`** + **`organization-users-invite-schema`**, **`getSubjectFromAccessToken`** in `jwt-org.ts` for “You” chip.
- Home shell primary action is **Settings** (was Organization-only).
- **Manual test checklist:** (1) Admin: Settings → Team → list loads; Invite with/without password; invite token dialog; Change role (including last-admin error if applicable); Deactivate + cancel; Deactivate success. (2) Technician: Settings → Team → read-only explanation only. (3) Snackbar shows `ApiProblemError` detail on conflicts.
- **Automated:** `npm run typecheck` + `npm run lint` (mobile); `mvn test` (API, unchanged code).

### File List

- `apps/mobile/app/(app)/settings.tsx` (new)
- `apps/mobile/app/(app)/team.tsx` (new)
- `apps/mobile/app/(app)/index.tsx` (modified)
- `apps/mobile/lib/organization-users-api.ts` (new)
- `apps/mobile/lib/organization-users-invite-schema.ts` (new)
- `apps/mobile/lib/jwt-org.ts` (modified)
- `docs/rbac-matrix.md` (modified)

## Change Log

- **2026-03-30:** Story created (`bmad-create-story`).
- **2026-03-30:** validate-create-story (checklist) amendments.
- **2026-03-30:** Implemented Story 2.5 (Settings, Team, org users API client, rbac-matrix doc); mobile `typecheck` + `lint`; API `mvn test` green; status → **review**.
- **2026-03-30:** Code review patch pass: `jwt-org` JSDoc; Settings Team row copy by role; Team empty state when org id missing; status → **done**.

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-30 against `checklist.md`, `epics.md` Story 2.5, `OrganizationUsersController`, `OrganizationUserService`, `OrganizationUsersIT`, `organization.tsx`, and `apps/mobile/package.json`.

### Critical / traceability (addressed in file)

- **`USER_DEACTIVATED`:** Pinned for **PATCH role** on **`DEACTIVATED`** users — prevents implementer guessing **404** vs **409**; aligns with integration tests.
- **`USER_EMAIL_CONFLICT`:** Pinned; removed ambiguous “or current code” wording.
- **FR27 wording:** Narrowed to FR24–FR26 + explicit **2.6** boundary so dev does not implement “customer portal” checks here.
- **Clipboard:** Documented that **`expo-clipboard`** is not a current dependency — avoids silent missing-module failure if Copy is attempted.

### Enhancements applied

- **PATCH idempotency** (same role → 200) noted for UI optimization.
- **AC4** split into active-row vs deactivated-row behavior.

### Residual risks (acceptable for ready-for-dev)

- **JWT role lag** after self-demotion — still requires manual test / possible sign-out; story already notes.
- **No mobile E2E** in repo — manual checklist remains primary unless Maestro/Appium added later.
- **UX-DR17** is referenced from epics; no separate `UX-DR17` section in `ux-design-specification.md` by that token — epics traceability is the anchor.

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — points to existing API module, `organization.tsx`, `org-profile-api` |
| Wrong libraries | Pass — stack matches `package.json`; clipboard called out |
| File locations | Pass — `app/(app)/`, `lib/` |
| Regression risk | Pass — client-only; API unchanged |
| UX | Pass — Paper, Dialog, feedback, Technician copy |
| Vague AC | Addressed — API codes pinned |
| Previous story continuity | Pass — 2.2–2.4 cited |

### Review Findings

- [x] [Review][Patch] Fix JSDoc placement in `jwt-org.ts` — the “Read role claim…” block precedes `getSubjectFromAccessToken` (wrong); `getRoleFromAccessToken` should retain the role-doc; document `getSubjectFromAccessToken` with its own `sub` claim line. [apps/mobile/lib/jwt-org.ts:19-35] — fixed 2026-03-30.

- [x] [Review][Patch] Role-gate the **Team** row description on **Settings** — technicians still see “Invite employees, roles, and access,” which conflicts with AC6 (no invite/admin affordance at this layer). Use `getSessionRole()` (or equivalent) to show neutral copy for `TECHNICIAN` (e.g. “Who’s on the team”) while keeping admin-oriented copy for `ADMIN`. [apps/mobile/app/(app)/settings.tsx:29-35] — fixed 2026-03-30.

- [x] [Review][Patch] When `isAdmin` but `getSessionOrganizationId()` is null, `useQuery` is disabled and the main area can render empty (no loading, error, or empty-state copy). Show a short inline message and/or retry aligned with session recovery. [apps/mobile/app/(app)/team.tsx:68-72] — fixed 2026-03-30.

- [x] [Review][Defer] No automated mobile tests for Settings/Team routes — [apps/mobile/app/(app)/settings.tsx], [apps/mobile/app/(app)/team.tsx] — deferred; story allows manual checklist; same as prior mobile stories unless Maestro/E2E is introduced.
