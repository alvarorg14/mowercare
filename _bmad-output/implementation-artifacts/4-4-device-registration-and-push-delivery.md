# Story 4.4: Device registration and push delivery

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As an **employee**,
I want **push notifications on my device when enabled**,
so that **I get timely awareness in the field (FR23; NFR-R2; UX-DR16)**.

**Implements:** **FR23**; **NFR-R2**; **UX-DR16**; Additional: FCM/APNs via **expo-notifications**, org-scoped **device token** storage and **server-side push** dispatch.

**Epic:** Epic 4 — Awareness — in-app notifications & push.

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 4.4])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** user grants permission **When** app registers device token with API **Then** token is stored per user with org scope; invalid tokens are handled without crashing | **Backend:** Liquibase table + JPA entity; **REST** upsert (and optional revoke) under `/api/v1/organizations/{organizationId}/...`; **tenant + employee** guards matching [`NotificationInboxController`](../../apps/api/src/main/java/com/mowercare/controller/NotificationInboxController.java). **Mobile:** **expo-notifications** permission + Expo push token; POST token after sign-in / org context known; handle token refresh and registration errors without crashing. |
| **Given** notification dispatch **When** push sends **Then** failures are logged per observability rules; user sees Banner or settings hint if push disabled | **Backend:** After fan-out creates [`NotificationRecipient`](../../apps/api/src/main/java/com/mowercare/model/NotificationRecipient.java) rows, invoke **FCM** (Firebase Cloud Messaging) for eligible devices; **structured SLF4J** logs on send failure (no tokens/PII in logs); do not fail the issue transaction solely because push failed (NFR-R2). **Mobile:** **UX-DR16** — consolidated module [`lib/notifications.ts`](../../apps/mobile/lib/notifications.ts) (create); **Banner** (or inline hint) when permission denied or token registration fails; Settings entry point optional if it matches existing Settings patterns. |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **4.1–4.3** | **Depends on** `notification_events`, `notification_recipients`, inbox REST — **do not** regress inbox or fan-out rules. |
| **4.5** | **Out of scope:** tap handler / Expo Router deep link to issue detail — but **FCM `data` payload** should include **`issueId`** (and **`organizationId`**) so **4.5** can consume the same shape without redesign. |

---

## Acceptance Criteria

1. **Schema — device tokens (org + user scoped)**  
   **Given** no device token table exists today  
   **When** Liquibase applies  
   **Then** a new table (e.g. **`device_push_tokens`**) stores at minimum: **`id`** (UUID PK), **`organization_id`** (FK tenant), **`user_id`** (FK), **`token`** (text; Expo/FCM token string), **`platform`** (enum or short string: `ios` \| `android` \| `unknown`), **`created_at`**, **`updated_at`**  
   **And** unique constraint appropriate for MVP: e.g. **one row per (`organization_id`, `user_id`, `token`)** or **upsert by (`organization_id`, `user_id`)** with latest token — **document the chosen rule** in OpenAPI + Dev Notes  
   **And** changelog file registered in [`db.changelog-master.yaml`](../../apps/api/src/main/resources/db/changelog/db.changelog-master.yaml).

2. **API — register or replace token (employee, tenant-safe)**  
   **Given** an authenticated **employee** JWT (`ADMIN` or `TECHNICIAN`)  
   **When** client sends **PUT** or **POST** (pick one; align with existing idempotent patterns) to register the device token for the path org  
   **Then** [`TenantPathAuthorization.requireJwtOrganizationMatchesPath`](../../apps/api/src/main/java/com/mowercare/security/TenantPathAuthorization.java) + [`RoleAuthorization.requireEmployee`](../../apps/api/src/main/java/com/mowercare/security/RoleAuthorization.java) apply  
   **And** body includes token string + platform (if available)  
   **And** response is **200 OK** with a stable JSON body (`id` echo) — idempotent **PUT** upsert (documented in OpenAPI; preferred over 201/204 for this route)  
   **And** invalid/expired tokens from client are rejected with **400** Problem Details (RFC 7807) — not 500.

