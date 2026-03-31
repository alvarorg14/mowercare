# Story 3.5: Update issue fields, assignment, status, resolve/close

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. validate-create-story (checklist.md): see Validation record at end. -->

## Story

As a **Technician or Admin**,
I want **to update issue attributes including status and assignment**,
so that **ownership and state stay current (FR15–FR17)**.

**Implements:** **FR15**, **FR16**, **FR17**; **UX-DR5** (assignment UX — **full** searchable picker deferred to **3.6** per epics; **3.5** ships minimal flows below); **UX-DR6** (MutationFeedback — pending / success / error + Retry), **UX-DR12** (destructive or impactful actions use clear hierarchy; confirm when closing if product-appropriate).

**Epic:** Epic 3 — Issues — capture, triage, ownership & history.

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 3.5])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** permission to edit **When** user changes status, assignment, or permitted fields **Then** server validates transitions; history records material changes; MutationFeedback covers mutation lifecycle | **API:** org-scoped **PATCH** (or equivalent) invoking **`IssueService`** update methods; **`issue_change_events`** already written in service **And** **400** with stable **`code`** on illegal status transition **And** **OpenAPI** + mobile **Zod** aligned **And** TanStack Query **`useMutation`** + **`invalidateQueries`** for **`['issue', orgId, issueId]`** and list keys **`['issues', orgId, scope]`** (match existing list query keys) |
| **Given** resolve/close **When** user completes workflow **Then** state model for MVP is enforced and visible on detail | **Service:** enforce **documented transition rules** before **`updateStatus`** **And** **RESOLVED** / **CLOSED** map to enum **`IssueStatus`**; detail screen reflects updated chips after mutation success |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **3.4** | **Builds on** read-only detail — add **edit** affordances and mutations; **reuse** **`IssueDetail`**, **`getIssue`**, **`queryKey`** `['issue', orgId, issueId]`. |
| **3.6** | **AssigneePicker** (searchable org list, **UX-DR5**) — **not required** in **3.5**. **3.5** ships **API** assignment + **minimal** mobile assignment (**Assign to me**, **Unassign**) because **Technicians** cannot **`GET /users`** ([`docs/rbac-matrix.md`](../../docs/rbac-matrix.md)); full picker lands in **3.6**. **Optional:** **Admin** may add a **simple** assignee list (no Searchbar requirement) using existing **`listEmployeeUsers`** / **`GET .../users`** — not required to pass **3.5** if scope stays **Technician-parity** only. |
| **3.7** | **Timeline UI** for history — **out of scope**; events already appended in **3.1**/**IssueService**. |
| **4.x** | Notifications on update — **out of scope** for **3.5**. |

## Acceptance Criteria

1. **REST — patch issue (partial update)**  
   **Given** valid **Bearer** JWT with **`organizationId`** matching path  
   **When** `PATCH /api/v1/organizations/{organizationId}/issues/{issueId}` is called with **`application/json`** body containing **one or more** optional fields (camelCase): **`title`**, **`description`**, **`status`**, **`priority`**, **`assigneeUserId`** (nullable for unassign), **`customerLabel`**, **`siteLabel`**  
   **Then** response is **200** with body matching **`IssueDetailResponse`** (same shape as **GET** detail) reflecting persisted state  
   **And** omitted fields are unchanged; **`null`** for nullable fields clears where product allows (document **`description`**, **`customerLabel`**, **`siteLabel`**, **`assigneeUserId`** semantics — align with create: empty string may normalize to **null** where consistent)  
   **And** **`TenantPathAuthorization.requireJwtOrganizationMatchesPath`** runs first  
   **And** **Admin** and **Technician** are both allowed for this route ([`docs/rbac-matrix.md`](../../docs/rbac-matrix.md) — extend matrix with PATCH row)  
   **And** OpenAPI documents **`IssueUpdateRequest`** (or equivalent) and response **`IssueDetailResponse`**.  
   **And** request body must include **at least one** updatable field after parsing; **`{}`** or all-**null** with no changes → **400** with **`VALIDATION_ERROR`** (or equivalent stable **`code`**) — avoids ambiguous no-op **PATCH**.

2. **REST — not found / tenant**  
   **Given** issue not in org or unknown id  
   **When** PATCH is called  
   **Then** **404** + **`NOT_FOUND`** (existing **`ResourceNotFoundException`** pattern)  
   **Given** JWT org ≠ path  
   **Then** **403** **`TENANT_ACCESS_DENIED`**.

3. **REST — assignee validation**  
   **Given** **`assigneeUserId`** set to a user **not** in this organization  
   **When** PATCH applies assignment  
   **Then** **404** assignee not found (same semantics as create — **`ResourceNotFoundException`** “Assignee not found”).

4. **Service — reuse existing methods; add transition validation**  
   **Given** [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) already implements **`updateStatus`**, **`updateAssignee`**, **`updatePriority`**, **`updateTitle`**, **`updateDescription`**, **`updateCustomerLabel`**, **`updateSiteLabel`** with **`appendEvent`**  
   **When** implementing PATCH orchestration  
   **Then** delegate to these methods **per changed field** (avoid duplicate event rows — no-op if value unchanged, already handled in service)  
   **And** introduce **explicit MVP status transition validation** (new helper or domain method): reject disallowed transitions with a **dedicated exception** mapped to **400** and stable **`code`** (e.g. **`INVALID_STATUS_TRANSITION`**) — **document the allowed graph** in this story’s Dev Notes (see below). **Do not** silently accept **any transition out of `CLOSED`** to another status when the table marks **`CLOSED`** as terminal (see **Field edits when CLOSED** for non-status fields).

5. **Controller**  
   **Given** [`IssueStubController`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java)  
   **When** adding PATCH  
   **Then** map **`PATCH /{organizationId}/issues/{issueId}`**, **`@AuthenticationPrincipal Jwt jwt`**, resolve **`actorUserId`** from JWT **`sub`**, call new service method e.g. **`patchIssue(organizationId, issueId, actorUserId, update)`**.

6. **Mobile — mutations and cache**  
   **Given** issue detail screen [`[id].tsx`](../../apps/mobile/app/(app)/issues/[id].tsx)  
   **When** user saves changes  
   **Then** use **`useMutation`** calling new **`patchIssue(issueId, body)`** in [`issue-api.ts`](../../apps/mobile/lib/issue-api.ts)  
   **And** **MutationFeedback** (**UX-DR6**): show **pending** (disable or loading), **success** (brief Snackbar or Paper feedback), **error** with **Problem Details**-aware copy + **Retry**  
   **And** on success: **`queryClient.invalidateQueries`** for detail **`['issue', orgId, issueId]`** and list data — list screen uses **`['issues', orgId, scope]`** (see [`index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx)); **`create.tsx`** uses **`invalidateQueries({ queryKey: ['issues'] })`** prefix invalidation — use the same or tighter **`['issues', orgId]`** so rows refresh after edit.

7. **Mobile — minimal assignment (FR16 without 3.6)**  
   **Given** **Technician** cannot list org users via API  
   **When** implementing assignment UX  
   **Then** provide **Assign to me** (sets **`assigneeUserId`** to current user id from session/JWT) and **Unassign** (**`null`**)  
   **And** defer searchable member list to **3.6** (no requirement to call **`GET .../users`** from Technician in **3.5**).

8. **Mobile — edit surfaces**  
   **Given** MVP needs status including **RESOLVED** / **CLOSED** and field edits  
   **When** UX is implemented  
   **Then** user can change **status** (menu or segmented control — consistent with Paper), **priority**, and **text fields** supported by API; layout stays consistent with **UX-DR18** (sticky header remains; form may scroll below).  
   **And** optional **Dialog** confirm before moving to **CLOSED** if you introduce confirm (**UX-DR12** — at least one primary action per surface).

9. **Docs**  
   **Given** [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md)  
   **When** PATCH ships  
   **Then** add row for **`PATCH /api/v1/organizations/{organizationId}/issues/{issueId}`** — **Allow** / **Allow** — update issue fields.

10. **Tests**  
    **Given** existing IT patterns (**`IssueDetailIT`**, **`AbstractPostgresIntegrationTest`**)  
    **When** `mvn test` runs  
    **Then** integration tests cover **200** patch (field change + response shape), **404** issue, **403** tenant, **400** invalid transition, assignee **404**  
    **And** **`RbacEnforcementIT`**: **Technician** and **Admin** **PATCH** allowed (no **`FORBIDDEN_ROLE`** for this route).  
    **And** mobile: **`npm run typecheck`** (and **lint** if CI uses it) passes.

## Tasks / Subtasks

- [x] **Domain — status transitions** (AC: 4, 10)
  - [x] Document and implement **MVP transition table** (see Dev Notes); unit or service-level tests for validator.

- [x] **API — request DTO + exception mapping** (AC: 1, 4, 5)
  - [x] `IssuePatch` parses **`JsonNode`** (known fields only; rejects unknown keys); **`@JsonInclude`** not used — client sends explicit keys.
  - [x] Reject **empty** update (`{}`) per AC1 via **`EmptyIssuePatchException`**.
  - [x] **`InvalidStatusTransitionException`**, **`IssueClosedException`**, **`InvalidIssuePatchException`**, **`EmptyIssuePatchException`** + **`ApiExceptionHandler`** → **400** + **`code`**.

- [x] **API — service orchestration** (AC: 4, 5)
  - [x] **`IssueService.patchIssue`** delegates to existing **`update*`** methods; **`updateStatus`** calls **`IssueStatusTransitionValidator`**.

- [x] **API — controller + OpenAPI** (AC: 1, 5)
  - [x] **`PATCH`** on **`IssueStubController`**; tag description updated.

- [x] **API — tests** (AC: 10)
  - [x] **`IssuePatchIT`**; **`RbacEnforcementIT`** PATCH rows; **`IssueStatusTransitionValidatorTest`**.

- [x] **Docs** (AC: 9)
  - [x] **`docs/rbac-matrix.md`**

- [x] **Mobile — API + types** (AC: 6)
  - [x] **`patchIssue`** + **`IssueUpdatePayload`**; diff-based payload (no separate Zod schema).

- [x] **Mobile — UI** (AC: 6–8)
  - [x] Edit mode on **`[id].tsx`**; **Assign to me** / **Unassign**; status/priority menus; **Snackbar** + close-confirm **Dialog** for **CLOSED**.

- [x] **Verification** (AC: 10)
  - [x] `mvn -q test` in `apps/api`
  - [x] `npm run typecheck` and `npm run lint` in `apps/mobile`

### Review Findings

- [x] [Review][Patch] Sprint `last_updated` regressed (`21:00` → `18:15`) in `_bmad-output/implementation-artifacts/sprint-status.yaml` — restore monotonic timestamp or align with edit time.

- [x] [Review][Patch] OpenAPI: PATCH body is `JsonNode` without `IssueUpdateRequest` (or equivalent) schema — AC1 expects documented request model; add Springdoc `@Schema` / DTO so Swagger shows field list.

- [x] [Review][Patch] Mutation UX-DR6: failed save shows `Snackbar` only; no **Retry** action for `useMutation` errors (detail load has Retry). Add action to re-run last patch or reopen save.

- [x] [Review][Patch] `IssuePatch.wouldChange` compares raw JSON strings to persisted values before the same trim/null normalization as `patchIssue` — a **CLOSED** issue could get false **`ISSUE_CLOSED`** (e.g. whitespace-only description). Align comparison with service normalization in `IssuePatch.java` / `IssueService.patchIssue`.

- [x] [Review][Patch] **Assign to me** when access token or subject is missing returns silently — surface `Snackbar` or disabled state with reason (`IssueDetailScreen`).

_Batch-applied 2026-03-31 (code review option 0). Also: `IssueStubController` parses PATCH body via `ObjectMapper.readTree(String)` for Spring MVC compatibility; `IssueService.patchIssue` runs status transition validation before **`ISSUE_CLOSED`** when reopening from **CLOSED**._

## Dev Notes

### MVP status transition table (draft — implement explicitly)

Align with [`IssueStatus`](../../apps/api/src/main/java/com/mowercare/model/IssueStatus.java): **OPEN**, **IN_PROGRESS**, **WAITING**, **RESOLVED**, **CLOSED**.

| From \\ To | OPEN | IN_PROGRESS | WAITING | RESOLVED | CLOSED |
|------------|------|-------------|---------|----------|--------|
| OPEN | ✓ | ✓ | ✓ | ✓ | ✓ |
| IN_PROGRESS | ✓ | ✓ | ✓ | ✓ | ✓ |
| WAITING | ✓ | ✓ | ✓ | ✓ | ✓ |
| RESOLVED | ✓ | ✓ | ✓ | ✓ | ✓ |
| CLOSED | ✗ | ✗ | ✗ | ✗ | ✓ |

*(✓ = transition allowed; “From” row = current status, “To” column = requested status. Diagonal **CLOSED → CLOSED** is no-op, handled by existing service idempotency.)*

**Interpretation:** **CLOSED** is **terminal** — no transitions **out** of **CLOSED** to any other status. **RESOLVED** may still move to **CLOSED** or back to active states (**OPEN** / **IN_PROGRESS** / **WAITING**) for rework. If product later requires **reopen** from **CLOSED**, run **correct-course** and allow **CLOSED → OPEN** (or **IN_PROGRESS**) only.

**Field edits when CLOSED:** Prefer **reject** **PATCH** that changes **title**, **description**, **labels**, **priority**, **assignment**, or **status** when current status is **CLOSED** (same **`INVALID_STATUS_TRANSITION`** or a dedicated **`ISSUE_CLOSED`** code). If you only block **status** changes, document that other fields remain editable — default here is **full immutability** once **CLOSED** for simplicity.

### Scope boundaries

- **In scope:** **PATCH** API; **status**/**priority**/**text**/**assignment** (service already logs history); **minimal** assign UX; **MutationFeedback**; transition validation; RBAC + tenant tests.
- **Out of scope:** **AssigneePicker** (**3.6**), **history timeline** (**3.7**), **notifications** on update (**4.x**), replacing **`IssueStubController`** name (deferred debt).

