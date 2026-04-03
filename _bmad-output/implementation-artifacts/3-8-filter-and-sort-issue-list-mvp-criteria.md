# Story 3.8: Filter and sort issue list (MVP criteria)

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As a **Technician or Admin**,
I want **to filter and sort the issue list by MVP criteria such as status, priority, and recency**,
so that **I can find the right work quickly (FR19)**.

**Implements:** **FR19**; aligns with **UX-DR2**, **UX-DR8**, **UX-DR10** (list triage, empty states, honest loading).

**Epic:** Epic 3 — Issues — capture, triage, ownership & history.

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 3.8])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** list endpoint **When** query params are used per OpenAPI (`status`, `sort`, etc.) **Then** results match contract | **API:** extend **`GET .../issues`** with documented optional filters + sort; keep **`IssueListResponse`** shape (**`items`** only — no breaking change). |
| **Given** mobile **When** user applies filters **Then** UI exposes **only** supported options (no dead controls) | **Mobile:** filter + sort controls wired to the same query params; **EmptyState** for “filters hid everything” vs true org empty (**UX-DR8**). |
| **Given** RBAC **When** filters imply “all org issues” **Then** behavior matches matrix | **MVP:** retain **parity** with current list: **Admin** and **Technician** both use the **same** org-scoped list rules as today ([`docs/rbac-matrix.md`](../../docs/rbac-matrix.md) — `scope=all` already org-wide for both). This story **does not** introduce Technician-only “assigned to me” visibility for `all` unless product explicitly changes matrix; if unchanged, **document** that in RBAC row. |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **3.3** | **Direction A** — [`index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx) has **Open / All / Mine** + **`listIssues(scope)`**; **extend** this screen (and **`issue-api.ts`**) with new query params + query keys. |
| **3.7** | **Independent** — change-events do not affect list query. After **PATCH** issue, existing **`['issues', ...]`** invalidation patterns remain. |

## Acceptance Criteria

1. **API — query contract (OpenAPI + validation)**  
   **Given** **`GET /api/v1/organizations/{organizationId}/issues`**  
   **When** optional filters/sort are supplied  
   **Then** behavior remains **backward compatible**: omitting new params preserves current semantics (**`scope`** default **`open`**; sort **updatedAt desc**, **id desc** tie-break; max **200** rows — [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) **`LIST_MAX`**) **And** document every new param in **`IssueStubController`** + Springdoc with **`allowableValues`** where enums apply **And** invalid combinations return **`400`** **`VALIDATION_ERROR`** with stable **`code`** (same pattern as invalid **`scope`** — see [`IssueListScope.parse`](../../apps/api/src/main/java/com/mowercare/model/IssueListScope.java)).

2. **API — filter by status (optional)**  
   **Given** enum **`IssueStatus`** — [`IssueStatus.java`](../../apps/api/src/main/java/com/mowercare/model/IssueStatus.java)  
   **When** client sends **`status`** — **locked:** **repeated** query params (Spring MVC: `@RequestParam(required = false) List<String> status`) e.g. `status=OPEN&status=IN_PROGRESS` — **omit** the param entirely when not filtering (do not send empty values; treat spurious blanks as **omit** or **`400`** — pick one and test)  
   **Then** parse each token to **`IssueStatus`** (values match **JSON/OpenAPI** enum strings: **`OPEN`**, **`IN_PROGRESS`**, … — **not** the lowercase style used for **`scope`**) **invalid token → `400` `VALIDATION_ERROR`**  
   **And** results include **only** issues whose **`status`** is in the provided set **And** filtering applies **after** **`scope`** semantics (intersection): e.g. **`scope=open`** restricts to non-terminal statuses first, then **`status`** narrows further **And** if the intersection is empty, return **`200`** with **`items: []`** (not an error).

3. **API — filter by priority (optional)**  
   **Given** enum **`IssuePriority`**  
   **When** client sends **`priority`** — **same locked style as status** (repeated params, enum names **`LOW`** … **`URGENT`**)  
   **Then** results include only matching priorities **And** intersects with **`scope`** + **`status`** filters.

4. **API — sort (recency + priority)**  
   **When** client sends **`sort`** (suggested **`allowableValues`**: **`updatedAt`** default, **`createdAt`**, **`priority`**) **and** optional **`direction`** (**`asc`** | **`desc`**, default **`desc`** for time fields; for **`priority`**, default **`desc`** so **URGENT** ranks above **LOW**)  
   **Then** the list is ordered accordingly **with a stable tie-break** (**`id` desc** recommended for deterministic ordering) **And** sorts map to indexed columns where possible (**`idx_issues_organization_id_status`** exists — [`0008-issues-and-history.yaml`](../../apps/api/src/main/resources/db/changelog/changes/0008-issues-and-history.yaml)); add a **Liquibase** index for **`priority`** if explain shows seq scan on org-wide lists.  
   **Critical — priority sort:** [`IssuePriority`](../../apps/api/src/main/java/com/mowercare/model/IssuePriority.java) is persisted as **STRING**. A naive **`ORDER BY priority`** in SQL orders **lexicographically** (e.g. **HIGH** before **LOW**) — **wrong**. Implement severity order explicitly: **`CASE priority WHEN 'LOW' THEN 0 … WHEN 'URGENT' THEN 3 END`** in JPQL/Criteria, or a small **generated column / view** — **do not** rely on default string collation for business priority.

5. **API — implementation approach**  
   **Prefer** extending [`IssueRepository`](../../apps/api/src/main/java/com/mowercare/repository/IssueRepository.java) with **`JpaSpecificationExecutor<Issue>`** (or equivalent) so **`scope`**, **`status`**, **`priority`**, and **`sort`** compose in **one** query path **without** N+1.  
   **Critical — `assignee` fetch:** `@EntityGraph` on repository methods **does not** automatically apply to **`findAll(Specification, Pageable)`**. Use a **`join fetch`** for **`assignee`** in the **Specification** (distinct) **or** an `@EntityGraph` entity graph API compatible with your Spring Data version — verify **`toListItem`** does not trigger **N+1** (same pattern as review for **3.7** change-events). **Alternative:** curated `@Query` methods if specifications are overkill — but avoid duplicating five parallel code paths.

6. **Mobile — query wiring**  
   **Given** [`listIssues`](../../apps/mobile/lib/issue-api.ts) and [`index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx)  
   **When** filters/sort change  
   **Then** **`listIssues`** accepts a **typed** params object (e.g. `scope`, `statuses`, `priorities`, `sort`, `direction`) building **`URLSearchParams`** **And** TanStack Query **`queryKey`** includes **all** dimensions, e.g. **`['issues', orgId, scope, filterKey, sortKey]`** so cache segments correctly **And** pull-to-refresh refetches the active query.

