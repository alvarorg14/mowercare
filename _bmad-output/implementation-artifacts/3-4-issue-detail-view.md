# Story 3.4: Issue detail view

Status: review

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. validate-create-story (checklist.md): passed 2026-03-31 — see Validation record at end. -->

## Story

As a **Technician or Admin**,
I want **to open an issue and see full context**,
so that **I can act on the right job (FR14)**.

**Implements:** **FR14**; **UX-DR18** (sticky title / id context while scrolling long content).

**Epic:** Epic 3 — Issues — capture, triage, ownership & history.

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 3.4])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** user has visibility **When** they open detail **Then** fields match API; long content scrolls with **sticky header** context | **API:** **`GET …/issues/{issueId}`** returns full issue payload **org-scoped**; **Mobile:** **`[id].tsx`** replaces placeholder — **`useQuery`**, **scrollable body**, **Appbar** shows title (and id snippet as needed) **fixed** above scroll |
| **Given** user lacks visibility **When** deep link or guess UUID **Then** **403**/**404** per policy + Problem Details | **API:** no cross-tenant leakage — **404** when issue not in org (same as internal **`loadIssue`** semantics); **403** when JWT org ≠ path; **Mobile:** map errors to copy + **Retry** + back to list |

### Cross-story boundaries

| Story | Relationship |
|-------|--------------|
| **3.3** | **Replaces** **`[id].tsx` placeholder**; reuse **status/priority presentation** from **[`IssueRow`](../../apps/mobile/components/IssueRow.tsx)** (**`Chip`** + **`issueStatusTokens`**, same labels) — **do not** invent separate **`IssueStatusChip`** / **`PriorityBadge`** components unless you extract shared helpers from **`IssueRow`** in this story; reuse **`listIssues`** query cache where helpful (`queryClient.getQueryData` optional — not required). |
| **3.5** | **Edits** (status, fields, resolve) — **out of scope** for **3.4**; detail is **read-only**. No mutation buttons required; optional “Edit” stub is **discouraged** unless product wants a disabled hint. |
| **3.6** | **AssigneePicker** — **out of scope**. |
| **3.7** | **Activity/history timeline** — **out of scope**; **UX-DR18** sticky header still applies when **description** is long (history comes later). |
| **4.5** | Deep link from push — **3.4** should handle **invalid id** / **not found** gracefully (aligns with epics **Story 3.4** second AC). |

## Acceptance Criteria

1. **REST — get issue by id (new)**  
   **Given** valid **Bearer** JWT with **`organizationId`** claim matching path  
   **When** `GET /api/v1/organizations/{organizationId}/issues/{issueId}` is called with valid UUID **`issueId`**  
   **Then** response is **200** with JSON body containing at least: **`id`**, **`title`**, **`status`**, **`priority`**, **`description`** (nullable), **`customerLabel`**, **`siteLabel`**, **`assigneeUserId`**, **`assigneeLabel`** (nullable — email MVP, consistent with list), **`createdAt`**, **`updatedAt`** (ISO-8601 UTC with **`Z`**)  
   **And** **`TenantPathAuthorization.requireJwtOrganizationMatchesPath`** runs before the read  
   **And** **Admin** and **Technician** are both allowed (**same** as list — [Source: `docs/rbac-matrix.md`](../../docs/rbac-matrix.md))  
   **And** OpenAPI documents the response type (new **`IssueDetailResponse`** or equivalent record in `com.mowercare.model.response`).

2. **REST — not found and tenant safety**  
   **Given** **`issueId`** does not exist **or** belongs to **another** organization (internal query is **`findByIdAndOrganization_Id`**)  
   **When** GET is called  
   **Then** response is **404** with **RFC 7807** Problem Details (existing **`ResourceNotFoundException`** pattern — do **not** leak cross-tenant existence)  
   **Given** JWT org ≠ path **`organizationId`**  
   **When** GET is called  
   **Then** **403** with existing tenant denial behavior (see **`TenantScopeIT`** / **`RbacEnforcementIT`** patterns).

3. **Service layer**  
   **Given** [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) already has private **`loadIssue(issueId, organizationId)`** used by update helpers  
   **When** implementing GET  
   **Then** expose a **public** **`@Transactional(readOnly = true)`** method (e.g. **`getIssue`**) that loads by org + id and maps to the detail DTO — **reuse** mapping logic with **`toListItem`** where possible to avoid field drift (extract shared private mapper if needed)  
   **And** use **`@EntityGraph`** or **`JOIN FETCH`** for **`assignee`** on the single-issue read so **`assigneeLabel`** does not lazy-load outside transaction.

4. **Controller**  
   **Given** [`IssueStubController`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java) hosts issue routes today  
   **When** adding GET by id  
   **Then** add mapping **`GET /{organizationId}/issues/{issueId}`** with **`@PathVariable`** UUIDs, **`@AuthenticationPrincipal Jwt jwt`**, OpenAPI annotations — **no** stub wording for this route.

5. **Mobile — fetch and display**  
   **Given** user navigates from list (**3.3**) or cold deep link with **`id`** param  
   **When** screen mounts  
   **Then** use TanStack Query **`useQuery`** with stable **`queryKey`** e.g. **`['issue', organizationId, issueId]`** (or **`['issues', 'detail', issueId]`** — document one pattern) calling new **`getIssue(issueId)`** in [`issue-api.ts`](../../apps/mobile/lib/issue-api.ts)  
   **And** show **loading** (ActivityIndicator / skeleton) with accessible label (**UX-DR10**, **NFR-P2**)  
   **And** show **error** with **Problem Details**-aware copy + **Retry** (**UX-DR9**)  
   **And** render **title**, **status** + **priority** using the **same visual language** as the list (**`Chip`**, **`issueStatusTokens`**, priority styling — see **[`IssueRow`](../../apps/mobile/components/IssueRow.tsx)**), **customer** / **site** lines, **assignee** (or “Unassigned”), **description** (empty state if null), **timestamps** (absolute or relative — document; list uses **relative** for triage; detail may use **both** short relative + full date for clarity).

6. **Mobile — layout (UX-DR18)**  
   **Given** content may be **long** (description)  
   **When** user scrolls  
   **Then** **Appbar** (or equivalent) remains **visible** with **issue title** and short **id** context (truncated UUID is fine) — **sticky** behavior: keep **`Appbar.Header`** **outside** **`ScrollView`** (matches current placeholder structure)  
   **And** **Back** returns to list.

7. **Mobile — invalid / not found**  
   **Given** API returns **404** or **malformed `issueId` path segment**  
   **When** detail cannot load  
   **Then** user sees clear **empty/error** state with **Back** to issues list (aligns with epics: guess UUID / foreign issue)  
   **Note:** invalid UUID in the path may yield **400** from Spring before controller logic — handle **non-OK** responses uniformly (**UX-DR9**).

8. **Docs**  
   **Given** [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md) lists issue routes  
   **When** GET by id ships  
   **Then** add a row for **`GET /api/v1/organizations/{organizationId}/issues/{issueId}`** — Allow / Allow — read issue detail.

9. **Tests**  
   **Given** existing IT patterns (**`IssueListIT`**, **`AbstractPostgresIntegrationTest`**)  
   **When** `mvn test` runs  
   **Then** integration tests cover **200** (shape + key fields), **404** unknown id, **403** tenant mismatch for GET by id  
   **And** extend **[`RbacEnforcementIT`](../../apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java)** (or equivalent) so **Technician** and **Admin** **GET** detail match list policy — **no** `FORBIDDEN_ROLE` for this route  
   **And** mobile: **`npm run typecheck`** (and **lint** if CI uses it) passes.

## Tasks / Subtasks

- [x] **API — DTO + OpenAPI** (AC: 1, 2)
  - [x] Add **`IssueDetailResponse`** (or name aligned with list DTO family) under `com.mowercare.model.response`
  - [x] Document **`GET`** in OpenAPI; **camelCase** JSON

- [x] **API — service** (AC: 3)
  - [x] **`IssueService.getIssue(organizationId, issueId)`** (or equivalent) — read-only, assignee fetched in same query

- [x] **API — controller** (AC: 4)
  - [x] **`GET /{organizationId}/issues/{issueId}`** on **`IssueStubController`**

- [x] **API — tests** (AC: 9)
  - [x] New **`IssueDetailIT`** or extend existing issue IT — bootstrap, create issue, GET by id, wrong org **403**, random UUID **404**

- [x] **Docs** (AC: 8)
  - [x] Update **`docs/rbac-matrix.md`**

- [x] **Mobile — API** (AC: 5–7)
  - [x] **`getIssue(issueId)`** in **`issue-api.ts`**; exported **`IssueDetail`** type matching API

- [x] **Mobile — UI** (AC: 5–7)
  - [x] Replace **[id].tsx** placeholder: **`useQuery`**, scrollable content, **Appbar** sticky pattern, chips, error/loading
  - [x] Reuse **`formatRelativeTimeUtc`** from [`relative-time.ts`](../../apps/mobile/lib/relative-time.ts) where appropriate

- [x] **Verification** (AC: 9)
  - [x] `mvn -q test` in `apps/api`
  - [x] `npm run typecheck` in `apps/mobile`

## Dev Notes

### Scope boundaries

- **In scope:** **GET** issue by id, **read-only** detail screen, **UX-DR18** sticky header, **FR14** visibility semantics via **404/403** policy.
- **Out of scope:** **PATCH**/updates (**3.5**), **AssigneePicker** (**3.6**), **history timeline** (**3.7**), **filter/sort** (**3.8**), push deep-link wiring (**4.5** — but **handle** invalid id UX here).

