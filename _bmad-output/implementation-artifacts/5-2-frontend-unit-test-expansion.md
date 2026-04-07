# Story 5.2: Frontend unit test expansion

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As a **developer**,
I want **more unit and component tests on the mobile app (hooks, navigation helpers, form validation, critical UI)**,
so that **UI refactors stay safe and regressions are caught early (NFR-P2 honest states)**.

**Implements:** NFR-P2 (predictable UI behavior); UX-DR6, UX-DR9 (error and mutation feedback patterns tested where applicable).

**Epic:** Epic 5 — Post-MVP quality, maintainability & UX polish.

**Backlog note:** [`epics.md`](../planning-artifacts/epics.md) Story 5.2 lists **high-value** units at a summary level; **this story file** is the implementation source of truth for concrete files, CI wiring, and mock patterns.

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 5.2])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** mobile package **When** standard test command runs in CI **Then** tests for auth/session, issue behaviors (pure/mocked), notification/deep-link utilities, Zod aligned with API | Add/extend Jest tests under `apps/mobile`; **wire `npm test` into** [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) **mobile job** (currently lint + typecheck only). |
| **Given** new tests **When** local + CI **Then** no manual device interaction; stable mocks documented in one place | Introduce **`jest.setup.js`** (or equivalent) + **`setupFilesAfterEnv`** in [`jest.config.js`](../../apps/mobile/jest.config.js) when shared mocks are needed; document in **`docs/testing-mobile.md`**. |
| **Given** coverage **When** reviewed **Then** hotspots covered or listed as follow-ups | Maintain a short **risk hotspot** subsection in `docs/testing-mobile.md` (or inline completion notes); optional coverage artifact is **non-blocking** unless team enables thresholds later (Story 5.6). |

### Cross-epic boundaries

| Source | Relationship |
|--------|----------------|
| [Story 5.1](./5-1-backend-unit-and-integration-test-expansion.md) | Backend testing patterns live in `docs/testing-backend.md`; **mirror** command-oriented style for mobile in `docs/testing-mobile.md`. |
| Story 5.3 (E2E — backlog in [`sprint-status.yaml`](./sprint-status.yaml)) | E2E is **out of scope** here — only **unit/component** tests. |
| Story 5.6 (CI docs — backlog) | May later consolidate `docs/testing.md`; for 5.2, a **dedicated mobile testing doc** is enough. |

---

## Acceptance Criteria

1. **High-value unit coverage (mobile `lib/` + pure UI helpers)**  
   **Given** `apps/mobile`  
   **When** `npm test` runs (Jest + [`jest-expo`](../../apps/mobile/package.json) preset)  
   **Then** automated tests cover **meaningful behavior** (not empty assertions) for at least these **categories**, using **existing** tests where they already satisfy the bar:  
   - **Auth / session in memory:** [`lib/auth/session.ts`](../../apps/mobile/lib/auth/session.ts) — `setSession`, getters, token type default.  
   - **JWT claim helpers (no verify):** [`lib/jwt-org.ts`](../../apps/mobile/lib/jwt-org.ts) — valid payload shapes, malformed token, missing claims, role enum.  
   - **Zod schemas aligned with API:** [`lib/auth/login-schema.ts`](../../apps/mobile/lib/auth/login-schema.ts), [`lib/issue-create-schema.ts`](../../apps/mobile/lib/issue-create-schema.ts), and other **`lib/*-schema.ts`** — valid/invalid cases (UUID, lengths, enums). Cross-check enum names and field shapes against the live API contract (e.g. `GET /v3/api-docs` from a running `apps/api`, or DTOs in `apps/api`) when adding cases.  
   - **Issue list query behavior (pure, epics “issue list … easily mocked”):** [`lib/issue-api.ts`](../../apps/mobile/lib/issue-api.ts) — `defaultIssueListParams`, `buildIssueListQueryString` (query string shape: `scope`, repeated `status`/`priority`, `sort`, `direction`).  
   - **Pure formatting / copy:** [`lib/relative-time.ts`](../../apps/mobile/lib/relative-time.ts) — invalid ISO → `—`; use **`jest.useFakeTimers()`** where time-relative assertions need determinism.  
   - **Deep link / push (already strong):** extend [`__tests__/push-navigation.test.ts`](../../apps/mobile/__tests__/push-navigation.test.ts) **only if** new branches or regressions need locking; otherwise **reference in docs** as existing coverage.  
   - **Issue activity copy:** [`__tests__/IssueActivityTimeline.test.ts`](../../apps/mobile/__tests__/IssueActivityTimeline.test.ts) — extend if new `summarizeChangeEvent` branches appear.  
   **And** prioritize **pure functions** and **schemas** first; **React components** only where high value (use **`@testing-library/react-native`** already in devDependencies).  
   **And** do **not** duplicate E2E concerns — network + full navigation belong in 5.3.

