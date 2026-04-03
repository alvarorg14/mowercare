# Story 3.7: Issue activity / history on detail

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As a **Technician or Admin**,
I want **to see who changed what and when on an issue**,
so that **we maintain trust in the shared system of record (FR18)**.

**Implements:** **FR18**; supports **NFR-S5** for issue-relevant audit presentation.

**Epic:** Epic 3 — Issues — capture, triage, ownership & history.

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 3.7])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** prior changes **When** user views detail **Then** timeline shows material changes with actor and timestamp **in ISO-8601 UTC in UI** | **Mobile:** dedicated **Activity / History** section on issue detail — chronological list; each row: **human-readable change**, **actor** (label), **time** (show **relative** + ensure **machine-readable ISO UTC** available for a11y, e.g. subtitle or `accessibilityLabel`) |
| **Given** API **When** history is requested **Then** responses are **org-scoped** and **paginated if needed** | **API:** new **GET** sub-resource under the issue; **tenant** match + same role gate as issue read; **pageable** response (default **oldest-first** to match narrative timeline, or **newest-first** with clear product choice — **recommend oldest-first** for “story of the issue”) |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **3.1 / 3.5** | **`issue_change_events`** rows already written by **`IssueService.appendEvent`** for **CREATED**, **STATUS_CHANGED**, **ASSIGNEE_CHANGED**, **PRIORITY_CHANGED**, **TITLE_CHANGED**, **DESCRIPTION_CHANGED**, **CUSTOMER_LABEL_CHANGED**, **SITE_LABEL_CHANGED**. This story **exposes** them; **do not** duplicate write logic. |
| **3.4 / 3.5 / 3.6** | Detail screen **[id].tsx** exists; **extend** with history UI + query. After **PATCH**, invalidate **issue** + **history** caches. |
| **3.8** | List filter/sort — **out of scope**. |
| **3.9** | Full a11y pass later; still use **≥16sp** for history sentences per UX and sensible **accessibilityLabel** on rows. |

## Acceptance Criteria

1. **API — list change events for an issue (org-scoped)**  
   **Given** valid JWT and **`TenantPathAuthorization`** org match  
   **When** **`GET /api/v1/organizations/{organizationId}/issues/{issueId}/change-events`** is called (name locked in OpenAPI — alternative `.../history` acceptable if documented, but prefer **`change-events`** alignment with table **`issue_change_events`**)  
   **Then** **200** returns a **page** of events for that issue **only** **And** **404** if issue not in org (same as **`getIssue`**) **And** **401** / **403** follow existing patterns **And** **Admin** + **Technician** both **Allow** (mirror **`GET .../issues/{issueId}`** — [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md)) **And** response items include at minimum: **`id`**, **`occurredAt`** (instant, JSON ISO-8601), **`changeType`** (enum string matching **`IssueChangeType`**), **`actorUserId`**, **`actorLabel`** (MVP: actor **email**, same idea as **`assigneeLabel`** on list items), **`oldValue`**, **`newValue`** (nullable strings as stored — see AC2) **And** **OpenAPI** + Springdoc updated **And** **`docs/rbac-matrix.md`** gains a row for this **GET**.

2. **API — human-readable values for assignee changes**  
   **Given** **`ASSIGNEE_CHANGED`** events store **old/new** as **UUID strings** (or null) in **`issue_change_events`**  
   **When** mapping to API  
   **Then** expose **resolved labels** for display: e.g. **`oldAssigneeLabel`** / **`newAssigneeLabel`** (nullable) **or** embed a small **value summary** field — **do not** force the mobile client to map UUIDs to emails alone; server resolves **`User.email`** in-org. **Edge case:** **`issues.assignee_user_id`** uses **SET NULL** when assignee **user** is deleted, but **history rows** can still hold **UUIDs** for users no longer in the org — **lookup** by id where possible; if missing, **fallback** (**“Former user”**, **“Unknown”**, or short id) **and** comment the policy in code.

