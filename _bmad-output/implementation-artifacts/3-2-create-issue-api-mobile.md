# Story 3.2: Create issue (API + mobile)

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. validate-create-story (checklist.md): passed 2026-03-31 — see Validation record at end. -->

## Story

As a **Technician or Admin**,
I want **to create an issue with required MVP fields and customer/site context**,
so that **work is captured in the field (FR11, FR12)**.

**Implements:** **FR11**, **FR12**; **UX-DR2** (FAB entry), **UX-DR13** (React Hook Form + Zod), **UX-DR6**, **UX-DR9** (honest loading/error feedback).

**Epic:** Epic 3 — Issues — capture, triage, ownership & history.

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 3.2])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** authenticated user with create permission **When** they submit create with required fields **Then** issue is persisted org-scoped; Problem Details on validation errors; mobile shows honest pending/success/failure | **API:** `POST /api/v1/organizations/{organizationId}/issues` calls **`IssueService.createIssue`** (no duplicate persistence logic in controller); **`@Valid`** request DTO → **400** `VALIDATION_ERROR` via existing [`ApiExceptionHandler`](../../apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java); **Mobile:** TanStack Query **mutation** with **pending / error / success** UI (Snackbar or Banner + disabled submit while pending). |
| **Given** OpenAPI **When** contract is generated or hand-written **Then** Zod schemas align with create payload | **Springdoc** documents request/response records; **mobile** `IssueCreateSchema` (Zod) fields **match** JSON property names and enums **string values** (`OPEN`, `MEDIUM`, …). |

### Cross-story boundaries

| Story | Relationship |
|-------|--------------|
| **3.1** | **Uses** [`IssueService.createIssue`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) — **do not** reimplement history or org scoping in the controller. |
| **3.3** | Will replace **stub** issue **list** with real data; **3.2** may keep **GET** list as **empty stub** until 3.3. |
| **3.5–3.6** | **Admin reassign** stub on `IssueStubController` stays stub until those stories. |

## Acceptance Criteria

1. **REST — create issue (replace stub POST)**  
   **Given** a valid **Bearer** JWT with `organizationId` claim matching path  
   **When** `POST /api/v1/organizations/{organizationId}/issues` is called with a JSON body satisfying validation  
   **Then** response is **201 Created** with a body that includes at least **`id`** (UUID) and fields needed for the mobile success screen (recommend: **`id`**, **`title`**, **`status`**, **`priority`**, **`createdAt`**, plus optional **`description`**, **`customerLabel`**, **`siteLabel`**, **`assigneeUserId`** if present)  
   **And** the issue is persisted with **`organization_id`** from the path and **actor** = JWT **`sub`** (user id) for the **CREATED** history event (already enforced inside **`IssueService`**)  
   **And** **`TenantPathAuthorization.requireJwtOrganizationMatchesPath`** runs before business logic (same pattern as [`IssueStubController`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java))  
   **And** **Admin** and **Technician** are both allowed for create — **no** `RoleAuthorization.requireAdmin` on this route ([Source: `docs/rbac-matrix.md`](../../docs/rbac-matrix.md)).

2. **REST — validation (Problem Details)**  
   **Given** invalid body (missing required fields, blank title, oversize strings) caught by **`@Valid`** / bean validation  
   **When** the request is processed  
   **Then** response is **400** with **`application/problem+json`**, type **`urn:mowercare:problem:VALIDATION_ERROR`**, **`code`:** `VALIDATION_ERROR` — [`MethodArgumentNotValidException`](../../apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java) handler.  
   **And** **Given** malformed JSON or **unparseable enum** string (e.g. `"status":"NOT_A_STATUS"`) **when** Spring cannot bind the body **then** either add an **`HttpMessageNotReadableException`** (or equivalent) handler that returns **Problem Details** with a stable **`code`** (e.g. `VALIDATION_ERROR` or `BAD_REQUEST`) **or** document the actual default error shape — **do not** assume `VALIDATION_ERROR` without a handler (currently **no** dedicated handler in [`ApiExceptionHandler`](../../apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java)).

3. **REST — actor and assignee resolution**  
   **Given** JWT subject is the acting user  
   **When** create runs  
   **Then** **`IssueService.createIssue(organizationId, actorUserId, …)`** receives **`UUID.fromString(jwt.getSubject())`** (same approach as [`OrganizationUserService`](../../apps/api/src/main/java/com/mowercare/service/OrganizationUserService.java))  
   **And** optional **`assigneeUserId`** in JSON is passed through; **`null`/omitted** means unassigned (service already uses **`resolveAssigneeOrNull`**).  
   **And** **Given** **`assigneeUserId`** references a user **not** in the organization **when** create runs **then** [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) throws **`ResourceNotFoundException`** (“Assignee not found”) → **404** Problem Details **`code`:** `NOT_FOUND` ([`ApiExceptionHandler`](../../apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java)) — mobile must surface this (not only 400).

