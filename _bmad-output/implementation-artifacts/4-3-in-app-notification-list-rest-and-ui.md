# Story 4.3: In-app notification list (REST) and UI

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As an **employee**,
I want **an in-app feed of notifications**,
so that **I can see activity even when push is off (FR22; UX-DR7)**.

**Implements:** **FR22**; **UX-DR7**; **UX-DR11** (Notifications tab).

**Epic:** Epic 4 — Awareness — in-app notifications & push.

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 4.3])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** authenticated user **When** they open Notifications **Then** list shows issue ref, event type, time, read/unread; TanStack Query handles refresh | **Backend:** org-scoped REST list (and read-state updates) over **`notification_recipients`** joined to **`notification_events`** + **`issues`**. **Mobile:** Notifications surface + **TanStack Query** `useQuery` + pull-to-refresh / refetch. |
| **Given** empty feed **When** no notifications **Then** EmptyState explains the situation | **Mobile:** dedicated empty copy (not the same as “no issues” filters). |

### Cross-story boundaries

| Story | Relationship |
|-------|--------------|
| **4.1–4.2** | **Depends on** `notification_events` + per-user **`notification_recipients`** (indexed by org + recipient + time). **4.3** only **reads** rows the current user is allowed to see (`recipient_user_id` = JWT subject). |
| **4.4–4.5** | Push/deep links **not** in scope; no FCM, no device tokens, no push tap handling. |

---

## Acceptance Criteria

1. **Read state in data model**  
   **Given** `notification_recipients` exists without read tracking ([`0011-notification-recipients.yaml`](../../apps/api/src/main/resources/db/changelog/changes/0011-notification-recipients.yaml))  
   **When** migrations apply  
   **Then** a new column exists (e.g. **`read_at`** `timestamptz` **NULL** = unread, non-null = read) on **`notification_recipients`**, with Liquibase **`0012-...yaml`** registered in [`db.changelog-master.yaml`](../../apps/api/src/main/resources/db/changelog/db.changelog-master.yaml)  
   **And** JPA field on [`NotificationRecipient`](../../apps/api/src/main/java/com/mowercare/model/NotificationRecipient.java) + optional small helper `isRead()`  
   **And** default for **existing** rows: **NULL** (treat as unread) is acceptable for MVP.

2. **List API — paginated, tenant-safe, user-scoped**  
   **Given** an authenticated **employee** JWT (`ADMIN` or `TECHNICIAN`)  
   **When** `GET /api/v1/organizations/{organizationId}/notifications` is called with standard paging (align with issues list: `page`, `size`, max cap if issues use one)  
   **Then** response includes **only** `notification_recipients` where `organization_id` = path org **and** `recipient_user_id` = JWT `sub`  
   **And** each item exposes at minimum: **recipient id** (for mark-read), **issue id**, **issue title** (or stable ref string), **`eventType`** (taxonomy string, e.g. `issue.created`), **occurredAt** (from parent `notification_events`), **read** (derived from `read_at`)  
   **And** sort default: **newest first** (use recipient `created_at` or event `occurred_at` — pick one, document in OpenAPI; prefer aligning with existing list patterns)  
   **And** [`TenantPathAuthorization.requireJwtOrganizationMatchesPath`](../../apps/api/src/main/java/com/mowercare/security/TenantPathAuthorization.java) + [`RoleAuthorization.requireEmployee`](../../apps/api/src/main/java/com/mowercare/security/RoleAuthorization.java) on the controller.

3. **Mark read API**  
   **Given** the same caller as (2)  
   **When** `PATCH` (or `POST`) marks a single notification as read by **recipient row id** under the same org path  
   **Then** only a row belonging to **that user** in **that org** can be updated; otherwise **404** or **403** consistent with existing API error shape ([RFC 7807 Problem Details](../../apps/mobile/lib/http.ts))  
   **And** idempotent: second call leaves `read_at` set (no error).