7. **Mobile — UX**  
   **Given** **UX-DR8** (empty vs filtered-empty)  
   **When** any combination of **scope + status/priority/sort** yields **no rows**  
   **Then** if the org has **at least one** issue under **unfiltered** **`scope=all`** (same approach as current **probe** in [`index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx), extended to account for **sort** not affecting existence), show **“Nothing matches filters”** + **Reset filters** (clears status/priority/sort to defaults; **optional:** switch **scope** to **All** if product prefers) — **never** show **“No issues yet”** in that case.  
   **And** keep **loading** and **error + Retry** banner behavior honest (**UX-DR10** — list is **read**; do **not** use **MutationFeedback** here; use **Banner** / **ActivityIndicator** as today).

8. **Tests**  
   **Given** [`IssueListIT`](../../apps/api/src/test/java/com/mowercare/controller/IssueListIT.java)  
   **When** extended  
   **Then** cover: filter-only rows, sort order (at least **createdAt** vs **updatedAt**), invalid param **`400`**, tenant **`403`** unchanged **And** **`RbacEnforcementIT`** list tests still pass (Technician + Admin **200** on list).

## Tasks / Subtasks

- [x] **API — parsing + service** (AC: 1–5)
  - [x] Add small param parser(s) or `@RequestParam` bindings for status/priority/sort/direction; centralize validation (invalid enum → **400**).
  - [x] Refactor **`IssueService.listIssues`** to accept filter/sort DTO; implement specification-based query with org scope + **`scope`** + optional filters.
  - [x] Update **`IssueStubController`** OpenAPI text (replace “only scope” wording in tag description if needed).

- [x] **API — DB index** (AC: 4)
  - [x] Add **`idx_issues_organization_id_priority`** (or composite covering sort/filter) if needed after checking generated SQL / tests.

- [x] **Docs** (AC: 1, epic RBAC)
  - [x] Update [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md) row for **`GET .../issues`** with new params; state explicitly whether **Technician** vs **Admin** visibility differs (**MVP:** likely **no** change from current note).

- [x] **Mobile** (AC: 6–7)
  - [x] Extend **`issue-api.ts`** + **`index.tsx`** with filter/sort UI (Menu, Chips, SegmentedButtons, or **IconButton** + sheet — match Paper/MD3 patterns already in app).
  - [x] Adjust empty-state logic for filtered-empty vs org-wide empty.

- [x] **Verification** (AC: 8)
  - [x] **`mvn test`** (`IssueListIT`, `RbacEnforcementIT`); mobile **`npm run typecheck`** + **`npm run lint`**.

### Review Findings

- [x] [Review][Patch] OpenAPI `400` response on `GET .../issues` still reads like invalid `scope` only — update `@ApiResponse` text to include invalid `status` / `priority` / `sort` / `direction` tokens (`IssueStubController` ~line 76).
- [x] [Review][Patch] `@Operation` / `@Schema` examples for list query — avoid mixing status and priority enum examples (`IssueStubController` list `description` + `status` param).
- [x] [Review][Patch] Add `IssueListIT` coverage for an invalid `priority` query token (parity with existing invalid `status` test).
- [x] [Review][Patch] Add `IssueListIT` coverage for an invalid `direction` query token.

## Dev Notes

### Current behavior (do not regress)

| Area | Today |
|------|--------|
| **Scope** | **`open`** = `OPEN`, `IN_PROGRESS`, `WAITING`; **`all`** = all statuses; **`mine`** = assignee = caller ([`IssueService.listIssues`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java)) |
| **Sort** | **`updatedAt` DESC**, **`id` DESC** via **`LIST_PAGE`** |
| **Cap** | **200** rows |
| **Mobile** | Segmented **Open / All / Mine**; probe query when empty to distinguish filtered vs org-wide ([`index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx)) |

