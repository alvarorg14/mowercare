# Story 5.3: End-to-end tests for critical flows

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As a **team**,
I want **automated E2E coverage of the main user journeys across API + mobile where feasible**,
so that **releases catch broken flows that unit tests miss (NFR-P1 baseline)**.

**Implements:** NFR-P1 (smoke-level confidence); cross-cutting validation of FR4–FR6, FR11–FR14, FR22–FR23 on a **smoke** subset.

**Epic:** Epic 5 — Post-MVP quality, maintainability & UX polish.

**Backlog note:** [`epics.md`](../planning-artifacts/epics.md) Story 5.3 states outcomes at BDD level; **this story file** is the implementation source of truth for toolchain choice, directory layout, env vars, CI split, and concrete routes.

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 5.3])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** monorepo **When** E2E strategy chosen and documented (**one** primary approach) **Then** docs describe local + CI, env (API URL, org bootstrap/fixtures) | Add **`docs/testing-e2e.md`** (recommended) and a short pointer from root [`README.md`](../../README.md) (update the **CI** / testing bullets so they mention E2E — README currently omits mobile `npm test`; align with [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)). |
| **Given** toolchain **When** suite runs **Then** min paths: sign-in → issues list → issue detail; plus notification list **or** push surface **if** no Apple/Google secrets in CI | Implement **Maestro** flows (see below) for **Issues** journey + **Notifications** tab; **do not** require real FCM/APNs in CI — cover **in-app notification list** UI; push receipt stays manual/device-lab if not automatable. |
| **Given** CI **When** full device E2E unavailable in GHA **Then** document fallback + explicit trade-off | Default **PR** workflow keeps **fast** checks (`mvn verify`, mobile lint/typecheck/Jest). Document **optional** workflow (e.g. `workflow_dispatch`, scheduled, or local-only Maestro) for full UI E2E; tie **API regression** to existing backend ITs ([`docs/testing-backend.md`](../../docs/testing-backend.md)). |

### Primary approach (locked for this story)

| Option | Verdict |
|--------|---------|
| **Maestro** | **Selected.** Declarative YAML flows, strong fit for **black-box** Expo dev builds / emulators without Jest-native coupling; lower CI wiring cost than Detox for a first slice. |
| Detox | Deferred — heavier native test-runner integration; valid later if team standardizes on Detox. |
| Appium | Deferred — broader scope; not needed for MVP smoke. |
| API-only smoke + optional UI driver | Partially satisfied by **existing** `mvn verify` (API path); **this story adds** the **UI driver** half via Maestro **outside** the default PR job unless you add an optional job. |

### Cross-epic boundaries

| Source | Relationship |
|--------|--------------|
| [Story 5.2](./5-2-frontend-unit-test-expansion.md) | **Unit/component** coverage and [`docs/testing-mobile.md`](../../docs/testing-mobile.md) — **do not** duplicate Jest concerns; E2E complements RTL/Jest. |
| [Story 5.1](./5-1-backend-unit-and-integration-test-expansion.md) / [`docs/testing-backend.md`](../../docs/testing-backend.md) | **API** smoke already in CI via Testcontainers ITs; E2E story **references** them as the **CI fallback** for full-stack confidence when Maestro does not run. |
| Story 5.6 (backlog) | May later merge into a single `docs/testing.md`; for 5.3, a **dedicated E2E doc** is enough. |

---

## Acceptance Criteria

