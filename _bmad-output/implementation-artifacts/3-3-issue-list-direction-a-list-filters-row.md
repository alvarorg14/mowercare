# Story 3.3: Issue list — Direction A (list + filters + row)

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. validate-create-story (checklist.md): passed 2026-03-31 — see Validation record at end. -->

## Story

As a **Technician or Admin**,
I want **a scannable issue queue with scope filters and informative rows**,
so that **I can triage quickly (FR13; FR19 partial via scope only; UX-DR2–DR4, DR8, DR10)**.

**Implements:** **FR13**; **UX-DR2**, **UX-DR3**, **UX-DR4**, **UX-DR8**, **UX-DR10**; **NFR-P1** baseline (responsive list load).

**Epic:** Epic 3 — Issues — capture, triage, ownership & history.

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 3.3])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** issues exist **When** user opens Issues home **Then** Direction A: list-first, **Open / All / Mine** scope, FAB **New issue**, rows with title/id, site/customer line, status + priority chips, assignee, relative time | **API:** real **`GET …/issues`** with org scope + **scope** semantics; **Mobile:** **SegmentedButtons** or tabs for scope, **FlatList** + **IssueRow**, **FAB**, TanStack Query **`useQuery`** |
| **Given** empty or filtered-empty **When** list renders **Then** EmptyState: “no issues” vs “no matches” + recovery | **`ListEmptyComponent`** with two variants; “no matches” when scope yields zero but org has issues (client may need total count **or** infer — see Dev Notes) |
| **Given** pull-to-refresh **When** errors occur **Then** error not swallowed | **RefreshControl** optional; **Banner** or inline error + **Retry** from query error state (**UX-DR10**) |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **3.2** | **Replaces** stub **GET** list; reuse **`authenticatedFetchJson`**, session **`organizationId`**, patterns from **`issue-api.ts`** / create flow. |
| **3.4** | **Detail** screen: row tap should navigate to **`/issues/[id]`** — **minimal placeholder** acceptable in **3.3** (read-only stub) so routing exists; **full detail** is **3.4**. |
| **3.8** | **Filter/sort** by status, priority, recency **query params** and advanced RBAC for “all org issues” — **out of scope** for **3.3** except **scope** (Open/All/Mine). Do **not** implement full **FR19** here. |

## Acceptance Criteria

1. **REST — list issues (replace stub GET)**  
   **Given** valid **Bearer** JWT with **`organizationId`** claim matching path  
   **When** `GET /api/v1/organizations/{organizationId}/issues` is called with supported query parameters  
   **Then** response is **200** with JSON body **`items`** (array) of issue **summary** rows suitable for the list UI  
   **And** each item includes at least: **`id`**, **`title`**, **`status`**, **`priority`**, **`customerLabel`**, **`siteLabel`**, **`assigneeUserId`** (nullable), **`assigneeLabel`** (nullable string — e.g. **email** or **local-part** for initials/name until a display-name field exists), **`createdAt`**, **`updatedAt`** (ISO-8601 UTC with **`Z`**)  
   **And** **`TenantPathAuthorization.requireJwtOrganizationMatchesPath`** runs before reads (**same** as [`IssueStubController`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java))  
   **And** **Admin** and **Technician** are both allowed for list (**no** admin-only gate) — [Source: `docs/rbac-matrix.md`](../../docs/rbac-matrix.md)  
   **And** replace **`IssueListStubResponse`** / “stub” OpenAPI text with real **`@Schema`** types; remove misleading “stub” wording for **GET**.

2. **REST — scope query (Open / All / Mine)**  
   **Given** query parameter **`scope`** with values: **`open`** \| **`all`** \| **`mine`** — use **lowercase** strings in the API contract (query params) and a single **`IssueListScope`** (or equivalent) enum on the server for parsing  
   **When** **`scope` is omitted**  
   **Then** default to **`open`** (queue-first triage; matches “Open” as first segment in UX)  
   **When** **`scope=open`**  
   **Then** return issues where **`status`** is **not** **`RESOLVED`** and **not** **`CLOSED`** (**OPEN**, **IN_PROGRESS**, **WAITING**)  
   **When** **`scope=all`**  
   **Then** return **all** issues in the organization **visible to this MVP policy** — **MVP:** both **Admin** and **Technician** see **full org issue set** (Epics “**per RBAC**” here means **both roles** use the **same** three scope options; differential visibility is deferred to **Story 3.8** where noted in [Source: `docs/rbac-matrix.md`](../../docs/rbac-matrix.md))  
   **When** **`scope=mine`**  
   **Then** return issues where **`assignee_user_id`** equals JWT **`sub`** (parsed as UUID) — issues with **`assignee_user_id` null** are **excluded**  
   **And** unknown **`scope`** value (e.g. `scope=foo`) → **400** **`VALIDATION_ERROR`** with stable **`code`** — **omitting** **`scope`** is **not** an error (default **`open`**) (extend [`ApiExceptionHandler`](../../apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java) only if a dedicated exception is introduced).