### Previous story intelligence (3.4)

- Detail **`queryKey`:** **`['issue', orgId, issueId]`** — invalidate on patch success.
- **`getIssue`**, **`IssueDetail`** types — PATCH response should match **GET** to avoid two shapes.
- Reuse **chips** / **`issueStatusTokens`** / **`statusLabel`** patterns from **[id].tsx**.
- **UUID** guard and **missing org** **`Banner`** — keep behavior when opening mutations.
- **`ApiProblemError`** for error copy + Retry.

### Architecture compliance

| Topic | Source |
|-------|--------|
| REST + JSON + Problem Details + stable **`code`** | [`architecture.md`](../planning-artifacts/architecture.md) |
| **`{organizationId}`** / **`{issueId}`** path params | [`architecture.md`](../planning-artifacts/architecture.md) |
| TanStack Query **`invalidateQueries`** after mutations | [`architecture.md`](../planning-artifacts/architecture.md) |
| Domain events / history | **`IssueChangeEvent`** already persisted per field update |

### Existing code to reuse

| Asset | Notes |
|-------|-------|
| [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) | All granular **`update*`** methods + **`appendEvent`** |
| [`IssueChangeType`](../../apps/api/src/main/java/com/mowercare/model/IssueChangeType.java) | Event types already cover material fields |
| [`IssueDetailResponse`](../../apps/api/src/main/java/com/mowercare/model/response/IssueDetailResponse.java) | Return type for PATCH |
| Mobile **session** | **`getSessionOrganizationId`**, subject for “Assign to me” via **`getSubjectFromAccessToken`** (see [`team.tsx`](../../apps/mobile/app/(app)/team.tsx) pattern) |