4. **OpenAPI / Springdoc**  
   **Given** the new request/response types are annotated with **`@Schema`**  
   **When** `/v3/api-docs` is generated  
   **Then** create issue documents **request body** and **201 response**; enums for **`IssueStatus`** / **`IssuePriority`** appear as **string** enums for client alignment.

5. **Mobile — navigation shell for create**  
   **Given** authenticated employee  
   **When** they open the **Issues** area  
   **Then** there is a **primary path** to **Create issue** (**FAB** “New issue” on Issues home per **UX-DR2** / Direction A — [Source: `ux-design-specification.md`](../planning-artifacts/ux-design-specification.md))  
   **And** **Story 3.3** will flesh out the full list — **3.2** may show **empty placeholder** or minimal copy on Issues index if list data is not yet implemented.

6. **Mobile — form and client validation**  
   **Given** Create issue screen  
   **When** user edits fields  
   **Then** **React Hook Form** + **Zod** validate before submit (**UX-DR13**); schema aligns with API (required **title**, **status**, **priority**; optional **description**, **customerLabel**, **siteLabel**, **assigneeUserId**).

7. **Mobile — mutation honesty**  
   **Given** submit pressed  
   **When** request is in flight / succeeds / fails  
   **Then** UI shows **loading** (disable submit, optional `ActivityIndicator`), **success** feedback (Snackbar or navigate back with confirmation), **error** from **`ApiProblemError`** using **`code`** / **`detail`** where useful (**UX-DR6**, **UX-DR9**).  
   **And** **404** (`NOT_FOUND`) is possible for invalid assignee — show a clear message (not a generic “validation” error).

8. **Tests**  
   **Given** existing API test patterns  
   **When** `mvn test` runs  
   **Then** at least one **MockMvc** (or REST-assured style) integration test covers **201** happy path + **400** validation failure for create issue  
   **And** mobile: **`npm run typecheck`** (and **`npm run lint`** if feasible) passes for new files.

## Tasks / Subtasks

- [x] **API — DTOs** (AC: 1–4)
  - [x] Add `IssueCreateRequest` in `com.mowercare.model.request` — `@NotBlank` / `@Size` on **title**; **`IssueStatus`** / **`IssuePriority`** `@NotNull`; optional **description**, **assigneeUserId**, **customerLabel**, **siteLabel** with appropriate **`@Size`** (align with [`Issue`](../../apps/api/src/main/java/com/mowercare/model/Issue.java) column lengths)
  - [x] Add `IssueCreatedResponse` (or equivalent) in `com.mowercare.model.response` — fields for 201 body + `@Schema`

- [x] **API — controller** (AC: 1–4)
  - [x] Replace **stub** `POST …/issues` implementation: call **`IssueService.createIssue`**, map enums and nulls; return **201**
  - [x] Keep **`GET …/issues`** as **stub empty list** until Story **3.3**; keep **`POST …/issues/_admin/reassign`** stub until later epic stories
  - [x] Refactor naming/tag: e.g. split **`IssueStubController`** into focused classes **or** rename tag to reflect “Issues” with documented stub endpoints — **do not** leave misleading OpenAPI text saying “stub” for the **create** operation once real

- [x] **API — tests** (AC: 8)
  - [x] New IT or extend existing controller IT class — follow [`TenantScopeIT`](../../apps/api/src/test/java/com/mowercare/controller/TenantScopeIT.java) / [`OrganizationUsersIT`](../../apps/api/src/test/java/com/mowercare/controller/OrganizationUsersIT.java) patterns: JWT builder, tenant path, JSON body
  - [x] Optional but recommended: **404** when **`assigneeUserId`** is another org’s user or unknown user id; **400** bean validation vs **binding** error behavior per AC2

- [x] **Docs** (AC: 1)
  - [x] Update [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md): note **POST create** is **real** (no longer stub-only row wording)

- [x] **Mobile — routes + UI** (AC: 5–8)
  - [x] Add **`app/(app)/issues/`** Expo Router segment: **`_layout.tsx`** (Stack), **`index.tsx`** (Issues home with **FAB** → create), **`create.tsx`** (form)
  - [x] Wire navigation from existing shell (e.g. **App home** button or redirect) so Issues is reachable without hacks
  - [x] **`useMutation`** + **`authenticatedFetchJson`** for `POST …/issues`
  - [x] Zod schema **`issueCreateSchema`** + RHF; map enum strings to match Java **`Enum.name()`**