2. **Stable mocks and no interactive device**  
   **Given** tests import Expo modules, TanStack Query, or router  
   **When** the suite runs in CI (headless)  
   **Then** **jest.mock** / manual mocks live in **one discoverable place** (e.g. `jest.setup.js` + `__mocks__/` or colocated `jest.mock` with a **single doc pointer** in `docs/testing-mobile.md`).  
   **And** avoid importing **`expo-notifications`** in tests when it causes side effects — follow the **pattern** in [`push-navigation.test.ts`](../../apps/mobile/__tests__/push-navigation.test.ts) (constants inlined, test the navigation module API).

3. **CI contract**  
   **Given** GitHub Actions  
   **When** a PR runs  
   **Then** the **mobile** job runs `npm test` (or `npm run test`) in [`apps/mobile`](../../apps/mobile) after `npm ci`, **and** the job stays green.  
   **Note:** As of story creation, the mobile job runs **lint + typecheck only** — **this story adds** the test step to satisfy epics “when … runs in CI”.

4. **Documentation**  
   **Given** a new contributor  
   **When** they read **`docs/testing-mobile.md`** (create if absent)  
   **Then** they see: how to run tests locally, Jest preset (`jest-expo`), pointer to shared mocks, and a **short “risk hotspots / follow-ups”** list for anything intentionally deferred.

5. **UX-DR6 / UX-DR9 alignment (where testable without E2E)**  
   **Given** form validation and error feedback are implemented via Zod + React Hook Form  
   **When** schema tests exist  
   **Then** invalid inputs used in tests **match** user-visible validation messages where those messages come from Zod (spot-check key forms: login, issue create). Full screen-level mutation/error UX can remain partially covered until 5.3/5.5 as long as gaps are **listed** in `docs/testing-mobile.md`.

---

## Tasks / Subtasks

- [x] **Inventory + doc shell** (AC: 1, 4)
  - [x] Skim `apps/mobile/lib/**/*.ts` and pure components; list **logic-heavy** vs **thin** files; add `docs/testing-mobile.md` with commands and follow-up list.
- [x] **Unit tests — lib & schemas** (AC: 1, 2, 5)
  - [x] Add `*.test.ts` under `__tests__/` or colocated `lib/**/*.test.ts` per project preference (existing pattern: `__tests__/*.test.ts`).
  - [x] Cover `session.ts`, `jwt-org.ts`, `login-schema`, `issue-create-schema`, **`buildIssueListQueryString` / `defaultIssueListParams`** (`issue-api.ts`), `relative-time` (fake timers), and at least **one** additional schema (`organization-users-invite-schema`, `org-profile-schema`, etc.) if gaps remain.
- [x] **Optional component tests** (AC: 1)
  - [x] Add 1–2 **high-value** component tests (e.g. presentational pieces with `summarizeChangeEvent`-style logic) only if quick; defer heavy screen tests if they require large provider trees — document deferral.