1. **Strategy + documentation**  
   **Given** the monorepo  
   **When** implementation is complete  
   **Then** **`docs/testing-e2e.md`** exists and states: **Maestro** is the primary UI automation; how to install Maestro CLI; how to point the app at an API (`EXPO_PUBLIC_API_BASE_URL` per [`lib/config.ts`](../../apps/mobile/lib/config.ts)); prerequisites (Node, JDK, Docker for API tests, emulator/simulator or device for UI runs).  
   **And** **test identity** is documented: use a **non-committed** env file or shell exports for org UUID + email + password (same shape as login form — [`app/(auth)/index.tsx`](../../apps/mobile/app/(auth)/index.tsx)); document **bootstrap** for empty DB via [`README.md`](../../README.md#first-organization-and-admin-bootstrap) (`POST /api/v1/bootstrap/organization` + `X-Bootstrap-Token`).  
   **And** root **`README.md`** links to `docs/testing-e2e.md` and reflects **CI** reality (API verify, mobile lint/typecheck/**test** per `.github/workflows/ci.yml`).

2. **Journey A — Sign-in → Issues list → Issue detail**  
   **Given** API running with a bootstrapped org, a known employee user, and **at least one issue** visible in the list (seed via **create-issue API**, SQL fixture, or documented one-liner — **document the chosen precondition** in `docs/testing-e2e.md` so Maestro does not tap an empty list)  
   **When** the Maestro flow runs against a **development build** (Expo dev client or equivalent; document which)  
   **Then** the flow: enters **Organization ID**, **email**, **password** on the auth screen → reaches **Issues** list → opens **one** issue → **Issue detail** screen is shown (route [`app/(app)/(tabs)/issues/[id].tsx`](../../apps/mobile/app/(app)/(tabs)/issues/[id].tsx)).  
   **And** selectors prefer **accessibility** / visible text aligned with the app (e.g. `accessibilityLabel="Organization ID"` on the org field — see auth screen).

3. **Journey B — Notifications (no push secrets in CI)**  
   **Given** the same authenticated session (or login + navigate)  
   **When** the Maestro flow runs  
   **Then** it reaches the **in-app notification list** ([`app/(app)/(tabs)/notifications/index.tsx`](../../apps/mobile/app/(app)/(tabs)/notifications/index.tsx)) and asserts a **stable** empty or populated state (e.g. heading/list placeholder) **without** requiring FCM/APNs credentials.  
   **And** if **push tap** / deep-link journeys are **out of scope** for automation, **`docs/testing-e2e.md`** states **manual or device-lab** coverage and points to existing unit coverage ([`__tests__/push-navigation.test.ts`](../../apps/mobile/__tests__/push-navigation.test.ts)).

4. **CI constraints + explicit fallback**  
   **Given** GitHub Actions `ubuntu-latest` does not run iOS/Android device E2E by default  
   **When** a contributor opens a PR  
   **Then** **default** CI still passes using existing jobs (API `mvn verify`, mobile quality gates).  
   **And** `docs/testing-e2e.md` explicitly states the **trade-off**: full Maestro runs are **local** and/or **optional** CI (e.g. `workflow_dispatch`, self-hosted runner, or future paid emulator service); **API-level** regression remains the **`mvn verify`** integration suite documented in [`docs/testing-backend.md`](../../docs/testing-backend.md).  
   **And** the team “accepts” this by documenting it — no silent gap.

5. **No secrets in repo**  
   **Given** E2E needs credentials  
   **When** tests are committed  
   **Then** no passwords/tokens appear in source; use env vars documented in `docs/testing-e2e.md` and `.env.example` **names only** if extended.

---

## Tasks / Subtasks

- [x] **Tooling + layout** (AC: 1, 5)
  - [x] Add Maestro devDependency or document **global** `maestro` CLI install (pick one approach and document).
  - [x] Create **`/.maestro/`** (or `apps/mobile/.maestro/` — pick **one** convention, document it) with YAML flows: `issues_smoke.yaml`, `notifications_smoke.yaml`.
- [x] **Flows** (AC: 2, 3)
  - [x] Implement journey A against a running API + seeded user (bootstrap + admin or invited user per README) **and** ensure **≥1 issue** exists before the flow (HTTP create, script, or DB seed — document in `docs/testing-e2e.md`).
  - [x] Implement journey B to notifications tab; avoid push pipelines.
- [x] **Docs** (AC: 1, 4)
  - [x] Author **`docs/testing-e2e.md`**: commands, env vars, API bootstrap pointer, Maestro run instructions, CI fallback paragraph.
  - [x] Update **[`README.md`](../../README.md)** — testing section + link to E2E doc; fix mobile CI line if it still says lint/typecheck only.
- [x] **Optional CI hook** (AC: 4) — only if time-safe
  - [x] Add **optional** GitHub Actions job or **`workflow_dispatch`** that installs Maestro + Android emulator **or** document clearly **why** PR does not run Maestro (cost/flake). Minimum bar: **documentation**; optional job is a bonus.

---

## Dev Notes

### Route map (Expo Router)

| User step | File |
|-----------|------|
| Sign-in | [`app/(auth)/index.tsx`](../../apps/mobile/app/(auth)/index.tsx) |
| Issues list | [`app/(app)/(tabs)/issues/index.tsx`](../../apps/mobile/app/(app)/(tabs)/issues/index.tsx) |
| Issue detail | [`app/(app)/(tabs)/issues/[id].tsx`](../../apps/mobile/app/(app)/(tabs)/issues/[id].tsx) |
| Notifications | [`app/(app)/(tabs)/notifications/index.tsx`](../../apps/mobile/app/(app)/(tabs)/notifications/index.tsx) |

### Architecture compliance

| Topic | Source |
|-------|--------|
| Expo SDK **55**, Router, TanStack Query | [`architecture.md`](../planning-artifacts/architecture.md) — mobile stack |
| GitHub Actions CI | [`architecture.md`](../planning-artifacts/architecture.md) — `.github/workflows/` |
| API contracts / tenant isolation | Backend ITs per [`docs/testing-backend.md`](../../docs/testing-backend.md); OpenAPI `/v3/api-docs` |

### Library / framework requirements

- **Maestro** — primary UI E2E; keep versions pinned in docs when adding npm wrapper.
- **Expo** — E2E runs against a **development build** when native modules matter; document **Expo Go vs dev client** if limitations apply to Maestro.
- **Do not** add Detox + Maestro in the same story — **one** primary UI framework per epics.

### File structure requirements

- Prefer **repo-root** `.maestro/` for discoverability **or** `apps/mobile/.maestro/` — **one** place only.
- New doc: **`docs/testing-e2e.md`** at repo root (parallel to `docs/testing-mobile.md`, `docs/testing-backend.md`).

### Testing requirements

- **Jest** remains the **unit** layer — no replacement.
- Maestro tests are **integration/UI** — may be slower; keep **smoke** count minimal (two flows).
- **Flake:** prefer stable selectors (`accessibilityLabel`, button text); avoid raw delays except as last resort with comment.

### Previous story intelligence (5.2)

- [`docs/testing-mobile.md`](../../docs/testing-mobile.md) already defers **screen wiring** to Maestro/Detox — **this story delivers** that track.
- Mobile CI already runs **`npm test -- --ci`** — E2E is **additive**, not a replacement.
- **88** Jest tests — protect pure helpers via Jest; E2E should not re-assert every edge case.

### Git intelligence (recent commits)

- **5.2** added broad Jest coverage and mobile CI test step — keep E2E **orthogonal** (new directory, new doc).
- Prior **4.x** work: notifications, push deep links — E2E covers **list** tab; deep link **unit** tests remain in Jest.

### Latest technical notes

- Maestro works by driving the **installed app**; ensure docs describe **starting Metro + app** before `maestro test`.
- For **iOS** vs **Android**, document which OS the team verifies first (Android emulator often easier on CI later).

### Project context reference

- No `project-context.md` in repo; **`epics.md`**, **`architecture.md`**, **`docs/testing-*.md`**, and **this file** govern implementation.

---

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

- `apps/mobile`: `npm test -- --ci` — **PASS** (89 tests, 2026-04-08).
- `apps/api`: `mvn -B verify` — **PASS** (2026-04-08).

### Completion Notes List

- Added **Maestro** flows at repo root [`.maestro/`](../../.maestro/): `issues_smoke.yaml`, `notifications_smoke.yaml`, `README.md`.
- Added **API seed** for E2E: [`.maestro/seed.sh`](../../.maestro/seed.sh) → [`.maestro/seed.py`](../../.maestro/seed.py) (bootstrap, login, create **E2E Smoke Issue**).
- Authored [`docs/testing-e2e.md`](../../docs/testing-e2e.md) (install, env table, seed, run, **CI** subsection, push/deep-link gap).
- Updated [`README.md`](../../README.md): **Testing** table + CI mobile **`npm test -- --ci`** + Maestro note.
- Extended [`.env.example`](../../.env.example) with **comment-only** `E2E_*` names.
- Linked [`docs/testing-mobile.md`](../../docs/testing-mobile.md) → E2E doc.
- **Optional CI job:** not added — trade-off documented in `docs/testing-e2e.md` (per story).

### File List

- `.maestro/README.md`
- `.maestro/seed.sh`
- `.maestro/seed.py`
- `.maestro/issues_smoke.yaml`
- `.maestro/notifications_smoke.yaml`
- `docs/testing-e2e.md`
- `docs/testing-mobile.md`
- `README.md`
- `.env.example`

### Change Log

- 2026-04-08: Story 5.3 — Maestro E2E flows, seed script, `docs/testing-e2e.md`, README + `.env.example`; sprint status → **review**.

---

## Create-story validation record

**Run:** checklist per [`.cursor/skills/bmad-create-story/checklist.md`](../../.cursor/skills/bmad-create-story/checklist.md)  
**Date:** 2026-04-07 (re-validate: `validate` command)  
**Target:** `5-3-end-to-end-tests-for-critical-flows.md`  
**Sprint:** `5-3-end-to-end-tests-for-critical-flows` → `ready-for-dev`

### Verdict: **Pass** (ready for `dev-story`)

| Check | Result |
|-------|--------|
| Epics / PRD alignment | **Pass** — All three epics BDD clauses mapped: (1) one primary approach + docs + env/bootstrap; (2) two minimum paths (issues journey + notifications without push secrets); (3) CI fallback + explicit trade-off. |
| Architecture anchors | **Pass** — Expo Router paths, API testing doc cross-link, CI split explicit. |
| Anti–reinvent-the-wheel | **Pass** — Reuses backend ITs for API smoke; Jest for units; Maestro for UI only. |
| File locations / stack | **Pass** — `docs/testing-e2e.md`, `.maestro/` or `apps/mobile/.maestro/`, existing app routes cited. |
| Regression / scope | **Pass** — No Detox+Maestro; push secrets explicitly out of CI path for journey B. |
| Preconditions / edge cases | **Pass** (after patch) — Journey A **requires ≥1 issue**; seed/fixture must be documented (was a gap; now in AC2 + tasks). |

### Findings from re-validation

| Severity | Finding | Resolution |
|----------|---------|------------|
| **Critical (was)** | Journey A said “open **one** issue” without requiring **non-empty** list or seed steps — Maestro could fail on empty org. | **Fixed:** AC2 **Given** clause + Flows task now require **≥1 issue** and documentation in `docs/testing-e2e.md`. |
| Enhancement | Epics AC1 asks E2E “locally **and** in CI”; story correctly defers full UI E2E from default PR — ensure `docs/testing-e2e.md` includes an explicit **CI** subsection (even if “optional job / not on PR”). | Already implied by AC1/AC4; implementer must not omit a **CI** heading in the doc. |
| Non-blocking | Root [`README.md`](../../README.md) **CI** section still lists mobile as lint + typecheck only (no `npm test`) — stale vs [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml). | Called out in story AC1/tasks for implementer during `dev-story`. |

### Checklist execution

Second-pass systematic review against [`epics.md`](../planning-artifacts/epics.md) Story 5.3 (lines 875–896), [`architecture.md`](../planning-artifacts/architecture.md), Story 5.2 boundaries, and `apps/mobile/app` routes. **Critical** gap (empty issue list) closed in story text.

---

**Completion status:** Done — code review findings addressed (batch patch, 2026-04-07).

### Implementation validation (dev-story)

| AC | Evidence |
|----|----------|
| 1 | [`docs/testing-e2e.md`](../../docs/testing-e2e.md), [`README.md`](../../README.md), [`.env.example`](../../.env.example) |
| 2 | [`.maestro/issues_smoke.yaml`](../../.maestro/issues_smoke.yaml), seed creates issue |
| 3 | [`.maestro/notifications_smoke.yaml`](../../.maestro/notifications_smoke.yaml), push gap in doc |
| 4 | **CI** section in `docs/testing-e2e.md` |
| 5 | No secrets in repo; env names only in `.env.example`; `seed.py` requires `E2E_ADMIN_PASSWORD` (no default) |

### Review Findings

- [x] [Review][Patch] Merge duplicate `## CI` blocks in `docs/testing-e2e.md` into one coherent section (currently two headings back-to-back) [docs/testing-e2e.md:77-104]
- [x] [Review][Patch] Catch `URLError` and other non-`HTTPError` failures in `seed.py` so a down API exits cleanly instead of a Python traceback [.maestro/seed.py:14-26]
- [x] [Review][Patch] Add `**/__pycache__/` and/or `*.pyc` to `.gitignore` so running `seed.py` does not produce committable bytecode artifacts [.gitignore]
- [x] [Review][Patch] Replace the `eval "$(bash .maestro/seed.sh 2>/dev/null)"` pattern with explicit copy-paste exports or a safer workflow (stderr suppression hides failures; `eval` is risky) [docs/testing-e2e.md:54]
- [x] [Review][Patch] Remove embedded default password `e2epassw0rd12` for `E2E_ADMIN_PASSWORD` from `seed.py` — AC5 requires no credential material in source; require the env var or fail fast [.maestro/seed.py:34]
- [x] [Review][Patch] Harden JSON handling in `seed.py` (e.g. `organizationId` / `accessToken` presence) with clear error messages instead of `KeyError` [.maestro/seed.py:61-101]
- [x] [Review][Patch] Prefer asserting on the visible **Notifications** headline in `notifications_smoke.yaml` instead of the full subtitle string (fragile on copy edits) [.maestro/notifications_smoke.yaml:21-23]