### File structure requirements

| Area | Guidance |
|------|----------|
| API request DTO | `com.mowercare.model.request` |
| API exception | `com.mowercare.exception` + handler registration |
| Controller | `IssueStubController` |
| Mobile | `issue-api.ts`, `app/(app)/issues/[id].tsx` |

### Testing requirements

- **API:** Assert **issue_change_events** count or latest event type when needed (optional depth); focus on HTTP contract + RBAC.
- **Mobile:** typecheck + manual spot-check of mutation states.

### Git intelligence (recent commits)

- **`6c49deb`** — Story **3.4** issue detail GET + mobile.
- **`b761be6`** — Story **3.3** list + query keys for issues list.

### Project context reference

- No `project-context.md` in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md).

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2) agent

### Debug Log References

None.

### Completion Notes List

- **`PATCH /organizations/{organizationId}/issues/{issueId}`** — body parsed as JSON via **`ObjectMapper.readTree`**, **`IssuePatch.from(JsonNode)`** validates known fields; **`IssueService.patchIssue`** applies updates; **`CLOSED`**: attempted reopen → **`INVALID_STATUS_TRANSITION`**; other edits → **`IssueClosedException`** (**`ISSUE_CLOSED`**).
- **IT:** **`IssuePatchIT`** (200 title, 400 empty/unknown, 404/403, closed immutability, bad assignee); **`RbacEnforcementIT`** PATCH Admin + Technician; **`IssueStatusTransitionValidatorTest`** unit test.
- **Mobile:** **`patchIssue`**, edit mode with diff payload, **Assign to me** / **Unassign**, close **Dialog**, **`Snackbar`**, **`invalidateQueries`** `issue` + `issues`.