- [x] **Jest setup** (AC: 2)
  - [x] Add `setupFilesAfterEnv` only if shared mocks are needed; otherwise document “colocated mocks” policy.
- [x] **CI** (AC: 3)
  - [x] Update [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) mobile job: run `npm test -- --ci` or `npm test` after install (match Jest CI conventions; `--ci` is preferred for deterministic exit codes).
- [x] **Verification** (AC: 3)
  - [x] Run `npm test` locally in `apps/mobile` before PR.

---

## Dev Notes

### Current codebase anchors

| Area | Location |
|------|----------|
| Jest config | [`jest.config.js`](../../apps/mobile/jest.config.js) — `jest-expo`, `__tests__/**/*.[jt]s?(x)` |
| Existing tests | [`__tests__/push-navigation.test.ts`](../../apps/mobile/__tests__/push-navigation.test.ts), [`__tests__/IssueActivityTimeline.test.ts`](../../apps/mobile/__tests__/IssueActivityTimeline.test.ts) |
| Auth + API helpers | [`lib/auth/`](../../apps/mobile/lib/auth/), [`lib/api.ts`](../../apps/mobile/lib/api.ts), [`lib/http.ts`](../../apps/mobile/lib/http.ts) |
| Schemas | [`lib/*-schema.ts`](../../apps/mobile/lib/) |
| CI (mobile) | [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) |

### Architecture compliance

| Topic | Source |
|-------|--------|
| Expo / TypeScript / Router / TanStack Query | [`architecture.md`](../planning-artifacts/architecture.md) — Frontend architecture |
| Tests live alongside app; CI runs quality gates | Same — repo layout + CI |
| No secrets in repo; public API base only | `app.config` / `expo-constants` — tests must not embed real tokens |

### Library / framework requirements

- **Jest 29** + **jest-expo ~55** (aligned with Expo SDK **55** in `package.json`).
- **@testing-library/react-native** for component tests.
- **Zod 4** — use `.safeParse` in tests for clear success/failure cases.
- Do **not** upgrade Expo/React Native major versions in this story unless required for test stability.

### File structure requirements

- Prefer `apps/mobile/__tests__/**/*.test.ts` to match existing layout; colocated `*.test.ts` next to modules is acceptable if consistent.
- New docs: `docs/testing-mobile.md` at repo root (parallel to [`docs/testing-backend.md`](../../docs/testing-backend.md)).

### Testing requirements

- **Meaningful assertions:** inputs → outputs; schema errors; navigation calls with expected paths (existing push tests are the model).
- **Time:** Use fake timers for `formatRelativeTimeUtc` to avoid flake.
- **HTTP:** Prefer testing **pure** request builders and parsers; mock `fetch` at the boundary if adding `http.ts` tests — avoid real network in CI.

### Previous story intelligence (5.1)

- Backend story established **documentation + CI green** as completion bar; **same** for mobile: `npm test` must pass locally and in CI after workflow change.
- **BDD-style test names** were required for Java; for TypeScript/Jest, use **clear `it('...')` descriptions** (given/when/then phrasing optional but encouraged for new files).

### Git intelligence (recent commits)

- Recent mobile work: **push deep link** (`lib/push-navigation.ts`), **device tokens**, **notifications** — protect **`push-navigation`** and **notification**-adjacent utilities with tests when touching them.

### Latest technical notes

- **Expo SDK 55** / **React 19** / **React Native 0.83** — follow `package.json`; no speculative dependency bumps.

### Project context reference

- No `project-context.md` in repo; **`epics.md`**, **`architecture.md`**, and **this file** govern implementation.

---

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

- `apps/mobile`: `npm test -- --ci`, `npm run lint`, `npm run typecheck` — **PASS** (2026-04-07); after QA sweep: **88** tests green.
- `apps/api`: `mvn -B verify` — **PASS** (2026-04-07, regression).

### Completion Notes List