### Architecture compliance

| Topic | Source |
|-------|--------|
| REST + JSON + Problem Details | [`architecture.md`](../planning-artifacts/architecture.md) |
| List query params documented | [`architecture.md`](../planning-artifacts/architecture.md) (example `?status=`) |
| TanStack Query keys | [`architecture.md`](../planning-artifacts/architecture.md) |
| RFC 7807 for errors | Existing **`VALIDATION_ERROR`** pattern |

### Previous story intelligence (3.7)

- **Invalidation:** **`patchIssue`** success already invalidates **`['issues']`** prefix — ensure new query keys still match **`invalidateQueries`** patterns in **[id].tsx** / edit flows (include **full** list key dimensions so filtered lists refresh).
- **No new roles:** issue list remains **tenant + JWT**; **`TenantPathAuthorization`** first (same as [`IssueStubController.listIssues`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java)).
- **Validation errors:** Invalid **`scope`** throws [`InvalidScopeException`](../../apps/api/src/main/java/com/mowercare/exception/InvalidScopeException.java) → **`ApiExceptionHandler`** → **`400`** **`VALIDATION_ERROR`**. New parsers should follow the same Problem Details shape for bad **`status`** / **`priority`** / **`sort`** tokens.

### Testing requirements

- **IT:** Seed 3+ issues with different **status/priority/updatedAt**; assert JSON order and filter counts.
- **Mobile:** Manual: change sort + filters, pull-to-refresh, empty states.

### Latest tech stack

- **Spring Boot 3.x**, **Spring Data JPA** — `JpaSpecificationExecutor`, `Sort`, `Pageable` (still cap at **200** — use **`PageRequest.of(0, LIST_MAX, sort)`**).

### Git intelligence (recent commits)

- **`f4e7997`** — **3.7** change-events API + mobile activity timeline.
- **`d75bdd7`** — **3.6** AssigneePicker.
- **`b761be6`** — **3.3** scoped list + Direction A UI.

### Project context reference

No **`project-context.md`** in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md).

## Dev Agent Record

### Agent Model Used

Cursor agent (Claude)

### Debug Log References

None.

### Completion Notes List