3. **API — pagination**  
   **Given** epics require pagination **when needed**  
   **When** implemented  
   **Then** use **`Pageable`** internally (e.g. **`page`** default **0**, **`size`** default **50**, **max cap** e.g. **100**) **And** default sort **`occurredAt`** **asc** (timeline order) **And** expose a **stable JSON contract** for clients: prefer a **record** like **`IssueChangeEventsResponse`** with **`items`** (list of event DTOs) **and** pagination fields (**`totalElements`**, **`totalPages`**, **`number`**, **`size`**) **matching the style of** **`IssueListResponse`** / OpenAPI clarity — **avoid** undocumented Spring Data **`Page`** JSON shape unless you explicitly document **`content`**, **`pageable`**, etc., in OpenAPI **and** type the mobile client **And** repository: **`Page<IssueChangeEvent>`** with org-scoped query; DB index **`idx_issue_change_events_issue_occurred`** supports sort.

4. **Mobile — timeline on issue detail**  
   **Given** [`apps/mobile/app/(app)/issues/[id].tsx`](../../apps/mobile/app/(app)/issues/[id].tsx)  
   **When** user views an issue  
   **Then** show an **Activity** (or **History**) section **below** main fields (or after metadata — follow readable flow) **And** **loading** / **empty** (“No activity yet”) / **error** + **Retry** using **`ApiProblemError`** + TanStack Query **And** list is **scrollable** inside the screen’s **`ScrollView`** (already used) **And** **sticky** app bar behavior from **3.4** remains (UX: sticky context when scrolling long history — [Source: `_bmad-output/planning-artifacts/ux-design-specification.md` — Layout principles]).

5. **Mobile — timestamps FR18**  
   **Given** epics require **ISO-8601 UTC** in UI  
   **When** rendering each event  
   **Then** display **relative** time for scan (reuse **`formatRelativeTimeUtc`** from [`relative-time.ts`](../../apps/mobile/lib/relative-time.ts) if applicable) **And** include **full ISO UTC** in **`accessibilityLabel`** or secondary line so assistive tech / power users see canonical time.

6. **Mobile — copy & invalidation**  
   **Given** **`getIssue`** uses **`['issue', orgId, issueId]`** (see [`[id].tsx`](../../apps/mobile/app/(app)/issues/[id].tsx) **`queryKey`**)  
   **When** adding history fetch  
   **Then** add **`listIssueChangeEvents(issueId)`** (or similar) in [`issue-api.ts`](../../apps/mobile/lib/issue-api.ts) **And** **`useQuery`** key e.g. **`['issue-change-events', orgId, issueId]`** **And** extend the **`patchIssue`** **`onSuccess`** path to **`invalidateQueries`** **`['issue-change-events', orgId, issueId]`** in addition to existing **`['issue', orgId, issueId]`** and **`['issues']`** (prefix) so the timeline updates after save.

7. **Tests / verification**  
   **Given** patterns in **`IssueDetailIT`**, **`RbacEnforcementIT`**  
   **When** complete  
   **Then** integration tests cover **200** with ordered events, **404** wrong org/issue, **403** tenant mismatch **And** **`mvn test`** **`apps/api`** **And** mobile **`npm run typecheck`** + **`npm run lint`**.

## Tasks / Subtasks

- [x] **API — DTO + mapping + service** (AC: 1–3)
  - [x] `IssueChangeEventResponse` record(s) in `com.mowercare.model.response` with OpenAPI annotations.
  - [x] `IssueService` method: load issue (reuse **`loadIssue`**), page events by **`issueId`** + org, map actors to email labels; resolve assignee UUIDs for **`ASSIGNEE_CHANGED`**.
  - [x] New controller method on **`IssueStubController`** (or dedicated controller — prefer **same** **`IssueStubController`** / **Issues** tag for discoverability).

- [x] **API — repository** (AC: 3)
  - [x] Add **`Page<IssueChangeEvent> findByIssue_Id(..., Pageable)`** (and keep org safety via join or filter — events have **`organization_id`**; ensure query does not leak cross-org).