3. **API — revoke token (optional but recommended)**  
   **Given** sign-out or “disable push” flows  
   **When** **DELETE** (or register empty) is called for the current token  
   **Then** row is removed or marked inactive so pushes are not sent to stale tokens — **document** behavior.

4. **Push dispatch — hook after fan-out**  
   **Given** [`NotificationRecipientFanoutService.recordRecipientsFor`](../../apps/api/src/main/java/com/mowercare/service/NotificationRecipientFanoutService.java) persists recipients  
   **When** a new `NotificationRecipient` is created for user U in org O  
   **Then** the system attempts **FCM** multicast/send to **all valid tokens** for (O, U)  
   **And** notification **data** payload includes at least **`organizationId`**, **`issueId`**, **`notificationEventId`** or **`recipientId`** (choose minimal stable set for **4.5**) — **string values** in FCM data map  
   **And** **failure to send push** (network, invalid token) is **logged** at **WARN** with correlation context if available; **invalid token** responses should **deactivate or delete** that token row (NFR-R2) — implementation removes the row when FCM reports **UNREGISTERED** (`MessagingErrorCode`) or **NOT_FOUND** (`ErrorCode` on `FirebaseMessagingException`)  
   **And** **issue create/update transaction** is **not rolled back** if push fails (push is best-effort in MVP).

5. **Server configuration & secrets**  
   **Given** FCM requires a service account  
   **When** the API starts in dev/staging/prod  
   **Then** Firebase Admin is initialized from **environment** (e.g. `GOOGLE_APPLICATION_CREDENTIALS` or JSON path documented in [`.env.example`](../../.env.example) / README) — **no** secrets committed  
   **And** if credentials are **missing** in dev, push dispatch is **no-op** with **clear INFO/WARN** log (developers can still run tests without Firebase).