- [x] **Verification**
  - [x] `mvn -q test` in `apps/api`
  - [x] `npm run typecheck` in `apps/mobile`

## Dev Notes

### Scope boundaries

- **In scope:** Real **create** HTTP contract, **mobile** create flow + validation + mutation UX, **rbac-matrix** update for create route.
- **Out of scope:** Full **issue list** (3.3), **detail** (3.4), **update** (3.5), **AssigneePicker** (3.6), **notifications** (Epic 4).

### Previous story intelligence (3.1)

- **`IssueService.createIssue`** validates non-null **title**, **status**, **priority**; emits **CREATED** history — [Source: `3-1-issue-aggregate-schema-and-change-history-storage.md`](./3-1-issue-aggregate-schema-and-change-history-storage.md).
- Packages: **`com.mowercare.model`**, **`repository`**, **`service`** — **no** new `com.mowercare.issue` package.
- **`IssueStubController`** had TODO for 3.2 — replace **POST** with real behavior.

### Architecture compliance

| Topic | Source |
|-------|--------|
| REST + OpenAPI + Problem Details | [`architecture.md`](../planning-artifacts/architecture.md) |
| Path params **camelCase** `{organizationId}` | [`architecture.md` — API conventions](../planning-artifacts/architecture.md) |
| Layering: controller → **IssueService** → repository | [`architecture.md` — Layering](../planning-artifacts/architecture.md) |
| Mobile: TanStack Query, RHF + Zod, Paper | [`architecture.md`](../planning-artifacts/architecture.md) |

### Domain defaults (product-aligned)

| Field | MVP note |
|-------|----------|
| **status** | Require in JSON **or** document server default — if omitted, prefer **explicit `OPEN`** in API contract for clarity with Zod |
| **priority** | Same — enums are **`IssueStatus`** / **`IssuePriority`** in [`model`](../../apps/api/src/main/java/com/mowercare/model/) |
| **description** | Nullable |
| **customerLabel** / **siteLabel** | Map to DB **`customer_label`** / **`site_label`**; JSON names **camelCase** |

### Existing code to reuse

| Asset | Notes |
|-------|--------|
| [`IssueService.createIssue`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) | Single entry for persistence + history |
| [`ApiExceptionHandler`](../../apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java) | **400** validation |
| [`authenticatedFetchJson`](../../apps/mobile/lib/api.ts) | Bearer + refresh retry |
| [`ApiProblemError`](../../apps/mobile/lib/http.ts) | Typed error for UI |
| [`OrganizationProfileController`](../../apps/api/src/main/java/com/mowercare/controller/OrganizationProfileController.java) | `@Valid` + OpenAPI patterns |

### File structure requirements

| Area | Guidance |
|------|----------|
| API requests/responses | `apps/api/src/main/java/com/mowercare/model/request/`, `.../response/` |
| API controller | `apps/api/src/main/java/com/mowercare/controller/` — align with existing stub or rename for clarity |
| Mobile | `apps/mobile/app/(app)/issues/`, `apps/mobile/lib/` for `issue-create-schema.ts` or colocate with screen |

### Testing requirements

- **API:** Assert **201** + JSON shape; **400** with **`VALIDATION_ERROR`** for **`@Valid`** failures; **403** tenant mismatch can reuse patterns from other controller ITs; **404** **`NOT_FOUND`** for invalid assignee per **`IssueService.resolveAssigneeOrNull`**.
- **Mobile:** Prefer **typecheck** + minimal lint; E2E optional (not required by epics for 3.2).

### Error semantics (prevent wrong expectations)

| Condition | HTTP | `code` (typical) |
|-----------|------|------------------|
| Bean validation (`@Valid`) | 400 | `VALIDATION_ERROR` |
| Assignee not in org | 404 | `NOT_FOUND` |
| Actor/org missing in DB | 404 | `NOT_FOUND` |
| JWT org ≠ path | 403 | `TENANT_ACCESS_DENIED` |
| Unparseable JSON / enum | 400 | `VALIDATION_ERROR` via `HttpMessageNotReadableException` handler in `ApiExceptionHandler` |

### Git intelligence (recent commits)

- **`dce71b8`** — Story **3.1** issue persistence and change history — **`IssueService`**, Liquibase **`issues`** / **`issue_change_events`**.

### Latest tech notes