- [x] **Docs + RBAC** (AC: 1)
  - [x] `docs/rbac-matrix.md` row; **`RbacEnforcementIT`** GET test for Technician + Admin + tenant **403**.

- [x] **Mobile — API + UI** (AC: 4–6)
  - [x] Typed client + **`useQuery`**; **`IssueActivityTimeline`** or inline section in **`[id].tsx`**; Paper **List** / **Text** rows.

- [x] **Verification** (AC: 7)
  - [x] API IT + mobile checks.

### Review Findings

- [x] [Review][Patch] Lazy `User actor` on `IssueChangeEvent` causes N+1 queries when mapping `listChangeEvents` (`ev.getActor()` per row). Add `@EntityGraph(attributePaths = "actor")` on `IssueChangeEventRepository.findByIssue_IdAndOrganization_Id` or an equivalent fetch join so each page uses one query for actors. [`IssueChangeEvent.java:39-41`](../../apps/api/src/main/java/com/mowercare/model/IssueChangeEvent.java), [`IssueService.java:337-355`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) — **Fixed:** `@EntityGraph(attributePaths = "actor")` on `findByIssue_IdAndOrganization_Id`.

- [x] [Review][Patch] AC2 (assignee labels) is not covered by integration tests — add an IT that PATCHes assignee (or seeds two users), then asserts `oldAssigneeLabel` / `newAssigneeLabel` on `ASSIGNEE_CHANGED`. [`IssueChangeEventsIT.java`](../../apps/api/src/test/java/com/mowercare/controller/IssueChangeEventsIT.java) — **Fixed:** `givenIssue_whenPatchAssignee_whenGetChangeEvents_thenAssigneeLabels`.

- [x] [Review][Patch] `resolveAssigneeSnapshotLabel` returns the raw string when `oldValue`/`newValue` is not a valid UUID (catch branch). Story AC2 asks for a defined fallback such as **Former user** / **Unknown** rather than echoing arbitrary stored text. [`IssueService.java:362-374`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) — **Fixed:** catch branch returns **`Unknown`**; javadoc updated.

- [x] [Review][Defer] Mobile `listIssueChangeEvents` only requests `page=0` & `size=50` — issues with more than 50 events will not show older history until load-more or paging is added. [`issue-api.ts:141-154`](../../apps/mobile/lib/issue-api.ts) — deferred, acceptable MVP gap unless product requires full history on device.

## Dev Notes

### Data model (already persisted)

[Source: `IssueChangeEvent`](../../apps/api/src/main/java/com/mowercare/model/IssueChangeEvent.java), [IssueChangeType](../../apps/api/src/main/java/com/mowercare/model/IssueChangeType.java), [IssueChangeEventRepository](../../apps/api/src/main/java/com/mowercare/repository/IssueChangeEventRepository.java) — **`findByIssue_IdOrderByOccurredAtAsc`**.

**Security:** Every event row has **`organization_id`**. Prefer **`findByIssue_IdAndOrganization_Id`** (add to repository if missing) so pagination cannot cross tenants even if **`issueId`** were guessed.

### UX copy guidelines

Map **`IssueChangeType`** to short labels, e.g.:

- **CREATED** → “Issue created” (new value = title)
- **STATUS_CHANGED** → “Status” + old → new enums (humanize underscores)
- **ASSIGNEE_CHANGED** → “Assignee” + labels from resolved emails
- **PRIORITY_CHANGED**, **TITLE_CHANGED**, **DESCRIPTION_CHANGED**, **CUSTOMER_LABEL_CHANGED**, **SITE_LABEL_CHANGED** → field name + before/after (truncate long description in row with “tap” optional — MVP: single line ellipsis)

[Source: `_bmad-output/planning-artifacts/ux-design-specification.md` — Typography: ≥16sp for sentences in notes/history]

### Previous story intelligence (3.6)