### Previous story intelligence (3.3)

- **[id] placeholder** — [`apps/mobile/app/(app)/issues/[id].tsx`](../../apps/mobile/app/(app)/issues/[id].tsx): **Appbar** + body; **replace** with real detail (**AC 5–6**).
- **List API** — **`IssueListItemResponse`** fields are the **summary** baseline; **detail** adds **`description`** and must stay **consistent** on shared fields (**avoid** list vs detail drift).
- **Client** — [`issue-api.ts`](../../apps/mobile/lib/issue-api.ts): **`authenticatedFetchJson`**, org from **`getSessionOrganizationId()`** — same as **`listIssues`** / **`createIssue`**.
- **Review lessons from 3.3:** Guard **missing org** (disable query + user messaging); **theme**-aware dividers (**no** hardcoded `#` grays); **relative time** invalid ISO guard in **`formatRelativeTimeUtc`**.
- **Controller naming:** **`IssueStubController`** rename still **optional** deferred debt — adding GET by id is fine in place.

### Architecture compliance

| Topic | Source |
|-------|--------|
| REST + JSON + Problem Details | [`architecture.md`](../planning-artifacts/architecture.md) |
| Path **`{organizationId}`** camelCase | [`architecture.md`](../planning-artifacts/architecture.md) |
| Layering: controller → **IssueService** → repository | [`architecture.md`](../planning-artifacts/architecture.md) |
| Mobile: TanStack Query, Paper, Expo Router | [`architecture.md`](../planning-artifacts/architecture.md) |