### File List

- `apps/api/src/main/java/com/mowercare/issue/IssueStatusTransitionValidator.java`
- `apps/api/src/main/java/com/mowercare/model/request/IssuePatch.java`
- `apps/api/src/main/java/com/mowercare/model/request/IssueUpdateRequest.java`
- `apps/api/src/main/java/com/mowercare/exception/InvalidStatusTransitionException.java`
- `apps/api/src/main/java/com/mowercare/exception/IssueClosedException.java`
- `apps/api/src/main/java/com/mowercare/exception/EmptyIssuePatchException.java`
- `apps/api/src/main/java/com/mowercare/exception/InvalidIssuePatchException.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/main/java/com/mowercare/service/IssueService.java`
- `apps/api/src/main/java/com/mowercare/controller/IssueStubController.java`
- `apps/api/src/test/java/com/mowercare/issue/IssueStatusTransitionValidatorTest.java`
- `apps/api/src/test/java/com/mowercare/controller/IssuePatchIT.java`
- `apps/api/src/test/java/com/mowercare/controller/RbacEnforcementIT.java`
- `docs/rbac-matrix.md`
- `apps/mobile/lib/issue-api.ts`
- `apps/mobile/app/(app)/issues/[id].tsx`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

## Change Log

- **2026-03-31:** Implemented Story **3.5** — PATCH API + mobile edit; `mvn test`, mobile `typecheck`/`lint` green; status → **review**; sprint **3-5** → **review**.
- **2026-03-31:** `validate-create-story` — epic **UX-DR5** alignment; query key typo; empty **PATCH** AC; AC4 clarity; optional Admin assign; Validation record appended.
- **2026-03-31:** Story created — `bmad-create-story` auto-discover (first backlog: **3-5-update-issue-fields-assignment-status-resolve-close**).