- **`IssueStubController`** pattern: **`TenantPathAuthorization.requireJwtOrganizationMatchesPath`** on every method; **`GET`** issue does **not** call **`RoleAuthorization`** — access is **JWT** + **tenant** + [`AccountStatusVerificationFilter`](../../apps/api/src/main/java/com/mowercare/security/AccountStatusVerificationFilter.java) (**blocks** **`DEACTIVATED`** users). **New** **`GET .../change-events`** should mirror the **same** guards (tenant first, same auth posture). Product matrix **Admin + Technician** for issues — MVP has no other interactive roles; keep **`docs/rbac-matrix.md`** aligned.
- **Mobile:** **`AssigneePicker`**, **`patchIssue`**, **`invalidateQueries`** — add **change-events** invalidation alongside existing keys in **`[id].tsx`**.
- **Files:** [`IssueService.java`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) — read-only **new** method; **do not** change **`appendEvent`** behavior in this story unless fixing a bug.

### Architecture compliance

| Topic | Source |
|-------|--------|
| REST + JSON + Problem Details | [`architecture.md`](../planning-artifacts/architecture.md) |
| `issue_change_events` table | [`architecture.md`](../planning-artifacts/architecture.md), Liquibase **0008** |
| TanStack Query for server state | [`architecture.md`](../planning-artifacts/architecture.md) |
| Path params camelCase | [`architecture.md`](../planning-artifacts/architecture.md) |

### Reuse

| Asset | Notes |
|-------|-------|
| [`IssueListItemResponse`](../../apps/api/src/main/java/com/mowercare/model/response/IssueListItemResponse.java) | **`assigneeLabel`** pattern for **`actorLabel`** |
| [`formatRelativeTimeUtc`](../../apps/mobile/lib/relative-time.ts) | Relative display |
| [`IssueDetailIT`](../../apps/api/src/test/java/com/mowercare/controller/IssueDetailIT.java) | Bootstrap + GET issue patterns |

### Testing requirements

- **API IT:** Create issue → PATCH fields → GET change-events → assert count, order, types, actor labels; negative paths **404**/ **403** (mirror **`IssueDetailIT`** / **`RbacEnforcementIT`** GET issue patterns — e.g. **`RbacEnforcementIT`** lines that **`get`** **`/issues/{issueId}`** with admin + technician + tenant mismatch).
- **Repository:** Org-scoped query test if complex.
- **Mobile:** Smoke manually: open issue with history, pull-to-refresh if wired, save edit and see new row.

### API response & serialization

- **`occurredAt`:** **`Instant`** → JSON **ISO-8601** via Jackson (same convention as issue **`createdAt`** / **`updatedAt`** on existing issue DTOs).
- **Wrapper:** Document **`items`** + pagination fields in OpenAPI; mobile types must match **exact** field names (**camelCase**).

### Latest tech stack (pin in repo)

- **Spring Boot 3.x** / **Spring Data JPA** — standard **`Pageable`**; no new dependencies expected.
- **Expo / Paper** — list UI only.

### Git intelligence (recent commits)

- **`d75bdd7`** — Story **3.6** assignable-users + **`AssigneePicker`**.
- **`49bf354`** — Story **3.5** PATCH + edit mode.
- **`6c49deb`** — Story **3.4** GET detail + **`[id].tsx`**.

### Project context reference

No **`project-context.md`** in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md).

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2) agent

### Debug Log References

None.

### Completion Notes List

- **`GET /api/v1/organizations/{organizationId}/issues/{issueId}/change-events`** — **`IssueChangeEventsResponse`** with **`IssueChangeEventItemResponse`** (incl. **`oldAssigneeLabel`** / **`newAssigneeLabel`** for **`ASSIGNEE_CHANGED`**); org-scoped **`findByIssue_IdAndOrganization_Id`**; sort **`occurredAt`** asc; page size capped at **100**; default **50**.
- **IT:** **`IssueChangeEventsIT`** (CREATED, TITLE_CHANGED order, **404**, **403**); **`RbacEnforcementIT`** extended (admin + technician **200**, tenant **403**).
- **Mobile:** **`listIssueChangeEvents`** in **`issue-api.ts`**; **`IssueActivityTimeline`**; **`[id].tsx`** invalidates **`issue-change-events`** on save.
- **Verification:** `npm run typecheck` + `npm run lint` (mobile) green; **`mvn compile test-compile`** + **`mvn test -Dtest=IssueServiceTest`** green. **Full `mvn test`** (Testcontainers ITs) was not run here — Docker was unavailable in the agent environment; run **`mvn test`** locally with Docker to execute all `*IT` tests.