- Spring Boot **3.x** + **springdoc-openapi** already in project — extend annotations only.
- Mobile stack: **Expo 55**, **Zod 4**, **RHF 7**, **TanStack Query 5** — match existing **`package.json`** versions.

### Project context reference

- No `project-context.md` — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + [`epics.md`](../planning-artifacts/epics.md).

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

None.

### Completion Notes List

- **POST `/issues`:** `IssueStubController` calls `IssueService.createIssue`; response uses **`body.assigneeUserId()`** for `assigneeUserId` in `IssueCreatedResponse` to avoid lazy-loading assignee after the transaction.
- **`ApiExceptionHandler`:** `HttpMessageNotReadableException` → **400** `VALIDATION_ERROR` (AC2 / bad enum JSON).
- **Tests:** `IssueCreateIT` (201, 400 validation, 400 bad enum, 403 tenant, 404 assignee); `RbacEnforcementIT` POST expectations updated for real create.
- **Mobile:** Issues stack, FAB → create, RHF+Zod+mutation, `Alert` on success, Snackbar on API error with `NOT_FOUND` messaging for assignee.

### File List

- `apps/api/src/main/java/com/mowercare/model/request/IssueCreateRequest.java`
- `apps/api/src/main/java/com/mowercare/model/response/IssueCreatedResponse.java`
- `apps/api/src/main/java/com/mowercare/controller/IssueStubController.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/test/java/com/mowercare/controller/IssueCreateIT.java`
- `apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java`
- `apps/mobile/lib/issue-create-schema.ts`
- `apps/mobile/lib/issue-api.ts`
- `apps/mobile/app/(app)/issues/_layout.tsx`
- `apps/mobile/app/(app)/issues/index.tsx`
- `apps/mobile/app/(app)/issues/create.tsx`
- `apps/mobile/app/(app)/index.tsx`
- `docs/rbac-matrix.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

## Change Log

- **2026-03-31:** Implemented Story 3.2 — real issue create API + mobile flow; `mvn test` and mobile `typecheck`/`lint` green; status → **review**.
- **2026-03-31:** `validate-create-story` — AC2 clarified (bean validation vs JSON binding); AC3/7 + tasks + error table for **404** assignee; optional IT for binding errors.
- **2026-03-31:** Story created — `bmad-create-story` auto-discover (first backlog: **3-2**).

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-31 against `checklist.md`, `epics.md` Story **3.2**, `IssueService`, `ApiExceptionHandler`, `IssueStubController`.

### Critical (addressed in file)

| Issue | Resolution |
|-------|--------------|
| **404** for invalid **assignee** not called out | **`IssueService.resolveAssigneeOrNull`** throws **`ResourceNotFoundException`** → **404** `NOT_FOUND`. Added to AC3, AC7, testing + **Error semantics** table. |
| AC2 implied **unknown enum** → **`VALIDATION_ERROR`** | Only **`MethodArgumentNotValidException`** maps to that **`code`** today. AC2 split + task to add **`HttpMessageNotReadableException`** handler or document actual response. |

### Enhancements applied

| Addition | Benefit |
|----------|---------|
| **Error semantics** table | Dev agent maps HTTP + `code` without guessing. |
| Optional IT row for assignee **404** / binding | Reduces shipping silent mobile bugs. |

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — **`IssueService.createIssue`** remains single write path |
| Technical accuracy | Fixed — enum/validation vs **`NOT_FOUND`** |
| File locations | Pass — matches repo layout |
| Regression / scope | Pass — list stub + admin reassign stub unchanged |
| Previous story continuity | Pass — builds on **3.1** |

### Residual risks (acceptable for ready-for-dev)

- _None —_ **`HttpMessageNotReadableException`** is handled (AC2).

## Clarifications / questions saved for end

_None._

### Review Findings

- [x] [Review][Patch] Integration test DB teardown must delete **`issue_change_events`** and **`issues`** before **`users`** after Story 3.1 FKs — [`IssueCreateIT.java`](../../apps/api/src/test/java/com/mowercare/controller/IssueCreateIT.java), [`RbacEnforcementIT.java`](../../apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java) (same order as [`IssueServiceIT`](../../apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java)). **Fixed during code review** (was failing with `fk_issue_change_events_actor_user` on `userRepository.deleteAll()`).

- [x] [Review][Patch] Story **Error semantics** table and **Residual risks** still described JSON binding as TBD — this file updated to match `ApiExceptionHandler`.

- [x] [Review][Defer] Controller class name remains **`IssueStubController`** while POST create is live — optional rename/split per story tasks; OpenAPI tag/description already updated. — deferred, pre-existing naming debt