4. **Tests (API)**  
   **Given** [`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java) patterns  
   **When** `mvn test` runs  
   **Then** integration tests prove: list returns **only** current user’s recipients; **cross-user** / **cross-org** access denied; mark-read updates `read_at`; empty list shape is valid.

5. **Mobile — Notifications tab + list + EmptyState**  
   **Given** the authenticated session  
   **When** the user opens the **Notifications** destination (tab per **UX-DR11**)  
   **Then** a **FlatList** (or equivalent) shows rows with **issue ref**, **event type** (user-visible label mapped from taxonomy strings), **time**, **read vs unread** styling  
   **And** **TanStack Query** loads the list (`useQuery` + `queryKey` including `orgId`) and supports **pull-to-refresh** / `refetch` (match **Issues** list behavior in [`issues/index.tsx`](../../apps/mobile/app/(app)/issues/index.tsx))  
   **And** when there are **no** notifications, an **EmptyState** explains that there is no activity yet (distinct copy from issue list empty states).

6. **Mobile — Navigation (UX-DR11)**  
   **Given** IA: Issues vs Notifications vs Settings  
   **When** implemented  
   **Then** employees can reach **Notifications** without deep-link-only hacks — e.g. **bottom tabs** or **material top tabs** under `(app)` so **Issues**, **Notifications**, and **Settings** are peers (align with [architecture mobile mapping](../planning-artifacts/architecture.md) `features/notification/`, `notification list tab`).  
   **And** existing routes continue to work (adjust `(app)/index` redirect or entry so the shell is coherent).

7. **Scope**  
   **Given** this is **4.3**  
   **When** complete  
   **Then** **no** push registration, **no** `expo-notifications` dispatch, **no** deep link from push — those are **4.4–4.5**.

---

## Tasks / Subtasks

- [x] **Schema + model** (AC: 1)
  - [x] Liquibase `0012-notification-recipients-read-at.yaml` (name TBD); `ALTER TABLE notification_recipients ADD read_at`.

- [x] **Repository + service** (AC: 2–3)
  - [x] `NotificationRecipientRepository`: pageable query by `organization_id` + `recipient_user_id`, fetch join `notificationEvent` + `issue` (avoid N+1; use `@EntityGraph` or explicit query).
  - [x] Service method(s) for list DTO mapping + mark read by id + user guard.

- [x] **Controller** (AC: 2–4)
  - [x] New controller under `com.mowercare.controller`, `@RequestMapping("/api/v1/organizations")`, OpenAPI annotations like [`IssueStubController`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java).
  - [x] Response records under `com.mowercare.model.response` (camelCase JSON per architecture).

- [x] **API tests** (AC: 4)
  - [x] New IT or extend existing — multi-user fixture if needed (reuse patterns from [`IssueServiceIT`](../../apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java)).

- [x] **Mobile API client** (AC: 5)
  - [x] e.g. `lib/notification-api.ts` — `listNotifications`, `markNotificationRead`, types, `queryKey` helper.

- [x] **Mobile UI** (AC: 5–6)
  - [x] `NotificationRow` component (UX-DR7: issue ref, event type, time, read/unread).
  - [x] Notifications screen + tab layout refactor.
  - [x] Optional: tap row → `router.push` to issue detail [`/[id]`](../../apps/mobile/app/(app)/(tabs)/issues/[id].tsx)) if product-safe for all event types in MVP.

### Review Findings

- [x] [Review][Patch] Mark-read mutation errors are not surfaced — `onOpen` fires `markReadMutation.mutate` then navigates immediately; if `PATCH` fails, the user may still see an unread row after returning from issue detail. Consider `onError` on the mutation, toast, or awaiting success before navigation. [`apps/mobile/app/(app)/(tabs)/notifications/index.tsx`] — fixed: `mutateAsync` + banner on failure.

- [x] [Review][Patch] Integration test gap — `GET` wrong-org path returns 403; add a parallel test that `PATCH .../notifications/{recipientId}/read` with `organizationId` not matching the JWT org returns **403** with the same problem shape as tenant denial. [`apps/api/src/test/java/com/mowercare/controller/NotificationInboxIT.java`] — fixed: `givenOrg_whenPatchNotificationReadWrongOrg_thenForbidden`.

- [x] [Review][Patch] Unknown taxonomy strings in `formatNotificationEventType` fall back to a lightly transformed raw string; new backend event types may show awkward labels. Consider a neutral label (e.g. “Activity”) or title case. [`apps/mobile/lib/notification-api.ts`] — fixed: title-case words; empty → `Activity`.

- [x] [Review][Defer] Mobile feed requests only the first page (fixed `page=0`, `size=50`); users with more than 50 notifications cannot load older items in the UI. [`apps/mobile/app/(app)/(tabs)/notifications/index.tsx`] — deferred, MVP acceptable; follow-up if pagination becomes a product requirement.

---

## Dev Notes

### Current codebase anchors

| Area | Location |
|------|----------|
| Recipient rows | [`NotificationRecipient`](../../apps/api/src/main/java/com/mowercare/model/NotificationRecipient.java), [`NotificationRecipientRepository`](../../apps/api/src/main/java/com/mowercare/repository/NotificationRecipientRepository.java) |
| Events + taxonomy | [`NotificationEvent`](../../apps/api/src/main/java/com/mowercare/model/NotificationEvent.java), [`NotificationEventType`](../../apps/api/src/main/java/com/mowercare/model/NotificationEventType.java) |
| Issue title for ref | [`Issue`](../../apps/api/src/main/java/com/mowercare/model/Issue.java) |
| Tenant + role | [`TenantPathAuthorization`](../../apps/api/src/main/java/com/mowercare/security/TenantPathAuthorization.java), [`RoleAuthorization`](../../apps/api/src/main/java/com/mowercare/security/RoleAuthorization.java) |
| Mobile HTTP | [`authenticatedFetchJson`](../../apps/mobile/lib/api.ts), [`ApiProblemError`](../../apps/mobile/lib/http.ts) |
| List + refresh pattern | [`apps/mobile/app/(app)/(tabs)/issues/index.tsx`](../../apps/mobile/app/(app)/(tabs)/issues/index.tsx) |

### Architecture compliance

| Topic | Source |
|-------|--------|
| REST + JSON, `/api/v1`, camelCase | [`architecture.md`](../planning-artifacts/architecture.md) — REST / naming |
| In-app via REST refresh (no WebSockets MVP) | Same — Communication patterns |
| Mobile: Expo Router, TanStack Query | [`architecture.md`](../planning-artifacts/architecture.md) — Mobile stack |
| `notification/` feature area | [`architecture.md`](../planning-artifacts/architecture.md) — Requirements → code mapping |

### UX compliance

| Topic | Source |
|-------|--------|
| **NotificationRow** — issue ref, event type, time, read/unread | [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md) — NotificationRow |
| Tabs Issues / Notifications / Settings | UX-DR11 + roadmap Phase 2 in same doc |
| Empty vs filtered empty | Different copy from issue list “filters” empty |

### Library / framework

- **API:** Spring Data JPA paging, existing Jackson + Problem Details — no new stack.
- **Mobile:** React Native Paper, `@tanstack/react-query`, Expo Router — match existing versions in repo (`package.json`).

### File structure (expected touchpoints)

| Path | Purpose |
|------|---------|
| `apps/api/src/main/resources/db/changelog/changes/0012-*.yaml` | `read_at` on `notification_recipients` |
| `apps/api/.../controller/*Notification*Controller.java` | REST endpoints |
| `apps/api/.../service/Notification*QueryService.java` (name TBD) | List + mark read |
| `apps/api/.../model/response/Notification*.java` | JSON records |
| `apps/mobile/lib/notification-api.ts` | Client |
| `apps/mobile/app/(app)/...` | Tabs + notifications route + `NotificationRow` |

### Previous story intelligence (4.2)

- Per-user rows are **`notification_recipients`** keyed by `(notification_event_id, recipient_user_id)`; list queries **must** filter `recipient_user_id` = current user.
- Fan-out already created rows for eligible users — **list is read-only** except **`read_at`** updates.
- **4.2** explicitly deferred HTTP — this story adds the **first** public notification APIs.

### Git intelligence (recent commits)

- **4.2** added `notification_recipients`, fan-out, and `IssueServiceIT` recipient assertions — extend repository/service layer for **read** + **query**, do not change fan-out rules in 4.3 unless fixing a defect.

### Latest tech information

- No version bumps required; use **Spring `Pageable`** defaults consistent with **`IssueStubController`** list if present (check `PageableDefault` / max size).

### Project context reference

- No **`project-context.md`** in repo; rely on architecture + **this story** + linked code.

---

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

### Completion Notes List

- Liquibase **0012** adds nullable **`read_at`** on **`notification_recipients`**; **`NotificationRecipient`** has **`markRead`**, **`isRead()`**.
- **`NotificationInboxService`** + **`NotificationInboxController`**: `GET .../notifications` (page/size, max 100, sort `createdAt` desc on recipient), `PATCH .../notifications/{id}/read` (204, idempotent).
- **`NotificationInboxIT`**: list + mark-read + tenant 403 + cross-user 404 + empty list.
- Mobile: **`(app)/(tabs)/`** — Issues, Notifications, Settings tabs; **`notification-api.ts`**, **`NotificationRow`**, notifications screen with pull-to-refresh; **`(app)/index`** redirects to Issues tab; issue routes moved under **`(tabs)/issues`**.

### File List

- `apps/api/src/main/resources/db/changelog/changes/0012-notification-recipients-read-at.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/java/com/mowercare/model/NotificationRecipient.java`
- `apps/api/src/main/java/com/mowercare/repository/NotificationRecipientRepository.java`
- `apps/api/src/main/java/com/mowercare/service/NotificationInboxService.java`
- `apps/api/src/main/java/com/mowercare/controller/NotificationInboxController.java`
- `apps/api/src/main/java/com/mowercare/model/response/NotificationItemResponse.java`
- `apps/api/src/main/java/com/mowercare/model/response/NotificationListResponse.java`
- `apps/api/src/test/java/com/mowercare/controller/NotificationInboxIT.java`
- `apps/mobile/lib/notification-api.ts`
- `apps/mobile/components/NotificationRow.tsx`
- `apps/mobile/app/(app)/(tabs)/_layout.tsx`
- `apps/mobile/app/(app)/(tabs)/notifications/index.tsx`
- `apps/mobile/app/(app)/(tabs)/issues/_layout.tsx`
- `apps/mobile/app/(app)/(tabs)/issues/index.tsx`
- `apps/mobile/app/(app)/(tabs)/issues/[id].tsx`
- `apps/mobile/app/(app)/(tabs)/issues/create.tsx`
- `apps/mobile/app/(app)/(tabs)/settings.tsx`
- `apps/mobile/app/(app)/index.tsx`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- **2026-04-06:** Story 4.3 — in-app notification list REST + read state + mobile tabs/UI; `mvn test` and mobile `typecheck` / `lint` / `jest` green.

---

## Story completion status

- **Status:** done  
- **Note:** Code review patches applied (2026-04-06).

### validate-create-story (`checklist.md`)

| Layer | Result | Notes |
|-------|--------|--------|
| Epics / FR22 / UX-DR7 / UX-DR11 | **Pass** | AC trace list API, read/unread, TanStack Query, EmptyState, tab IA. |
| Read/unread without column | **Addressed** | AC1 adds `read_at` — required for “read/unread” in UX-DR7. |
| Tenant isolation | **Pass** | AC2–3 require org + user scoping + tests. |
| Scope | **Pass** | AC7 excludes push/deep link. |

**Critical issues:** None identified at authoring time.