## Clarifications / questions saved for end

1. **Reopen from CLOSED:** Not in MVP table above; see Dev Notes if product changes.
2. **PATCH vs multiple endpoints:** Single **PATCH** is recommended; if the team prefers **PUT** sub-resources, document deviation in PR and keep behavior identical.

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-31 against `checklist.md`, `epics.md` Story **3.5**, [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java), [`ApiExceptionHandler`](../../apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java), [`index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx) query keys.

### Critical (addressed in file)

| Issue | Resolution |
|-------|------------|
| **Epics traceability** — `epics.md` lists **UX-DR5** alongside **UX-DR6** / **UX-DR12** for Story **3.5** | **Implements** line now includes **UX-DR5** with explicit deferral of full picker to **3.6** |
| **Query key typo** — `['issue', org, id]` | Corrected to **`['issue', orgId, issueId]`** in epic traceability table |
| **Empty PATCH** — developer might return **200** for `{}` | AC1 requires **400** **`VALIDATION_ERROR`** when no updatable content |
| **AC4 wording** — “**CLOSED → OPEN**” mixed transition rules with **field immutability** | Rewritten to **terminal CLOSED** + reference to **Field edits when CLOSED** |
| **Admin vs Technician assignment** — checklist “wrong approach” risk | Cross-story **3.6** row notes **optional** Admin **`GET .../users`** simple list; **Technician** remains **Assign to me** / **Unassign** |

### Enhancements applied

| Addition | Benefit |
|----------|---------|
| **`@JsonInclude(NON_NULL)`** on patch DTO task | Prevents accidental clearing of fields with default JSON serialization |
| **Optional Admin** assignee list | Aligns with RBAC without forcing **3.6** scope creep |

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — delegate to existing **`IssueService`** `update*` methods |
| Technical accuracy | Pass — **`NOT_FOUND`** / **`TENANT_ACCESS_DENIED`** match **`ApiExceptionHandler`** |
| File locations | Pass |
| Regression / scope | Pass — **3.6**/**3.7**/**4.x** boundaries preserved |
| Epic alignment | Pass — FR15–17, UX-DR5–6, UX-DR12 |

### Residual risks (acceptable for ready-for-dev)

- **Admin** simple assignee list is optional; shipping **Technician-parity** only is still valid **3.5**.
- **Deactivated** users as assignees: not spelled in story; follow **`resolveAssigneeOrNull`** / create-issue behavior unless a gap is found during dev.