6. **Tests (API)**  
   **Given** [`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java) patterns  
   **When** `mvn test` runs  
   **Then** integration tests cover: token registration **scoped to org + user**; **403** wrong org path; **mock or stub** FCM sender so tests do not call Google (e.g. `@MockBean` on a `PushNotificationSender` interface).

7. **Mobile — expo-notifications + consolidated module (UX-DR16)**  
   **Given** [`apps/mobile/package.json`](../../apps/mobile/package.json) does not yet include **expo-notifications**  
   **When** dependencies are added  
   **Then** use a version **compatible with Expo SDK ~55** (same major as `expo` in `package.json`)  
   **And** add **`lib/notifications.ts`**: request permissions, get Expo push token, register with API when session + org id available, listen for token updates  
   **And** wire into app shell (e.g. authenticated layout or a small provider) so registration runs **after** login — **not** blocking render on failure  
   **And** **Banner** (React Native Paper [`Banner`](https://callstack.github.io/react-native-paper/docs/components/Banner/)) or equivalent when push disabled or registration fails — dismissible; copy explains in-app feed remains available.

8. **Mobile — app config**  
   **Given** push requires native capabilities  
   **When** `expo prebuild` / EAS build is used  
   **Then** [`app.config.ts`](../../apps/mobile/app.config.ts) includes required **expo-notifications** / Expo config plugins per current Expo 55 docs (iOS entitlement + Android channel as needed) — **document** any **EAS** / Firebase setup steps briefly in Dev Notes (no new markdown file unless repo already expects it).

9. **Scope**  
   **Given** this is **4.4**  
   **When** complete  
   **Then** **no** Expo Router handler for notification tap navigation (**4.5**); **no** WebSocket inbox.

---

## Tasks / Subtasks

- [x] **Liquibase + entity + repository** (AC: 1)
  - [x] New changelog `0013-...` (or next free number); entity under `com.mowercare.model` or existing package style.

- [x] **Device token service + controller** (AC: 2–3, 5–6)
  - [x] `DevicePushTokenController` under `com.mowercare.controller`, OpenAPI tags consistent with Notifications.
  - [x] `PushNotificationSender` interface + `FcmPushNotificationSender` implementation (Firebase Admin SDK dependency in [`pom.xml`](../../apps/api/pom.xml)).

- [x] **Dispatch hook** (AC: 4)
  - [x] From fan-out path or `NotificationEventRecorder` after recipients saved — inject sender; iterate recipients’ users’ tokens.

- [x] **Tests** (AC: 6)
  - [x] New `*IT` class mirroring [`NotificationInboxIT`](../../apps/api/src/test/java/com/mowercare/controller/NotificationInboxIT.java) patterns.

- [x] **Mobile** (AC: 7–8)
  - [x] Add `expo-notifications`; implement `lib/notifications.ts`; integrate with auth/org context; Banner UX.

### Review Findings

- [x] [Review][Decision] **PUT device token 200 + JSON (1C)** — Chosen contract: **200 OK** + `DevicePushTokenResponse`; OpenAPI description + AC2 updated; no API behavior change.
- [x] [Review][Decision] **FCM token row deletion (2B)** — Drop row on `MessagingErrorCode.UNREGISTERED` **or** parent `ErrorCode.NOT_FOUND` on `FirebaseMessagingException` (FCM Admin 9.4.3 has no `MessagingErrorCode.NOT_FOUND`).

**Batch-applied (option 0, 2026-04-06):** Expo `addPushTokenListener` + `ensureDevicePushTokenListener` from shell; `TenantPathAuthorization.requireSubjectAsUuid` for device token routes (and `TenantScopeController` DRY) → **401 AUTH_INVALID_TOKEN** for bad `sub`, consistent with other org-scoped endpoints; `PushNotificationSender` correlation map + WARN fields; revoke clears cache only after successful DELETE; `@Import(NoOpPushNotificationSenderConfig)` + `@Primary` no-op bean for ITs; sprint `last_updated` set to `2026-04-06T23:59:00Z`.

- [x] [Review][Patch] **Expo token refresh listener (AC7)** — `ensureDevicePushTokenListener` + `PushNotificationSetup` [`apps/mobile/lib/notifications.ts`, `apps/mobile/components/PushNotificationSetup.tsx`]
- [x] [Review][Patch] **JWT `sub` parsing** — `TenantPathAuthorization.requireSubjectAsUuid` [`DevicePushTokenController.java`, `TenantScopeController.java`, `TenantPathAuthorization.java`]
- [x] [Review][Patch] **FCM WARN correlation (AC4)** — `recipientId` / `notificationEventId` on sender + dispatcher [`PushNotificationSender.java`, `FcmPushNotificationSender.java`, `NotificationPushDispatcher.java`]
- [x] [Review][Patch] **`sprint-status.yaml` `last_updated`** — restored end-of-day stamp [`_bmad-output/implementation-artifacts/sprint-status.yaml`]
- [x] [Review][Patch] **Revoke cache after successful DELETE** [`apps/mobile/lib/notifications.ts`]
- [x] [Review][Patch] **IT no-op `PushNotificationSender`** [`NoOpPushNotificationSenderConfig.java`, `DevicePushTokenIT.java`]

- [x] [Review][Defer] **EAS / store builds: confirm push entitlements and Android channels (AC8)** — Plugin-only `app.config.ts` change may be enough for dev client; validate on first **EAS** iOS/Android release that entitlements and channels match Expo 55 docs. — deferred, pre-existing checklist item for release hardening.
- [x] [Review][Defer] **Push runs inside same DB transaction as recipient insert** — `NotificationRecipientFanoutService` calls `dispatchForNewRecipient` before commit; slow or flaky FCM prolongs the transaction. Acceptable for MVP per best-effort push; revisit async/after-commit if latency or timeouts appear. — deferred, pre-existing architecture/performance note.

---

## Dev Notes

### Current codebase anchors

| Area | Location |
|------|----------|
| Fan-out entry | [`NotificationRecipientFanoutService`](../../apps/api/src/main/java/com/mowercare/service/NotificationRecipientFanoutService.java), [`NotificationEventRecorder`](../../apps/api/src/main/java/com/mowercare/service/NotificationEventRecorder.java) |
| Inbox API patterns | [`NotificationInboxController`](../../apps/api/src/main/java/com/mowercare/controller/NotificationInboxController.java) |
| Mobile HTTP | [`authenticatedFetchJson`](../../apps/mobile/lib/api.ts), [`ApiProblemError`](../../apps/mobile/lib/http.ts) |
| Auth / org | Where JWT + org id are available (e.g. auth context) — register token when both exist |

### Architecture compliance

| Topic | Source |
|-------|--------|
| REST + JSON, `/api/v1`, camelCase, Problem Details | [`architecture.md`](../planning-artifacts/architecture.md) |
| Push: FCM/APNs via expo-notifications; tokens per user **org scope** | Same + epics Additional Requirements |
| Push boundary: `lib/notifications.ts`; tokens POST to API | [`architecture.md`](../planning-artifacts/architecture.md) § Mobile boundaries |
| Observability: structured logs; no secrets/PII in logs | [`architecture.md`](../planning-artifacts/architecture.md) |

### UX compliance (UX-DR16)

| Topic | Source |
|-------|--------|
| Permission + token flows in **one** module | [`epics.md`](../planning-artifacts/epics.md) UX-DR16 |
| Banner when push disabled or token invalid | Same; align with Warning/Banner patterns in [`ux-design-specification.md`](../planning-artifacts/ux-design-specification.md) |

### Library / framework notes

| Layer | Notes |
|-------|--------|
| API | Spring Boot 4.x / Java 25 per [`pom.xml`](../../apps/api/pom.xml); add **Firebase Admin** BOM-compatible artifact for FCM HTTP v1. |
| Mobile | **expo-notifications** must match **Expo SDK 55**; use `expo install expo-notifications` to resolve compatible version. |

### File structure (expected touchpoints)

| Path | Purpose |
|------|---------|
| `apps/api/src/main/resources/db/changelog/changes/0013-*.yaml` | Device token table |
| `apps/api/.../controller/*Device*Token*.java` or extend notification area | REST |
| `apps/api/.../service/*Push*.java` | FCM send + logging |
| `apps/mobile/lib/notifications.ts` | UX-DR16 consolidated behavior |
| `apps/mobile/app.config.ts` | Plugins / notification config |

### Previous story intelligence (4.3)

- **Notification inbox** and **tabs** are done; extend **Settings** or shell only as needed for “push disabled” messaging — avoid duplicating [`notification-api.ts`](../../apps/mobile/lib/notification-api.ts) patterns; add a small `device-token-api.ts` or extend `lib/api` if preferred.
- **List pagination** deferred (first page only) — unrelated to push.
- **Tenant tests**: mirror **403** wrong-org tests from [`NotificationInboxIT`](../../apps/api/src/test/java/com/mowercare/controller/NotificationInboxIT.java).

### Git intelligence (recent commits)

- **4.3** established `NotificationInboxController` paths and employee-only access — **reuse** the same security helpers for device routes.
- **4.2** fan-out is synchronous inside issue transaction — **push must not** break issue persistence; use try/catch or error handler around FCM calls.

### Latest tech information

- **Expo SDK 55** + **expo-notifications**: follow [Expo Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/) for **projectId**, **Expo push token** acquisition, and **Android notification channel** defaults.
- **FCM HTTP v1** via **Firebase Admin SDK** for Java is the typical server match for Expo’s FCM tokens.

### Project context reference

- No **`project-context.md`** in repo; rely on architecture + this story + linked code.

---

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.1) — dev-story workflow

### Debug Log References

### Completion Notes List

- **Liquibase `0013`:** `device_push_tokens` with `UNIQUE (organization_id, user_id, token)`; FK CASCADE to org and user.
- **API:** `PUT` / `DELETE` `/api/v1/organizations/{organizationId}/device-push-tokens` with tenant + employee guards; validation via `DevicePushTokenPutRequest` / `DevicePushTokenDeleteRequest`.
- **FCM:** `firebase-admin` 9.4.3; `FirebaseApp` bean when `mowercare.firebase.enabled=true` and credentials path set; otherwise send is no-op (INFO at startup). `FcmPushNotificationSender` drops DB row on `UNREGISTERED` or `ErrorCode.NOT_FOUND`.
- **Dispatch:** `NotificationPushDispatcher` after each saved recipient in `NotificationRecipientFanoutService`; data map: `organizationId`, `issueId`, `notificationEventId`, `recipientId` (strings); failures never roll back issue transaction.
- **Mobile:** `expo-notifications` + `expo-device`; native token via `getDevicePushTokenAsync` for Firebase server; `lib/notifications.ts` + `lib/device-token-api.ts`; `PushNotificationSetup` banner in `(app)/_layout`; `signOut` revokes token best-effort.
- **Tests:** `DevicePushTokenIT` with `@Import(NoOpPushNotificationSenderConfig)` (`@Primary` no-op `PushNotificationSender`). `mvn test`, mobile `typecheck` / `lint` / `jest` green.

### File List

- `apps/api/pom.xml`
- `apps/api/src/main/resources/application.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/resources/db/changelog/changes/0013-device-push-tokens.yaml`
- `apps/api/src/main/java/com/mowercare/ApiApplication.java`
- `apps/api/src/main/java/com/mowercare/config/FirebaseConfiguration.java`
- `apps/api/src/main/java/com/mowercare/config/FirebaseProperties.java`
- `apps/api/src/main/java/com/mowercare/controller/DevicePushTokenController.java`
- `apps/api/src/main/java/com/mowercare/model/DevicePushPlatform.java`
- `apps/api/src/main/java/com/mowercare/model/DevicePushToken.java`
- `apps/api/src/main/java/com/mowercare/model/request/DevicePushTokenPutRequest.java`
- `apps/api/src/main/java/com/mowercare/model/request/DevicePushTokenDeleteRequest.java`
- `apps/api/src/main/java/com/mowercare/model/response/DevicePushTokenResponse.java`
- `apps/api/src/main/java/com/mowercare/repository/DevicePushTokenRepository.java`
- `apps/api/src/main/java/com/mowercare/service/DevicePushTokenService.java`
- `apps/api/src/main/java/com/mowercare/service/FcmPushNotificationSender.java`
- `apps/api/src/main/java/com/mowercare/service/NotificationPushDispatcher.java`
- `apps/api/src/main/java/com/mowercare/service/NotificationRecipientFanoutService.java`
- `apps/api/src/main/java/com/mowercare/service/PushNotificationSender.java`
- `apps/api/src/test/java/com/mowercare/controller/DevicePushTokenIT.java`
- `apps/api/src/test/java/com/mowercare/testsupport/NoOpPushNotificationSenderConfig.java`
- `apps/mobile/package.json`
- `apps/mobile/package-lock.json`
- `apps/mobile/app.config.ts`
- `apps/mobile/app/(app)/_layout.tsx`
- `apps/mobile/components/PushNotificationSetup.tsx`
- `apps/mobile/lib/auth-context.tsx`
- `apps/mobile/lib/device-token-api.ts`
- `apps/mobile/lib/notifications.ts`
- `.env.example`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- **2026-04-06:** Story 4.4 — device push tokens, FCM dispatch after fan-out, mobile registration + banner + sign-out revoke.
- **2026-04-06:** Code review — batch patches; decisions **1C** (document 200 PUT) and **2B** (`UNREGISTERED` + `NOT_FOUND` token purge).

---

## Story completion status

- **Status:** done  
- **Note:** Review decisions 1C / 2B applied; open items are deferred checklist only (EAS entitlements, async push).

### validate-create-story (`checklist.md`)

| Layer | Result | Notes |
|-------|--------|-------|
| Epics FR23 / NFR-R2 / UX-DR16 | **Pass** | AC cover token API, FCM dispatch, failure logging, Banner. |
| Cross-story 4.5 | **Pass** | Payload fields documented; navigation explicitly out of scope. |
| Regression | **Pass** | AC4 requires best-effort push without rolling back issue TX. |

**Critical issues:** None identified at authoring time.