- **API:** `IssueListQueryParser` + `IssueListFilters`; `IssueSpecifications` + `IssueSorts` (priority via `JpaSort.unsafe` CASE); `IssueRepository` extends `JpaSpecificationExecutor` with `@EntityGraph` on `findAll(Spec, Pageable)`; `InvalidIssueListQueryException` + handler; Liquibase **`0009-issues-organization-priority-index`**.
- **Mobile:** `IssueListParams`, `defaultIssueListParams`, `buildIssueListQueryString`; Issues home — sort **Menu**, filter **Dialog** with checkboxes, probe query for empty states; `queryKey` includes all dimensions.
- **Tests:** `IssueListQueryParserTest`, `ApiExceptionHandlerTest` (invalid list query); **`IssueListIT`** extended (Docker/Testcontainers required — not executed in agent env without Docker).
- **Verification run:** `mvn test -Dtest=IssueListQueryParserTest,ApiExceptionHandlerTest,IssueServiceTest`; mobile `npm run typecheck` + `npm run lint` green.

### File List

- `apps/api/src/main/java/com/mowercare/exception/InvalidIssueListQueryException.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/main/java/com/mowercare/issue/IssueListSortField.java`
- `apps/api/src/main/java/com/mowercare/issue/IssueListQueryParser.java`
- `apps/api/src/main/java/com/mowercare/issue/IssueSorts.java`
- `apps/api/src/main/java/com/mowercare/issue/IssueSpecifications.java`
- `apps/api/src/main/java/com/mowercare/repository/IssueRepository.java`
- `apps/api/src/main/java/com/mowercare/service/IssueService.java`
- `apps/api/src/main/java/com/mowercare/controller/IssueStubController.java`
- `apps/api/src/main/resources/db/changelog/changes/0009-issues-organization-priority-index.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/test/java/com/mowercare/issue/IssueListQueryParserTest.java`
- `apps/api/src/test/java/com/mowercare/controller/IssueListIT.java`
- `apps/api/src/test/java/com/mowercare/exception/ApiExceptionHandlerTest.java`
- `apps/mobile/lib/issue-api.ts`
- `apps/mobile/app/(app)/issues/index.tsx`
- `docs/rbac-matrix.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/3-8-filter-and-sort-issue-list-mvp-criteria.md`

## Validation record (create-story / checklist.md)

**Validated:** 2026-04-03 against `checklist.md`, `epics.md` Story **3.8**, [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java), [`Issue.java`](../../apps/api/src/main/java/com/mowercare/model/Issue.java), [`IssueListScope`](../../apps/api/src/main/java/com/mowercare/model/IssueListScope.java), [`index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx).

### Critical (addressed in file)

| Issue | Resolution |
|-------|--------------|
| **Priority `ORDER BY`** on STRING enum can sort lexicographically (wrong severity) | **AC4** — explicit **`CASE`** / documented approach; do not rely on raw column order |
| **`@EntityGraph` + `Specification`** may not combine → **N+1** on **`assignee`** | **AC5** — **`join fetch`** or verified graph on spec query path |
| Ambiguous **query param style** (comma vs repeated) | **AC2–3** — **locked:** repeated params; enum names align with **JSON** |
| **MutationFeedback** on list screen misleads (no mutation) | **AC7** — **Banner** / loading only; reference **UX-DR10** read path |
| Filtered-empty **probe** undefined once filters exist | **AC7** — probe **`scope=all`** without extra filters to detect org-wide issues |

### Enhancements applied

| Addition | Benefit |
|----------|---------|
| **`InvalidScopeException` / handler** pointer | Consistent **`400`** for new enum parsers |
| **Blank query param** called out | Avoids ambiguous “filter to nothing” bugs |

### Residual risks (acceptable for ready-for-dev)

- **`distinct`** may be required if **`join fetch assignee`** duplicates rows in some JPA providers — add **`distinct(true)`** on Criteria if integration tests show duplicates.
- **Probe** adds an extra request when list empty — acceptable MVP; could merge with a future **`HEAD`** or **`count`** endpoint later.

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — extend **`listIssues`**, **`IssueListResponse`**, existing list UI |
| Technical / contract | Pass — explicit sort + param format; OpenAPI must list enum strings |
| Regression | Pass — backward compatible when new params omitted |
| Epic alignment | Pass — FR19, RBAC parity |

## Change Log

- **2026-04-03:** Story created — `bmad-create-story` auto-discover (**3-8-filter-and-sort-issue-list-mvp-criteria**). Ultimate context engine analysis completed — comprehensive developer guide created.
- **2026-04-03:** `validate` — checklist pass; AC tightened (param style, priority sort, EntityGraph/spec, UX wording, validation record).
- **2026-04-04:** Implemented — list filters/sort API + mobile UI + docs; unit tests green; ITs require Docker locally.