- Added [`docs/testing-mobile.md`](../../docs/testing-mobile.md): commands, Jest layout, colocated mock policy, **coverage map** (lib + targeted components), **intentionally lighter coverage** (`app/` routes, `auth-context`, `notifications.ts` push path, full `api.ts` refresh loop), risk hotspots, OpenAPI cross-check note.
- **Initial story tests:** [`session.test.ts`](../../apps/mobile/__tests__/session.test.ts), [`jwt-org.test.ts`](../../apps/mobile/__tests__/jwt-org.test.ts), [`schemas.test.ts`](../../apps/mobile/__tests__/schemas.test.ts), [`issue-api-query.test.ts`](../../apps/mobile/__tests__/issue-api-query.test.ts), [`relative-time.test.ts`](../../apps/mobile/__tests__/relative-time.test.ts).
- **QA follow-up (same story):** Broadened `lib/` coverage — [`http.test.ts`](../../apps/mobile/__tests__/http.test.ts), [`config.test.ts`](../../apps/mobile/__tests__/config.test.ts), [`api-authenticated-fetch.test.ts`](../../apps/mobile/__tests__/api-authenticated-fetch.test.ts), [`auth-api.test.ts`](../../apps/mobile/__tests__/auth-api.test.ts), [`auth-storage.test.ts`](../../apps/mobile/__tests__/auth-storage.test.ts) (SecureStore mocked), [`device-token-api.test.ts`](../../apps/mobile/__tests__/device-token-api.test.ts), [`notification-api.test.ts`](../../apps/mobile/__tests__/notification-api.test.ts), [`query-keys.test.ts`](../../apps/mobile/__tests__/query-keys.test.ts), [`queryClient.test.ts`](../../apps/mobile/__tests__/queryClient.test.ts), [`theme.test.ts`](../../apps/mobile/__tests__/theme.test.ts), [`issue-row-exports.test.ts`](../../apps/mobile/__tests__/issue-row-exports.test.ts), [`assignee-picker-error.test.ts`](../../apps/mobile/__tests__/assignee-picker-error.test.ts).
- **Exports for testability (no behavior change):** [`IssueRow.tsx`](../../apps/mobile/components/IssueRow.tsx) — `issueRowStatusColor`, `issueRowStatusLabel`; [`AssigneePicker.tsx`](../../apps/mobile/components/AssigneePicker.tsx) — `pickAssigneePickerErrorMessage`.
- **CI:** Mobile job runs `npm test -- --ci` after lint and typecheck ([`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)).
- **Optional full-screen RTL:** Still deferred for provider-heavy screens; `summarizeChangeEvent` remains covered in [`IssueActivityTimeline.test.ts`](../../apps/mobile/__tests__/IssueActivityTimeline.test.ts).
- **Jest setup:** No global `setupFilesAfterEnv` — colocated `jest.mock` policy in docs.

### File List

- `docs/testing-mobile.md`
- `.github/workflows/ci.yml`
- `apps/mobile/components/IssueRow.tsx`
- `apps/mobile/components/AssigneePicker.tsx`
- `apps/mobile/__tests__/session.test.ts`
- `apps/mobile/__tests__/jwt-org.test.ts`
- `apps/mobile/__tests__/schemas.test.ts`
- `apps/mobile/__tests__/issue-api-query.test.ts`
- `apps/mobile/__tests__/relative-time.test.ts`
- `apps/mobile/__tests__/http.test.ts`
- `apps/mobile/__tests__/config.test.ts`
- `apps/mobile/__tests__/api-authenticated-fetch.test.ts`
- `apps/mobile/__tests__/auth-api.test.ts`
- `apps/mobile/__tests__/auth-storage.test.ts`
- `apps/mobile/__tests__/device-token-api.test.ts`
- `apps/mobile/__tests__/notification-api.test.ts`
- `apps/mobile/__tests__/query-keys.test.ts`
- `apps/mobile/__tests__/queryClient.test.ts`
- `apps/mobile/__tests__/theme.test.ts`
- `apps/mobile/__tests__/issue-row-exports.test.ts`
- `apps/mobile/__tests__/assignee-picker-error.test.ts`

### Change Log

- 2026-04-07: Story 5.2 — mobile Jest expansion, `docs/testing-mobile.md`, CI test step; sprint status → review.
- 2026-04-07: QA follow-up — near-full `lib/` unit coverage, component helper exports, `docs/testing-mobile.md` coverage map refresh; **88** Jest tests.

---

## Create-story validation record

**Run:** `validate` (checklist per [`.cursor/skills/bmad-create-story/checklist.md`](../../.cursor/skills/bmad-create-story/checklist.md))  
**Date:** 2026-04-07  
**Target:** `5-2-frontend-unit-test-expansion.md`  
**Sprint:** `5-2-frontend-unit-test-expansion` → `ready-for-dev`

### Verdict: **Pass** (ready for `dev-story`)

| Check | Result |
|-------|--------|
| Epics / PRD alignment | **Pass** — User story, NFR-P2, UX-DR6/DR9, and epics ACs (CI test command, high-value units, mocks, hotspots) reflected; **issue list pure helpers** explicitly added where epics call out “issue list/detail behaviors.” |
| Architecture anchors | **Pass** — Expo/Jest/RN stack, monorepo paths, CI pattern, no secrets in tests. |
| Anti–reinvent-the-wheel | **Pass** — Points to existing `__tests__`, `jest-expo`, `push-navigation` test style. |
| File locations / stack | **Pass** — `apps/mobile`, versions from `package.json`. |
| Regression / scope | **Pass** — E2E explicitly out of scope; aligns with 5.3/5.6 boundaries. |

### Findings applied during validation

| Severity | Finding | Resolution |
|----------|-----------|------------|
| **Enhancement (was gap)** | Epics require tests for **issue list/detail** pure behavior; story did not name **`buildIssueListQueryString` / `defaultIssueListParams`** in [`issue-api.ts`](../../apps/mobile/lib/issue-api.ts). | Added to AC1, tasks, and Zod/OpenAPI cross-check note. |
| Non-blocking | **`auth-storage.ts`** (SecureStore) is part of “auth helpers” but is async + native — optional **`jest.mock('expo-secure-store')`** tests; not required if deferred with a hotspot note. | Document only — implementer may list under follow-ups in `docs/testing-mobile.md`. |
| Non-blocking | No checked-in OpenAPI file; validation references **runtime** `/v3/api-docs` or API DTOs. | Addressed by sentence under Zod bullet in AC1. |

### Checklist execution

Systematic re-check against [`epics.md`](../planning-artifacts/epics.md) Story 5.2, [`architecture.md`](../planning-artifacts/architecture.md) frontend/testing notes, and `apps/mobile` layout; **critical** issues after fixes: **0**.

---

**Completion status:** Implementation complete; code review patches applied (2026-04-07) — story **done**.

### Review Findings

- [x] [Review][Patch] Assert exact Zod copy for invalid login `organizationId` — AC5 asks that invalid schema cases match user-visible messages; tighten `loginSchema` test to expect `Valid organization ID (UUID) required` (see `lib/auth/login-schema.ts`) instead of a loose `organization` substring. [`apps/mobile/__tests__/schemas.test.ts`]

- [x] [Review][Patch] Cover `ApiProblemError` title fallback in assignee picker error helper — `pickAssigneePickerErrorMessage` uses `detail ?? title`; add a case where `detail` is absent and `title` is set so the title path cannot regress. [`apps/mobile/__tests__/assignee-picker-error.test.ts`]

- [x] [Review][Patch] Strengthen invite short-password assertion — replace `message.includes('8')` with an exact match to `Password must be at least 8 characters` from `inviteUserFormSchema` refines (aligns with AC5 spot-check style used for email). [`apps/mobile/__tests__/schemas.test.ts`]
