# Story 2.6: Employee-only access guardrails

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. validate-create-story (checklist.md): passed with amendments 2026-03-30 — see Validation record at end. -->

## Story

As a **product**,
I want **the client and API to reject non-employee use cases for interactive access**,
so that **v1 remains employee-only (FR27)**.

**Implements:** **FR27** (employees only; no end-customer accounts or client access).

**Epic:** Epic 2 — Organization admin, roles & access control — **closes** the “employee-only” posture with **documentation + automated guardrails**, building on bootstrap-only org creation ([`1-3-bootstrap-first-organization-and-admin-user.md`](./1-3-bootstrap-first-organization-and-admin-user.md)), admin provisioning ([`2-2`](./2-2-invite-or-create-employee-user.md)–[`2-5`](./2-5-admin-settings-ui-invites-roles-deactivate.md)), and existing copy on auth ([`apps/mobile/app/(auth)/index.tsx`](../../apps/mobile/app/(auth)/index.tsx)).

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.6])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** product documentation and API error messages **When** a flow might resemble “customer access” **Then** there is no customer registration or portal; acceptance tests assert public customer routes are absent | **Docs** state employee-only model; **API** integration test proves representative “customer signup / portal” paths are **not** mapped (404) or are **not** in the public surface; **OpenAPI** / controller tags remain consistent with **employee** provisioning (no generic “Sign up” for end users) |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **2.2–2.5** | **Do not** change invite/create semantics — `accept-invite` is **employee invite completion**, not a customer portal; this story **documents** that and **locks** absence of other public registration routes. |
| **1.3 / bootstrap** | **Only** operator-controlled org creation via `POST /api/v1/bootstrap/organization` + `X-Bootstrap-Token` — not contradicted here; tests must **not** break bootstrap. |

## Acceptance Criteria

1. **Documented access model (repo)**  
   **Given** a reader opens the main product docs  
   **When** they look for how accounts are created  
   **Then** they find an explicit **employee-only** statement: **no** public self-registration and **no** end-customer app access in v1, with pointers to bootstrap (first org) and **Admin**-provisioned employees (invite/create).  
   **Suggested locations:** extend [`README.md`](../../README.md) (short “Access model” subsection) and/or add a focused doc under [`docs/`](../../docs/) (e.g. `employee-only-access.md`) and cross-link from [`docs/account-data-pii.md`](../../docs/account-data-pii.md) if appropriate.

2. **API — no “customer signup” routes**  
   **Given** the Spring MVC application  
   **When** automated tests run  
   **Then** tests prove there is **no** successful customer self-registration endpoint (`200/201` with a “sign up” semantics). Use **two** cases (Spring Security order matters):  
   - **Paths under** [`/api/v1/auth/**`](../../apps/api/src/main/java/com/mowercare/config/SecurityConfig.java) **(permitAll)** with **no** controller mapping — e.g. `POST /api/v1/auth/register`, `POST /api/v1/auth/signup` — expect **`404`** (DispatcherServlet: no handler).  
   - **Paths outside** `permitAll` (e.g. `POST /api/v1/customers`, `POST /api/v1/public/signup`) **without** JWT — expect **`401`** (authentication required **before** MVC dispatch); this still proves the route is **not** a public customer signup.  
   **Optional:** Issue the same **non-auth** requests **with** a valid **employee** JWT — if still **404**, the handler truly does not exist (stronger than 401-only for those paths).  
   **Note:** Keep the forbidden path list **small and intentional**; do not add brittle substring scans over all controllers unless you adopt an explicit registry pattern.

3. **API — allowlist sanity (optional but valuable)**  
   **Given** `SecurityConfig` [`permitAll`](../../apps/api/src/main/java/com/mowercare/config/SecurityConfig.java) rules  
   **When** reviewed alongside tests  
   **Then** public surface remains **auth** (`/api/v1/auth/**` — login, refresh, logout, accept-invite), **bootstrap** (`/api/v1/bootstrap/**`), and **OpenAPI/Swagger** docs — **no** new public route introduces customer self-registration (if a new `permitAll` is added later, it must be reviewed against FR27).

4. **API — OpenAPI / controller language**  
   **Given** OpenAPI is generated or hand-maintained  
   **When** auth and org-user operations are inspected  
   **Then** summaries/descriptions **do not** read like a consumer “create your account” marketplace; **employee** and **organization** context matches existing patterns (see [`AuthController`](../../apps/api/src/main/java/com/mowercare/controller/AuthController.java), [`OrganizationUsersController`](../../apps/api/src/main/java/com/mowercare/controller/OrganizationUsersController.java)).