3. **REST — ordering and bounds**  
   **Given** list query  
   **When** no advanced sort (Story 3.8) exists  
   **Then** default sort is **recency**: **`updatedAt`** descending (then **`id`** for stability)  
   **And** document **pagination** or a **reasonable max** (e.g. cap **200** rows with OpenAPI note) so responses stay bounded — [Source: `architecture.md` — list patterns](../planning-artifacts/architecture.md).

4. **Service layer**  
   **Given** [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) is the domain entry  
   **When** listing  
   **Then** **`IssueService`** (or a dedicated query method) performs org-scoped reads; **controller** stays thin — **no** duplicated tenant checks beyond **`TenantPathAuthorization`** + service org id.

5. **Persistence**  
   **Given** [`IssueRepository`](../../apps/api/src/main/java/com/mowercare/repository/IssueRepository.java)  
   **When** implementing filters  
   **Then** add **`@Query`** JPQL/Criteria or Spring Data method names that **filter by** `organization_id`, optional **status** sets, optional **assignee** — use **`JOIN FETCH`** (or entity graph) for **`assignee`** when mapping **`assigneeLabel`** to avoid **N+1** on list — consider **index** on **`(organization_id, updated_at desc)`** if not present (Liquibase changeset if justified).

6. **Mobile — Direction A shell**  
   **Given** authenticated employee on **Issues** home  
   **When** screen loads  
   **Then** **FAB** “**New issue**” remains primary create (**UX-DR2**) — [`issues/index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx)  
   **And** **SegmentedButtons** (or **Material** tabs) for **Open / All / Mine** — labels match UX (**UX-DR2** / [Source: `ux-design-specification.md`](../planning-artifacts/ux-design-specification.md))  
   **And** **`getSessionRole()`** from [`session.ts`](../../apps/mobile/lib/auth/session.ts) available if UI needs role-specific copy (optional for MVP if both roles share **All**).

7. **Mobile — list + row**  
   **Given** successful fetch  
   **When** user views the list  
   **Then** use **`FlatList`** (or Paper **`List`** sections) with a custom **IssueRow**: **title** (and optional short **id** snippet), **site/customer** line from labels, **IssueStatusChip** + **PriorityBadge** (**UX-DR3**, **UX-DR4** — **never** color-only; pair with label/icon)  
   **And** **assignee** shows **`assigneeLabel`** or “**Unassigned**”  
   **And** **relative time** from **`updatedAt`** or **`createdAt`** (document choice; prefer **`updatedAt`** for triage) via **`Intl.RelativeTimeFormat`** or lightweight helper.

8. **Mobile — loading / error / empty**  
   **Given** TanStack Query **`useQuery`** for list  
   **When** loading  
   **Then** show **ActivityIndicator** or **skeleton** with accessible label (**UX-DR10**, **NFR-P2**)  
   **When** error  
   **Then** **Banner** or inline message + **Retry**; map **`ApiProblemError`** **`code`** where useful (**UX-DR9**)  
   **When** **empty** org (no issues at all)  
   **Then** **EmptyState** variant: short explanation + **primary** path to **Create**  
   **When** **filters** yield zero (e.g. **Open** empty but org has closed issues)  
   **Then** second **EmptyState**: “Nothing in this queue” + **switch scope** / **View all** action (**UX-DR8**) — **MVP implementation (pick one and document in completion notes):** (a) **lightweight second query** — e.g. `scope=all` with **`limit=1`** (or a tiny **count** endpoint only if already justified) to detect “org has issues outside this scope”; or (b) **single** empty copy that still shows **segmented scope** controls so the user can switch without inferring “no matches” vs “no issues” (slightly weaker copy but one network call).

9. **Mobile — pull-to-refresh (optional)**  
   **Given** **RefreshControl**  
   **When** user pulls  
   **Then** **`refetch()`** from TanStack Query; **errors** remain visible (**UX-DR10**).

10. **Navigation — row → detail placeholder**  
    **Given** row press  
    **When** user taps  
    **Then** navigate to **`/issues/[id]`** with **placeholder** content acceptable (**Story 3.4** replaces with real detail) — ensures **UX-DR3** “tap → detail” **routing** exists.

11. **Tests**  
    **Given** existing API IT patterns  
    **When** `mvn test` runs  
    **Then** integration tests cover **200** list with **seeded** issues for **`open`**, **`all`**, **`mine`** (or unit + IT mix) + **403** tenant mismatch  
    **And** mobile: **`npm run typecheck`** (and **`lint`** if CI uses it) passes.

## Tasks / Subtasks

- [x] **API — DTOs + OpenAPI** (AC: 1–3)
  - [x] Replace stub list response with **`IssueListResponse`** + **`IssueListItemResponse`** (names may vary) under `com.mowercare.model.response`
  - [x] Document **`scope`** enum + **default `open`** when omitted; document sort + cap/pagination

- [x] **API — repository + service** (AC: 4–5)
  - [x] Extend **`IssueRepository`** with scoped queries; join **`User`** for assignee **email** (or select assignee id only and map label in service)
  - [x] Add **`IssueService.listIssues(…)`** (signature TBD) returning DTOs or domain projection

- [x] **API — controller** (AC: 1–2)
  - [x] Replace **`listIssues`** body: call service; **`IssueListScope.parse(scope)`** (omitted/`null` → **`open`**) for **`scope`**; return **200**
  - [x] Update controller **`@Tag`** / **description** — **GET** is **live** (class-level tag may still mention **`_admin/reassign`** stub until later stories)

- [x] **API — tests** (AC: 11)
  - [x] New/extended IT class — follow **`IssueCreateIT`** / **`TenantScopeIT`** patterns (JWT, org id, seed issues)

- [x] **Docs** (AC: 2)
  - [x] Update [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md): **GET list** is **real**; document **`scope`** semantics and MVP “**all** = full org for both roles” unless superseded by **3.8**

- [x] **Mobile — API client** (AC: 6–9)
  - [x] Extend [`issue-api.ts`](../../apps/mobile/lib/issue-api.ts) (or `issue-list.ts`) with **`listIssues(scope)`** returning typed rows

- [x] **Mobile — UI** (AC: 6–10)
  - [x] Refactor **`issues/index.tsx`**: **useQuery**, scope state, **FAB**, **FlatList**, **IssueRow** component (colocate under `components/` or `features/issue/` per team preference)
  - [x] Add **`app/(app)/issues/[id].tsx`** placeholder for **3.4**
  - [x] Theme: **IssueStatusChip** / **PriorityBadge** using **`useTheme()`** semantic colors ([Source: UX component strategy](../planning-artifacts/ux-design-specification.md))

- [x] **Verification**
  - [x] `mvn -q test` in `apps/api`
  - [x] `npm run typecheck` in `apps/mobile`

### Review Findings

- [x] [Review][Patch] Issues home shows no loading or error when `getSessionOrganizationId()` is missing — queries stay disabled; user sees only header and FAB with an empty body [`apps/mobile/app/(app)/issues/index.tsx`] — fixed: banner + Sign in, gated list/empty/error, disabled FAB and scope buttons
- [x] [Review][Patch] `RbacEnforcementIT` display name still says “GET issues stub” for tenant-denied list test — misleading after list went live [`apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java`:176] — fixed
- [x] [Review][Patch] `formatRelativeTimeUtc` does not guard invalid ISO strings — `Date` can yield NaN and `Intl.RelativeTimeFormat` may show odd output [`apps/mobile/lib/relative-time.ts`] — fixed
- [x] [Review][Patch] `IssueRow` list separator uses hardcoded `#00000022` — may not track theme / dark surfaces [`apps/mobile/components/IssueRow.tsx`:91] — fixed: `theme.colors.outlineVariant`

- [x] [Review][Defer] `IssueStubController` class name vs live GET list — optional rename deferred with Story 3.2/3.3 notes [`IssueStubController.java`] — deferred, pre-existing
- [x] [Review][Defer] Empty-state probe uses full `scope=all` fetch (up to 200 rows) to detect org-wide issues; story suggested `limit=1` as an example — acceptable MVP; narrow later if needed [`apps/mobile/app/(app)/issues/index.tsx`] — deferred, pre-existing
- [x] [Review][Defer] Composite DB index on `(organization_id, updated_at)` for list sort — story AC5 “consider”; existing indexes cover org and org+status [`0008-issues-and-history.yaml`] — deferred, pre-existing

## Dev Notes

### Scope boundaries

- **In scope:** Real **GET** list + **scope** (**Open/All/Mine**), Direction A **UI**, **IssueRow**, **EmptyState** variants, **TanStack Query** list loading, **placeholder** detail route.
- **Out of scope:** Full **FR19** filter/sort API (**3.8**), **AssigneePicker** (**3.6**), **rich** issue **detail** (**3.4**), **notifications** (Epic 4).

### Previous story intelligence (3.2)

- **`GET`** was **stub** returning **`IssueListStubResponse`** empty — **3.3** replaces entirely.
- **Mobile** [`issues/index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx) is **placeholder copy** + **FAB** — replace with real list.
- **Patterns:** [`authenticatedFetchJson`](../../apps/mobile/lib/api.ts), [`ApiProblemError`](../../apps/mobile/lib/http.ts), [`IssueCreatedResponse`](../../apps/api/src/main/java/com/mowercare/model/response/IssueCreatedResponse.java) field naming — **align list item** camelCase JSON with **OpenAPI**.
- **Controller name debt:** **`IssueStubController`** may remain until rename/split — **GET** must not read as “stub” in OpenAPI.

### Architecture compliance

| Topic | Source |
|-------|--------|
| REST + JSON + Problem Details | [`architecture.md`](../planning-artifacts/architecture.md) |
| Path **`{organizationId}`** camelCase | [`architecture.md`](../planning-artifacts/architecture.md) |
| Layering: controller → **IssueService** → repository | [`architecture.md`](../planning-artifacts/architecture.md) |
| Mobile: TanStack Query, Paper, Expo Router | [`architecture.md`](../planning-artifacts/architecture.md) |
| RBAC matrix detail | [`architecture.md`](../planning-artifacts/architecture.md) — fine-grained matrix **TBD**; **3.3** uses **MVP full-org visibility** for **All** |

### Domain reference

| Enum / field | Note |
|--------------|------|
| **`IssueStatus`** | `OPEN`, `IN_PROGRESS`, `WAITING`, `RESOLVED`, `CLOSED` — **Open** scope = exclude **RESOLVED**, **CLOSED** |
| **Assignee** | **`User.email`** for **`assigneeLabel`** MVP |

### Existing code to reuse

| Asset | Notes |
|-------|-------|
| [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) | Add **read** path; keep **writes** unchanged |
| [`Issue`](../../apps/api/src/main/java/com/mowercare/model/Issue.java) | Fields for row + joins |
| [`TenantPathAuthorization`](../../apps/api/src/main/java/com/mowercare/security/TenantPathAuthorization.java) | List **GET** |
| Mobile session | [`getSessionOrganizationId`](../../apps/mobile/lib/auth/session.ts), [`getSessionRole`](../../apps/mobile/lib/auth/session.ts) |

### File structure requirements

| Area | Guidance |
|------|----------|
| API responses | `apps/api/src/main/java/com/mowercare/model/response/` |
| API controller | `IssueStubController` or rename when touching |
| Mobile | `apps/mobile/app/(app)/issues/`, `apps/mobile/lib/issue-api.ts` (+ components as needed) |

### Testing requirements

- **API:** **200** + JSON shape; **403** wrong org; **scope** behavior **deterministic** with seeded DB rows in IT.
- **Mobile:** **typecheck** required; snapshot/E2E optional.

### Error semantics

| Condition | HTTP | `code` (typical) |
|-----------|------|------------------|
| Missing / invalid Bearer (unauthenticated) | 401 | (Spring Security / RFC 7807 per existing API behavior) |
| JWT org ≠ path | 403 | `TENANT_ACCESS_DENIED` |
| Unknown **`scope`** token | 400 | `VALIDATION_ERROR` |

### Git intelligence (recent commits)

- **`ef06b6d`** — Story **3.2** create issue API + mobile; **`IssueStubController`** POST live, **GET** stub.
- **`dce71b8`** — Story **3.1** **`Issue`** aggregate + history.

### Latest tech notes

- Spring Boot **3.x**, springdoc, JPA — match existing testcontainers IT style.
- Mobile: **Expo 55**, **TanStack Query 5**, **Paper** — match **`package.json`**.

### Project context reference

- No `project-context.md` in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md).

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

None.

### Completion Notes List

- **GET `/issues`:** `IssueListScope.parse` + **`IssueService.listIssues`**; **`@EntityGraph`** on **`IssueRepository`** pages (max **200**, **`updatedAt`/`id`** sort); **`IssueListStubResponse`** removed; **`InvalidScopeException`** → **400** **`VALIDATION_ERROR`**.
- **IT:** **`IssueListIT`** covers **`open`** / **`all`** / **`mine`**, default scope, bad scope, tenant **403**, response shape.
- **Mobile:** **`SegmentedButtons`** (**Open/All/Mine**), **`listIssues`**, probe query for empty-state copy, **`IssueRow`** + **`relative-time`**, **`[id]`** placeholder, **`create`** invalidates **`['issues']`**.
- **Empty-state MVP:** option **(a)** — secondary **`scope=all`** query when primary list empty and scope ≠ **`all`**.

### File List

- `apps/api/src/main/java/com/mowercare/model/IssueListScope.java`
- `apps/api/src/main/java/com/mowercare/exception/InvalidScopeException.java`
- `apps/api/src/main/java/com/mowercare/model/response/IssueListItemResponse.java`
- `apps/api/src/main/java/com/mowercare/model/response/IssueListResponse.java`
- `apps/api/src/main/java/com/mowercare/repository/IssueRepository.java`
- `apps/api/src/main/java/com/mowercare/service/IssueService.java`
- `apps/api/src/main/java/com/mowercare/controller/IssueStubController.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/test/java/com/mowercare/controller/IssueListIT.java`
- `apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java`
- `apps/mobile/lib/issue-api.ts`
- `apps/mobile/lib/relative-time.ts`
- `apps/mobile/components/IssueRow.tsx`
- `apps/mobile/app/(app)/issues/index.tsx`
- `apps/mobile/app/(app)/issues/[id].tsx`
- `apps/mobile/app/(app)/issues/create.tsx`
- `docs/rbac-matrix.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

## Change Log

- **2026-03-31:** Code review (option 1 — apply patches): missing-org banner + gated UI, `RbacEnforcementIT` display name, `formatRelativeTimeUtc` NaN guard, `IssueRow` theme divider; status → **done**; sprint **3-3** → **done**.
- **2026-03-31:** Implemented Story **3.3** — issue list API + Direction A mobile; `mvn test` and mobile `typecheck`/`lint` green; status → **review**.
- **2026-03-31:** `validate-create-story` — default **`scope`**, **`mine`** + null assignee, empty-state **MVP options**, **N+1** note, **401** row, epics **per RBAC** clarification; Validation record appended.
- **2026-03-31:** Story created — `bmad-create-story` auto-discover (first backlog: **3-3**).

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-31 against `checklist.md`, `epics.md` Story **3.3**, `IssueStubController`, `IssueRepository`, Story **3.2** file patterns.

### Critical (addressed in file)

| Issue | Resolution |
|-------|------------|
| **`scope`** default ambiguous (“required or defaulted”) | **Default `open`** when omitted; **unknown** value → **400**; omission → **not** an error. |
| **“Per RBAC”** (epics) vs **MVP full-org `all`** | Clarified: **same** three scopes for **Admin** and **Technician** in **3.3**; **3.8** refines **FR19** / visibility if needed. |
| **`mine`** and unassigned issues | **Explicit:** null **assignee** rows **excluded** from **`mine`**. |
| **EmptyState** “no issues” vs “no matches” too vague | **Two** concrete MVP options (second lightweight query vs single copy + scope UI). |
| **List + assignee label** risk | **`JOIN FETCH`** / entity graph called out to avoid **N+1**. |

### Enhancements applied

| Addition | Benefit |
|----------|---------|
| **`@RequestParam` default** in tasks | Matches AC and prevents divergent clients. |
| **401** in error table | Aligns with OpenAPI on existing **GET** stub. |

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — extends **stub GET**, **`IssueService`**, **`issue-api`** patterns |
| Technical accuracy | Pass — scope default, **mine** semantics, fetch strategy |
| File locations | Pass |
| Regression / scope | Pass — **3.8** / **3.4** boundaries preserved |
| Epic alignment | Pass — Direction A, EmptyState, pull-to-refresh + errors |

### Residual risks (acceptable for ready-for-dev)

- **Controller class name `IssueStubController`:** naming debt from **3.2**; optional rename when touching structure — does not block **GET** behavior.
- **Technician vs Admin “All” long-term:** product may tighten in **3.8**; **rbac-matrix** task documents MVP assumption.

## Clarifications / questions saved for end

_None._
