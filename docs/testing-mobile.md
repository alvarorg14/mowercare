# Mobile app testing (`apps/mobile`)

For **Maestro** UI journeys (sign-in, issues, notifications), see [`testing-e2e.md`](testing-e2e.md).

Commands assume repository root; use `cd apps/mobile` first.

## Commands

| Command | Purpose |
|--------|---------|
| `npm test` | Jest with **`jest-expo`** preset ([`jest.config.js`](../apps/mobile/jest.config.js)). |
| `npm test -- --ci` | CI-friendly run (no watch; non-interactive). |
| `npm run lint` | Expo ESLint. |
| `npm run typecheck` | `tsc --noEmit`. |

Prerequisites: Node 20+ (aligned with CI). No Android/iOS simulator required for unit tests.

## Layout

- Tests live under [`apps/mobile/__tests__/`](../apps/mobile/__tests__/) as `*.test.ts` / `*.test.tsx` (see `testMatch` in Jest config).
- **Colocated mocks:** Prefer `jest.mock('module')` at the top of a test file, or small `__mocks__/` next to the feature. There is **no** global `jest.setup.js` yet; add `setupFilesAfterEnv` in Jest config only when several suites need the same mock.

## Coverage map (lib & targeted components)

| Area | Tests |
|------|--------|
| HTTP + `fetchWithoutAuth`, `ApiProblemError` | [`__tests__/http.test.ts`](../apps/mobile/__tests__/http.test.ts) |
| API base URL (`getApiBaseUrl`) | [`__tests__/config.test.ts`](../apps/mobile/__tests__/config.test.ts) |
| `authenticatedFetchJson` (happy path + unsigned) | [`__tests__/api-authenticated-fetch.test.ts`](../apps/mobile/__tests__/api-authenticated-fetch.test.ts) |
| Auth API wrappers | [`__tests__/auth-api.test.ts`](../apps/mobile/__tests__/auth-api.test.ts) |
| Secure store refresh token | [`__tests__/auth-storage.test.ts`](../apps/mobile/__tests__/auth-storage.test.ts) |
| Device push token REST | [`__tests__/device-token-api.test.ts`](../apps/mobile/__tests__/device-token-api.test.ts) |
| Notification list key + `formatNotificationEventType` | [`__tests__/notification-api.test.ts`](../apps/mobile/__tests__/notification-api.test.ts) |
| TanStack Query client defaults | [`__tests__/queryClient.test.ts`](../apps/mobile/__tests__/queryClient.test.ts) |
| Query keys (employees, profile, assignable) | [`__tests__/query-keys.test.ts`](../apps/mobile/__tests__/query-keys.test.ts) |
| MD3 theme + issue status tokens | [`__tests__/theme.test.ts`](../apps/mobile/__tests__/theme.test.ts) |
| Push / deep link parsing & navigation | [`__tests__/push-navigation.test.ts`](../apps/mobile/__tests__/push-navigation.test.ts) |
| Issue activity copy | [`__tests__/IssueActivityTimeline.test.ts`](../apps/mobile/__tests__/IssueActivityTimeline.test.ts) |
| Issue row status helpers | [`__tests__/issue-row-exports.test.ts`](../apps/mobile/__tests__/issue-row-exports.test.ts) |
| Assignee picker error string | [`__tests__/assignee-picker-error.test.ts`](../apps/mobile/__tests__/assignee-picker-error.test.ts) |
| In-memory session | [`__tests__/session.test.ts`](../apps/mobile/__tests__/session.test.ts) |
| JWT claim helpers | [`__tests__/jwt-org.test.ts`](../apps/mobile/__tests__/jwt-org.test.ts) |
| Zod forms | [`__tests__/schemas.test.ts`](../apps/mobile/__tests__/schemas.test.ts) |
| Issue list query string | [`__tests__/issue-api-query.test.ts`](../apps/mobile/__tests__/issue-api-query.test.ts) |
| Relative time formatting | [`__tests__/relative-time.test.ts`](../apps/mobile/__tests__/relative-time.test.ts) |

## Intentionally lighter coverage (for now)

- **`app/` screens & layouts:** Wiring, Expo Router, and providers — better suited to **Maestro/Detox** (Story 5.3) or focused RTL with `QueryClientProvider` + Paper theme per screen.
- **`lib/auth-context.tsx`:** Context wiring — integration-style test or E2E.
- **`lib/notifications.ts` (`registerDevicePushWithBackend` etc.):** Tightly coupled to `expo-notifications` + `expo-device` — mock-heavy; ship when a regression appears or in a dedicated harness.
- **`lib/api.ts` (401 + refresh retry path):** Partially covered; full branch needs mocks for `refreshApi` + token rotation — optional follow-up.
- **Presentational components without exported helpers:** Covered indirectly or via small exports (see `IssueRow`, `AssigneePicker`).

## Risk hotspots / follow-ups

- **Full-screen RTL** for connected components: add `QueryClientProvider` + Paper theme when you need regression tests on full trees.
- **Coverage thresholds:** Optional; Story 5.6 may add informational coverage reporting.

## Aligning Zod with the API

When adding schema cases, cross-check enum names and field shapes against the running API (`GET /v3/api-docs` from `apps/api`) or Java DTOs under `apps/api`.