### Domain reference

| Enum / field | Note |
|--------------|------|
| **`IssueStatus`**, **`IssuePriority`** | Same as list / create |
| **Assignee** | **`assigneeLabel`** from **`User.email`** MVP |

### Existing code to reuse

| Asset | Notes |
|-------|-------|
| [`IssueRepository.findByIdAndOrganization_Id`](../../apps/api/src/main/java/com/mowercare/repository/IssueRepository.java) | **Single-issue** read |
| [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) | Private **`loadIssue`** — factor with new public read |
| [`IssueListItemResponse`](../../apps/api/src/main/java/com/mowercare/model/response/IssueListItemResponse.java) | Align field names with detail DTO |
| [`TenantPathAuthorization`](../../apps/api/src/main/java/com/mowercare/security/TenantPathAuthorization.java) | GET detail |
| Mobile: **`IssueRow`** chip styling (**`Chip`**, **`issueStatusTokens`**), **`ApiProblemError`** | List/create patterns — **no** standalone **`IssueStatusChip`** file today |

### File structure requirements

| Area | Guidance |
|------|----------|
| API responses | `apps/api/src/main/java/com/mowercare/model/response/` |
| API controller | `IssueStubController` (or split if refactoring) |
| Mobile | `apps/mobile/app/(app)/issues/[id].tsx`, `apps/mobile/lib/issue-api.ts` |

### Testing requirements

- **API:** **200** body shape; **404**; **403** tenant; use **bootstrap + login + POST create** pattern from **`IssueListIT`** where faster than raw inserts.
- **Mobile:** **typecheck** required.

### Error semantics

| Condition | HTTP | Notes |
|-----------|------|--------|
| Missing / invalid Bearer | 401 | Spring Security |
| JWT org ≠ path | 403 | `TENANT_ACCESS_DENIED` |
| Issue not in org / unknown id | 404 | No existence leak |