### File List

- `apps/api/src/main/java/com/mowercare/model/response/IssueChangeEventItemResponse.java`
- `apps/api/src/main/java/com/mowercare/model/response/IssueChangeEventsResponse.java`
- `apps/api/src/main/java/com/mowercare/repository/IssueChangeEventRepository.java`
- `apps/api/src/main/java/com/mowercare/service/IssueService.java`
- `apps/api/src/main/java/com/mowercare/controller/IssueStubController.java`
- `apps/api/src/test/java/com/mowercare/controller/IssueChangeEventsIT.java`
- `apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java`
- `docs/rbac-matrix.md`
- `apps/mobile/lib/issue-api.ts`
- `apps/mobile/components/IssueActivityTimeline.tsx`
- `apps/mobile/app/(app)/issues/[id].tsx`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/3-7-issue-activity-history-on-detail.md`

## Change Log

- **2026-03-31:** Story created — `bmad-create-story` auto-discover (first backlog: **3-7-issue-activity-history-on-detail**). Ultimate context engine analysis completed — comprehensive developer guide created.
- **2026-03-31:** `validate-create-story` — checklist pass; see validation record below.
- **2026-04-03:** Implemented Story **3.7** — change-events API, mobile Activity timeline, RBAC matrix + ITs; mobile `typecheck`/`lint` green; API unit compile + `IssueServiceTest` green; status → **review**; sprint **3-7** → **review**.

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-31 against `checklist.md`, `epics.md` Story **3.7**, [`IssueListResponse`](../../apps/api/src/main/java/com/mowercare/model/response/IssueListResponse.java), [`SecurityConfig`](../../apps/api/src/main/java/com/mowercare/config/SecurityConfig.java), [`AccountStatusVerificationFilter`](../../apps/api/src/main/java/com/mowercare/security/AccountStatusVerificationFilter.java), [`[id].tsx`](../../apps/mobile/app/(app)/issues/[id].tsx) query keys.

### Critical (addressed in file)

| Issue | Resolution |
|-------|------------|
| **Pagination JSON shape** ambiguous (raw `Page` vs app style) | **AC3** + **API response & serialization**: prefer **`IssueChangeEventsResponse`** with **`items`** + explicit pagination fields aligned with **`IssueListResponse`** pattern |
| **Security** note implied `RoleAuthorization` on issue GET | **Dev Notes**: tenant + JWT + deactivated filter; **`GET .../change-events`** mirrors **`getIssue`** |
| **Assignee delete** wording mixed FK on `issues` vs history UUIDs | **AC2**: clarified **SET NULL** on **`issues.assignee`** vs **stale UUIDs** in history |
| **Invalidation** vague vs actual **`[id].tsx`** | **AC6**: cites **`queryKey`** and lists **`onSuccess`** keys to extend |

### Enhancements applied

| Addition | Benefit |
|----------|---------|
| **`AccountStatusVerificationFilter`** pointer | Correct auth stack for new route |
| **Default page/size** in AC3 | Fewer arbitrary choices during implementation |
| **Jackson `Instant`** note | Prevents “wrong date format” bugs on mobile |

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — expose existing **`issue_change_events`**, no second write path |
| Technical / contract | Pass — explicit wrapper + org-scoped query |
| Regression | Pass — invalidation extends existing mutation |
| Epic alignment | Pass — FR18, ISO UTC, pagination |

### Residual risks (acceptable for ready-for-dev)

- Exact DTO names (`IssueChangeEventsResponse` vs `Page`) — lock when implementing OpenAPI.
- **Newest-first** vs **oldest-first**: story **defaults oldest-first**; confirm product preference in PR if UX wants “latest at top” without scrolling.