5. **Mobile — no customer registration UX**  
   **Given** the Expo Router tree under [`apps/mobile/app/`](../../apps/mobile/app/)  
   **When** the app is reviewed  
   **Then** there is **no** screen or route whose **primary** purpose is end-customer sign-up or a “customer portal” (e.g. no `(auth)/register.tsx` for public signup). The **sign-in** screen may add a **single line** of copy reinforcing **employee / org** use if not already clear enough (today: *“Sign in with your organization ID and employee credentials”* — already strong).  
   **Verification:** document a **short manual checklist** in Completion Notes; automated UI tests are **optional** (repo has **no** mobile test runner today per Story 2.5 pattern).

6. **RBAC doc**  
   **Given** [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md)  
   **When** updated for this story  
   **Then** the **Mobile** or intro subsection briefly states **FR27**: interactive app use is for **org employees** only (aligns with PRD).

## Tasks / Subtasks

- [x] **Documentation** (AC: 1, 6)
  - [x] Add **Access model** / **FR27** prose to `README.md` and/or new `docs/employee-only-access.md`; link from `docs/account-data-pii.md` where it helps.
  - [x] Update `docs/rbac-matrix.md` with a concise FR27 line.

- [x] **API integration tests** (AC: 2, 3)
  - [x] Add a dedicated test class (e.g. `EmployeeOnlyAccessGuardrailsIT` or extend an existing `@SpringBootTest` + `MockMvc` suite) using **Testcontainers** pattern like other ITs ([`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java)).
  - [x] Assert **404** for unmapped paths under **`/api/v1/auth/**`**; assert **401** (no JWT) for unmapped paths **outside** `permitAll` (see Technical requirements); ensure tests fail if someone adds a **successful** customer registration flow.
  - [x] Optionally assert **allowed** public auth paths still respond **without** JWT (e.g. `POST /api/v1/auth/login` with bad body → **400** — proves endpoint exists — use minimal valid structure per existing tests).

- [x] **OpenAPI / annotations pass** (AC: 4)
  - [x] Skim `AuthController`, `OrganizationUsersController`, `BootstrapController` for any wording that implies customer self-service; adjust **only** if something misleading.

- [x] **Mobile review** (AC: 5)
  - [x] Confirm route/file list under `app/` has no public registration flow; tweak subtitle copy only if product wants stronger FR27 language.
  - [x] Record manual verification in **Dev Agent Record**.

## Dev Notes

### Scope boundaries

- **In scope:** Documentation, **API tests** for absence of customer-style routes, light **copy** / **OpenAPI** alignment, **rbac-matrix** touch-up.
- **Out of scope:** New business features, new endpoints, changing invite/bootstrap semantics, Epic 3 issues.

### Architecture compliance

- **Access model:** [Source: `architecture.md` — employees only, no end-customer accounts v1; mobile-first, no web admin].
- **Security:** JWT on protected routes; `permitAll` only for auth, bootstrap, docs — [Source: `SecurityConfig.java`].
- **Bootstrap:** Single org creation path — [Source: `BootstrapController`, `BootstrapService`].

### Existing code to reuse

| Asset | Use |
|-------|-----|
| [`SecurityConfig`](../../apps/api/src/main/java/com/mowercare/config/SecurityConfig.java) | Reference for what is intentionally public (`permitAll` vs `authenticated`) — drives **401 vs 404** for unmapped paths |
| [`AuthController`](../../apps/api/src/main/java/com/mowercare/controller/AuthController.java) | Canonical `/api/v1/auth/*` operations |
| [`BootstrapController`](../../apps/api/src/main/java/com/mowercare/controller/BootstrapController.java) | Only org bootstrap path — not FR27 “customer” signup |
| [`OrganizationUsersIT`](../../apps/api/src/test/java/com/mowercare/controller/OrganizationUsersIT.java) | Patterns for `MockMvc`, Problem Details, Testcontainers |

### Technical requirements

| Area | Requirement |
|------|-------------|
| Tests | Use same **JDK 25 + Maven + Docker** assumptions as `apps/api` tests; do not weaken tenant or auth tests elsewhere. |
| 404 vs 401 | Under **`/api/v1/auth/**` (permitAll)**, unmapped → **404**. Under **any other** `/api/v1/**` path, **no JWT** → **401** from Spring Security (handler may not run). **Do not** assert “404 only” for every invented path without checking `SecurityConfig`. |
| `accept-invite` | This **is** a legitimate **unauthenticated** path for **employees** completing invites — **not** a customer portal; do **not** remove or block it in FR27 tests. |

### Library / framework requirements

| Layer | Notes |
|-------|-------|
| API | JUnit 5, Spring Boot test, MockMvc, Testcontainers — match existing `pom.xml` |

### File structure requirements

| Area | Guidance |
|------|----------|
| New tests | `apps/api/src/test/java/com/mowercare/...` — follow `*IT` naming beside other controllers |
| Docs | `docs/` at repo root; keep README section short |

### Testing requirements

- **Required:** `mvn -q test` (or project-standard `mvn verify`) green for `apps/api` with new IT.
- **Mobile:** `npm run lint` + `npm run typecheck` unchanged or green if copy edits touch TSX.

### Previous story intelligence (2.5)

- Mobile **Settings / Team** is **Admin**-gated; **Technician** sees explanation — aligns with FR27 (employees only, but not all employees are admins).
- Story 2.5 deferred **automated mobile E2E** — same applies here.

### Git intelligence (recent commits)

- **`a6e9bd4`** — Story **2.5** mobile Settings/Team — baseline routes under `app/(app)/`.
- **`f8238b9`** — **2.4** deactivate + filters.
- Prior Epic 2 work established **RBAC** and **org-scoped** admin APIs.

### Latest tech notes

- No dependency upgrades required for this story.

### Project context reference

- No `project-context.md` in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + linked sources.

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

None.

### Completion Notes List

- **Docs:** Added [`README.md`](../../README.md) “Access model (FR27)”; new [`docs/employee-only-access.md`](../../docs/employee-only-access.md); links from [`account-data-pii.md`](../../docs/account-data-pii.md); [`rbac-matrix.md`](../../docs/rbac-matrix.md) intro + Mobile FR27 line.
- **API:** `EmployeeOnlyAccessGuardrailsIT` — 404 for `POST /api/v1/auth/register` and `/auth/signup`; 401 for `POST /api/v1/customers` and `/public/signup` without JWT; 400 for `POST /api/v1/auth/login` with `{}` (validates real login handler).
- **OpenAPI / controllers:** Reviewed `AuthController`, `OrganizationUsersController`, `BootstrapController` — employee/bootstrap wording already correct; **no** code annotations changed.
- **Mobile manual check:** Routes under `apps/mobile/app/`: `index.tsx`, `(auth)/index.tsx`, `(auth)/_layout.tsx`, `(app)/*` — **no** `register` or customer signup screen; added one **bodySmall** line on sign-in reinforcing no public sign-up.
- **Verification:** `mvn test` (`apps/api`); `npm run lint` + `npm run typecheck` (`apps/mobile`).

### File List

- `README.md`
- `docs/employee-only-access.md`
- `docs/account-data-pii.md`
- `docs/rbac-matrix.md`
- `apps/api/src/test/java/com/mowercare/controller/EmployeeOnlyAccessGuardrailsIT.java`
- `apps/mobile/app/(auth)/index.tsx`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Review Findings

- [x] [Review][Patch] Sprint `last_updated` timestamp regressed — `_bmad-output/implementation-artifacts/sprint-status.yaml` changed `last_updated` from `2026-03-30T23:55:00Z` to `2026-03-30T20:45:00Z` (backward move). Restore a monotonic value (e.g. restore `23:55` or set to the actual transition time) so tracking metadata stays trustworthy. [`sprint-status.yaml`:1-2] — fixed 2026-03-30 (restored `23:55` UTC).

## Change Log

- **2026-03-30:** Story created (`bmad-create-story`).
- **2026-03-30:** validate-create-story — AC2 / Tasks / Technical requirements amended (401 vs 404); Validation record expanded; BootstrapController cross-link.
- **2026-03-30:** Implemented Story 2.6 — docs, `EmployeeOnlyAccessGuardrailsIT`, mobile sign-in copy; `mvn test` + mobile lint/typecheck green; status → **review**.
- **2026-03-30:** Code review — patch applied (`sprint-status` `last_updated` restored); status → **done**.

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-30 against `checklist.md`, `epics.md` Story **2.6**, `SecurityConfig.java`, and `2-5` story patterns.

### Critical (addressed in file)

| Issue | Resolution |
|-------|------------|
| **404 vs 401** | Original AC2 implied **404** for all sample paths; for routes **outside** `permitAll`, unauthenticated requests get **401** before MVC. AC2, Tasks, and Technical requirements updated to split **auth/\*\*** (404 if unmapped) vs **other** `/api/v1/**` (401 without JWT, or optional authenticated probe for 404). |
| **Epic alignment** | Epics AC is high-level (“tests assert public customer routes absent”); story now encodes **Spring Security–accurate** expectations so the dev agent does not write failing tests. |

### Enhancements applied

- **`BootstrapController`** added to “Existing code to reuse.”
- **`SecurityConfig`** row now ties **permitAll** to the 401/404 distinction.

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — points to existing IT patterns, controllers |
| Wrong test expectations | Fixed — 401/404 split |
| File locations | Pass — `apps/api/src/test/...`, `docs/` |
| Regression risk | Pass — additive tests + docs |
| Previous story continuity | Pass — 2.5 mobile test deferral repeated |
| Vague AC | Improved — security-aware acceptance criteria |

### Residual risks (acceptable for ready-for-dev)

- Exact **forbidden path** list is product choice; keep it minimal.
- **Git SHAs** in “Git intelligence” may age; re-verify with `git log` when implementing.

## Clarifications / questions saved for later

_None — story is ready for dev._