### Git intelligence (recent commits)

- **`b761be6`** — Story **3.3** list GET + Direction A + **`[id]`** placeholder.
- **`ef06b6d`** — Story **3.2** create issue + **`IssueCreatedResponse`** shape.

### Latest tech notes

- Spring Boot **3.x**, springdoc, JPA — match **`IssueListIT`** / **`IssueCreateIT`**.
- Mobile: **Expo Router** `useLocalSearchParams` for **`id`**; validate **UUID** shape if needed before fetch (client-side guard reduces noisy 400s).

### Project context reference

- No `project-context.md` in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md).

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

None.

### Completion Notes List

- **`GET /organizations/{organizationId}/issues/{issueId}`** — **`IssueDetailResponse`**, **`IssueService.getIssue`**, **`toDetailResponse`** built on **`toListItem`** + **`description`**; **`IssueRepository.findByIdAndOrganization_Id`** annotated with **`@EntityGraph(assignee)`** for eager assignee on single-issue reads (including **`loadIssue`**).
- **IT:** **`IssueDetailIT`** — **200** shape, **404**, **403** tenant; **`RbacEnforcementIT`** — Admin + Technician **GET** detail **200**.
- **Mobile:** **`queryKey` `['issue', orgId, issueId]`**, UUID validation, missing-org banner, error + retry, **`Chip`** styling aligned with **`IssueRow`**, sticky **`Appbar`** above **`ScrollView`**.

### File List

- `apps/api/src/main/java/com/mowercare/model/response/IssueDetailResponse.java`
- `apps/api/src/main/java/com/mowercare/repository/IssueRepository.java`
- `apps/api/src/main/java/com/mowercare/service/IssueService.java`
- `apps/api/src/main/java/com/mowercare/controller/IssueStubController.java` (GET by id + tag description)
- `apps/api/src/test/java/com/mowercare/controller/IssueDetailIT.java`
- `apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java`
- `docs/rbac-matrix.md`
- `apps/mobile/lib/issue-api.ts`
- `apps/mobile/app/(app)/issues/[id].tsx`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

## Change Log

- **2026-03-31:** Implemented Story **3.4** — issue detail API + mobile; `mvn test`, mobile `typecheck`/`lint` green; status → **review**; sprint **3-4** → **review**.
- **2026-03-31:** `validate-create-story` — **IssueStatusChip** / **PriorityBadge** corrected to **`IssueRow`**/`Chip`/`issueStatusTokens`; AC7 **400** path note; AC9 **RbacEnforcementIT** line; Validation record appended.
- **2026-03-31:** Story created — `bmad-create-story` auto-discover (first backlog: **3-4-issue-detail-view**).

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-31 against `checklist.md`, `epics.md` Story **3.4**, [`IssueRow.tsx`](../../apps/mobile/components/IssueRow.tsx), [`IssueStubController`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java).

### Critical (addressed in file)

| Issue | Resolution |
|-------|------------|
| **Wrong component names** — story cited **IssueStatusChip** / **PriorityBadge**; repo uses **`Chip`** + **`issueStatusTokens`** in **`IssueRow`** | Cross-story table, AC5, and reuse table updated to match actual code; optional extract-only-if-needed |
| **Epics AC “403/404”** | Story already maps **403** tenant path vs **404** missing issue in org; **400** invalid UUID called out for mobile |

### Enhancements applied

| Addition | Benefit |
|----------|---------|
| **RbacEnforcementIT** expectation in AC9 | Prevents shipping Admin-only GET detail by mistake |
| **400** note on AC7 | Avoids surprise when Spring rejects malformed UUID |

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — **`IssueRow`** patterns, **`issue-api`**, **`IssueService`** |
| Technical accuracy | Pass — **`ResourceNotFoundException`** → **404** via **`ApiExceptionHandler`** |
| File locations | Pass |
| Regression / scope | Pass — **3.5**–**3.8** boundaries preserved |
| Epic alignment | Pass — FR14, UX-DR18 |

### Residual risks (acceptable for ready-for-dev)

- **`IssueStubController`** class name — pre-existing debt; unchanged by this story.
- **Shared chip subcomponents** — extracting **`StatusChip`** from **`IssueRow`** is optional; duplicating chip styling briefly is acceptable if extraction balloons scope.

## Clarifications / questions saved for end

_None._
