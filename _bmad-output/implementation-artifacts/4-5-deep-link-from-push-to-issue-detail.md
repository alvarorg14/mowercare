# Story 4.5: Deep link from push to issue detail

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As an **employee**,
I want **tapping a push notification to open the relevant issue**,
so that **I can respond quickly (UX-DR11; FR22/FR23)**.

**Implements:** **UX-DR11** (deep link from push); integrates **FR22** (in-app awareness continuity) and **FR23** (push channel).

**Epic:** Epic 4 — Awareness — in-app notifications & push.

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 4.5])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** push payload with issue id **When** user taps notification **Then** app navigates to issue detail when permitted | **Mobile:** Subscribe to **expo-notifications** response events + handle **cold start** via last notification response; read **FCM data** keys already sent by the API (`issueId`, `organizationId`, …); **Expo Router** navigate to existing issue detail route under **`(tabs)/issues/[id]`** when **session org matches** payload org and user is authenticated. |
| **Given** invalid or foreign-org issue id **When** opened **Then** user sees error/empty state with path back to list | **Invalid UUID:** reuse or align with [`issues/[id].tsx`](../../apps/mobile/app/(app)/(tabs)/issues/[id].tsx) validation; ensure **“back”** goes to **Issues list** (not only `router.back()`, which can be wrong on cold deep link). **Foreign org:** if `organizationId` in payload **≠** `getSessionOrganizationId()`, do **not** call issue API for that id — show clear copy + **navigate to Issues tab**. **403/404** from `getIssue`: existing Banner + add explicit **“Back to issues”** if product wants parity with UX spec when opened from push. |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **4.4** | **Depends on** FCM **data** map: `organizationId`, `issueId`, `notificationEventId`, `recipientId` (strings) from [`NotificationPushDispatcher`](../../apps/api/src/main/java/com/mowercare/service/NotificationPushDispatcher.java). **Do not** change payload keys without updating this story and any clients. |
| **4.3** | Notification list row tap already navigates to issue detail — **reuse** the same route and patterns; deep link is an additional entry path. |
| **3.4** | Server policy for **inaccessible** issues (**403**/**404**, Problem Details) is already enforced; mobile must surface honestly per [epics.md](../planning-artifacts/epics.md) Story 3.4. |

---

## Acceptance Criteria

1. **Notification tap → issue detail (happy path)**  
   **Given** the user is **signed in**, session **organization id** matches **`organizationId`** in the push **data** payload, and **`issueId`** is a **valid UUID**  
   **When** the user **taps** the notification (foreground, background, or **cold start** after tap)  
   **Then** the app navigates to **issue detail** for that issue (same screen as list-driven navigation: [`app/(app)/(tabs)/issues/[id].tsx`](../../apps/mobile/app/(app)/(tabs)/issues/[id].tsx))  
   **And** the issue **GET** runs with the **current** org context (existing `getIssue` / TanStack Query behavior).

2. **Payload parsing and guards**  
   **Given** a notification response with **data** from FCM  
   **When** the handler runs  
   **Then** it reads **`issueId`** and **`organizationId`** (required for navigation); optional keys (`notificationEventId`, `recipientId`) may be ignored for navigation in MVP  
   **And** if **`issueId`** is missing or **not** a UUID (reuse the same regex rule as issue detail — [`UUID_RE`](../../apps/mobile/app/(app)/(tabs)/issues/[id].tsx)) **Then** show a **dedicated inline state** (or full-screen message) explaining the link is invalid **And** offer control to go to **`/(app)/(tabs)/issues`** via **`router.replace`** (reliable when there is no back stack).

3. **Organization mismatch (foreign org)**  
   **Given** payload **`organizationId` ≠** `getSessionOrganizationId()` (session loaded)  
   **When** the user taps the notification  
   **Then** the app **does not** navigate to that issue id as if it were in the current org  
   **And** the user sees copy that the notification belongs to **another organization** (or session is out of sync) **And** a primary action to open **`/(app)/(tabs)/issues`** (current org’s list).

4. **Unauthenticated / session restoring**  
   **Given** the user is **not** signed in or **session/org** is still restoring  
   **When** a notification is opened  
   **Then** the app **does not crash** and **does not** navigate to an issue with stale org context  
   **And** behavior is **defined and tested** (MVP acceptable: **drop** deep link after sign-in, or **queue** one pending `{ organizationId, issueId }` until `isAuthenticated` + org id available — pick one approach and document in Dev Notes).

5. **Regression**  
   **Given** existing flows (sign-in, tabs, push registration in [`PushNotificationSetup`](../../apps/mobile/components/PushNotificationSetup.tsx), issue list → detail)  
   **When** this story ships  
   **Then** **no** change to API contracts or push **payload** keys is required unless a bug is found; backend remains **out of scope** unless payload is proven insufficient on a platform.

6. **Tests**  
   **Given** [`jest-expo`](../../apps/mobile/package.json) setup  
   **When** `npm test` runs in `apps/mobile`  
   **Then** **unit tests** cover: parsing payload → route params; org mismatch branch; invalid UUID branch (pure functions or small module — avoid flaky native calls by mocking `expo-notifications`).

---

## Tasks / Subtasks

- [x] **Deep link handler module** (AC: 1–4)
  - [x] Add a focused module (e.g. `lib/push-navigation.ts` or extend [`lib/notifications.ts`](../../apps/mobile/lib/notifications.ts) with clear separation) that: extracts `issueId` / `organizationId` from `NotificationResponse`; compares org to session; returns a **discriminated union** or callback for navigate vs show error.
  - [x] Use **Expo SDK 55** APIs: `Notifications.addNotificationResponseReceivedListener` and `Notifications.getLastNotificationResponseAsync()` for cold start (see [Expo Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/)).

- [x] **Wire into app shell** (AC: 1, 4)
  - [x] Register listener in a **single** place that mounts when the user is in the authenticated tree (e.g. [`app/(app)/_layout.tsx`](../../apps/mobile/app/(app)/_layout.tsx) or `PushNotificationSetup`) so duplicate navigations are avoided.
  - [x] After session restore, call `getLastNotificationResponseAsync()` **once** where appropriate to handle tap‑to‑open from quit state.
  - [x] **Dedupe cold-start handling:** `getLastNotificationResponseAsync()` can return the **same** user action across launches or resumes until the OS clears it — track a **one-shot** guard (e.g. processed notification id + timestamp, or “consumed last response” ref for the session) so the app does **not** re-push issue detail on every foreground.

- [x] **Navigation** (AC: 1–3)
  - [x] Navigate with `expo-router` to `/(app)/(tabs)/issues/[issueId]` (or equivalent **href** string the router accepts) using **`router.push`** or **`replace`** — choose **replace** if opening from cold start should not leave a “dead” stack under the issue.

- [x] **UX: error / mismatch screens** (AC: 2–3)
  - [x] Align copy with [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md) (invalid id → empty/error + return to list).

- [x] **Optional hardening** (AC: 3, 5)
  - [x] If issue detail **Banner** on fetch error is insufficient for “back to list” when stack is empty, add **`router.replace('/(app)/(tabs)/issues')`** action next to Retry on [`[id].tsx`](../../apps/mobile/app/(app)/(tabs)/issues/[id].tsx) — only if needed for push entry UX.

- [x] **Tests** (AC: 6)
  - [x] Jest tests with mocked `expo-notifications` and session org.

### Review Findings

- [x] [Review][Patch] Extend AC6 unit tests for remaining push-navigation branches [`apps/mobile/__tests__/push-navigation.test.ts`] — Add coverage for: authenticated user + invalid payload → `router.replace('/(app)/push-link-error?reason=invalid')`; `flushPendingPushDeepLink` after a logged-out tap queued `{ kind: 'invalid' }` → same error route; after queueing a valid `{ organizationId, issueId }` then flushing with a different `sessionOrgId` → `reason=wrong_org`. Current tests cover happy path, listener vs cold_start, org mismatch on direct open, session-restore no-op, cold-start dedupe, and flush after sign-in when org matches.

---

## Dev Notes

### Current codebase anchors

| Area | Location |
|------|----------|
| FCM **data** payload | [`NotificationPushDispatcher.dispatchForNewRecipient`](../../apps/api/src/main/java/com/mowercare/service/NotificationPushDispatcher.java) — keys: `organizationId`, `issueId`, `notificationEventId`, `recipientId` |
| Push registration / Banner | [`lib/notifications.ts`](../../apps/mobile/lib/notifications.ts), [`PushNotificationSetup.tsx`](../../apps/mobile/components/PushNotificationSetup.tsx) |
| Session org | [`getSessionOrganizationId`](../../apps/mobile/lib/auth/session.ts) |
| Issue detail + UUID + errors | [`app/(app)/(tabs)/issues/[id].tsx`](../../apps/mobile/app/(app)/(tabs)/issues/[id].tsx) |
| App scheme | [`app.config.ts`](../../apps/mobile/app.config.ts) — `scheme: 'mowercare'` (useful if extending to URL-based linking later; **not** required for FCM data-only MVP) |

### Architecture compliance

| Topic | Source |
|-------|--------|
| Expo Router shell, feature-first mobile | [`architecture.md`](../planning-artifacts/architecture.md) |
| Push boundary: keep registration in `lib/notifications.ts`; **navigation from push** can live in sibling module or same file with **clear** separation | Same § Mobile boundaries |
| REST + Problem Details for issue fetch | Architecture + existing `ApiProblemError` usage |

### UX compliance

| Topic | Source |
|-------|--------|
| Deep links from push → issue detail; invalid id → error + back to list | [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md) (~lines 577, 368) |
| Optional push deep link in technician journey | Same doc § journeys |

### Library / framework notes

| Layer | Notes |
|-------|--------|
| Mobile | **Expo 55** / **expo-notifications ~55.0.x** — response listener + last response API; **expo-router ~55** for navigation |
| API | **No** new endpoints expected; verify payload arrives under `response.notification.request.content.data` (and any platform quirks in Expo docs). |

### File structure (expected touchpoints)

| Path | Purpose |
|------|---------|
| `apps/mobile/lib/push-navigation.ts` (or extend `notifications.ts`) | Parse payload, org guard, navigate |
| `apps/mobile/app/(app)/_layout.tsx` and/or `PushNotificationSetup.tsx` | Listener + cold-start hookup |
| `apps/mobile/app/(app)/(tabs)/issues/[id].tsx` | Optional “Back to issues” enhancement |
| `apps/mobile/**/__tests__/**` or colocated `*.test.ts` | Unit tests |

### Previous story intelligence (4.4)

- **Payload contract** was explicitly designed for this story — **do not** rename data keys without coordinated API + mobile change.
- **Org id strings:** [`getSessionOrganizationId`](../../apps/mobile/lib/auth/session.ts) and FCM `organizationId` are both string UUIDs; compare **case-insensitively** or normalize to lowercase before equality to avoid rare mismatch.
- Push setup is **best-effort** and **non-blocking**; deep-link handling must also **never** throw uncaught from listeners.
- **Token** lifecycle and **revoke** on sign-out are unrelated but **re-test** that signing out does not leave a stale listener that navigates with old org.

### Git intelligence (recent commits)

- **`feat(4-4): device push tokens, FCM dispatch...`** — FCM path and mobile registration landed; this story is **mobile-only** navigation on top.
- **`feat(notifications): Story 4.3...`** — Tabs and notification list; reuse **issue** route for consistency.

### Latest tech information

- [Expo Notifications — handling notification responses](https://docs.expo.dev/versions/latest/sdk/notifications/) (SDK 55): `addNotificationResponseReceivedListener`, `getLastNotificationResponseAsync`.
- [Expo Router — imperative navigation](https://docs.expo.dev/router/reference/router/) for `push` / `replace`.

### Project context reference

- No **`project-context.md`** in repo; rely on architecture + prior story files + linked code.

---

## Dev Agent Record

### Agent Model Used

Composer (GPT-5.2) — dev-story workflow

### Debug Log References

### Completion Notes List

- **AC4:** Implemented **single pending** queue in `lib/push-navigation.ts` (`pendingPushDeepLink`); flushed in `PushDeepLinkBootstrap` when `isAuthenticated && !isRestoringSession`. Cleared on **sign-out** via `clearPendingPushDeepLink()` in `auth-context.tsx`.
- **Cold start vs listener:** `cold_start` uses `router.replace` to issue detail; `listener` uses `router.push`. Module-level `lastColdStartDedupeKey` prevents repeated cold-start handling; bootstrap refs suppress duplicate listener delivery right after `getLastNotificationResponseAsync`.
- **Issue UUID:** `ISSUE_UUID_RE` shared from `lib/push-navigation.ts` in issue detail screen.
- **Tests:** `apps/mobile/__tests__/push-navigation.test.ts` — parse, org equality, navigate branches, flush after queue, duplicate cold_start.
- **Commands:** `npm test`, `npm run typecheck`, `npm run lint` (mobile); `mvn test` (api).

### File List

- `apps/mobile/lib/push-navigation.ts`
- `apps/mobile/components/PushDeepLinkBootstrap.tsx`
- `apps/mobile/app/(app)/push-link-error.tsx`
- `apps/mobile/app/_layout.tsx`
- `apps/mobile/lib/auth-context.tsx`
- `apps/mobile/app/(app)/(tabs)/issues/[id].tsx`
- `apps/mobile/__tests__/push-navigation.test.ts`

### Change Log

- **2026-04-06:** Story 4.5 — push tap → issue detail; pending queue for pre-auth opens; `push-link-error` for invalid payload / wrong org; issue detail error Banner “Back to issues”; sprint status → review.
- **2026-04-06:** Code review — AC6 tests for invalid authed, flush invalid, flush wrong_org; story marked done.

---

## Story completion status

- **Status:** done  
- **Note:** Code review complete; AC6 test gaps closed.

### validate-create-story — validation report

**Date:** 2026-04-06  
**Validator:** checklist.md methodology (re-analysis vs epics, architecture, code anchors, 4.3/4.4 stories).

**Verdict:** **Pass — ready for dev-story** (one gap was patched into Tasks/Dev Notes above: cold-start / last-response deduplication).

#### Epics & UX

| Check | Result |
|-------|--------|
| [epics.md](../planning-artifacts/epics.md) Story 4.5 (tap → detail; invalid/foreign → error + path back) | **Aligned** |
| UX spec (deep link, invalid id → list) | **Referenced** |
| Cross-story 4.4 payload keys | **Matches** `NotificationPushDispatcher` |

#### Code anchors

| Check | Result |
|-------|--------|
| Issue detail route + `UUID_RE` | **Valid** paths |
| Session org module | **Correct** (`organizationId` string) |
| Jest present (`__tests__/`, jest-expo) | **AC6 feasible** |

#### Disaster-prevention gaps addressed in this validation

| Severity | Topic | Resolution |
|----------|--------|------------|
| **Critical** | `getLastNotificationResponseAsync()` / stale “last response” causing **repeat navigation** | **Added** subtask + Dev Note (one-shot / session guard). |
| **Enhancement** | AC4 still allows **drop vs queue** pending deep link | **Leave to implementer** but must **document chosen behavior** in Dev Agent Record when implementing; prefer **queue one pending** `{ organizationId, issueId }` until `isAuthenticated` + org for better UX. |
| **Enhancement** | Optional: **mark notification read** when opening from push (FR22 continuity) | **Out of scope** for 4.5 per epics; do not block story. |
| **Nice-to-have** | Instrument **minimal** logging in dev for payload shape on real devices | Optional debug aid; not required by AC. |

#### Regression / scope

| Check | Result |
|-------|--------|
| Backend scope | **Appropriately out of scope** unless payload path broken on a platform |
| Push registration | **No conflict** if listener is non-throwing and mounted once |

**Blocking issues:** None.
