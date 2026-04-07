# Story 5.6: CI quality signals and developer test documentation

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As a **developer**,
I want **clear CI jobs and documentation for how tests map to quality (and optional non-blocking coverage reporting)**,
so that **contributors know what to run before PRs and CI failures are interpretable**.

**Implements:** NFR-S3 (ongoing verification); Additional: repo hygiene ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 5.6]).

**Epic:** Epic 5 — Post-MVP quality, maintainability & UX polish.

**Scope:** **CI workflow** (`.github/workflows/`), **root README** and/or **`docs/`** testing docs — **no** product feature code unless a tiny doc fix references a path. **Do not** change API contracts or mobile UX for this story.

---

## Acceptance Criteria

1. **CI clarity**  
   **Given** GitHub Actions (or project CI)  
   **When** a PR runs  
   **Then** backend unit/integration, mobile unit, and lint/typecheck jobs are **named consistently** and **logs identify the failing step** (job name + step name in GitHub Actions UI).

2. **Contributor documentation**  
   **Given** documentation  
   **When** a new contributor opens README or `docs/testing.md`  
   **Then** they find commands for **unit**, **integration**, and **E2E** (if any), plus **prerequisites** (Java, Node, Docker for Testcontainers).

3. **Coverage policy**  
   **Given** optional coverage thresholds  
   **When** enabled  
   **Then** thresholds are **incremental** (not blocking the first merge of Epic 5) or reported as **informational only** — **document the policy** in the same doc set (README and/or `docs/testing.md`).

---

## Tasks / Subtasks

- [x] **CI workflow** (AC: 1)
  - [x] Open [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml): give the workflow and each **job** a clear, stable **`name:`** (human-readable) and consider **renaming job IDs** (e.g. `api` → `api-verify`, `mobile` → `mobile-quality`) so PR checks list reads unambiguously.
  - [x] **Split** the mobile job into **separate steps** (at minimum: `npm ci`, `npm run lint`, `npm run typecheck`, `npm test -- --ci`) so a red ❌ points to the exact phase; keep total behavior identical to today unless you add optional informational steps below.
  - [x] Optionally add **`npm run check:contrast`** as its own step after unit tests (fast, no emulator) — **if** you add it, treat failure as **blocking** like lint (aligns with Epic 3.9 / 5.5 accessibility baseline); **if** you skip it in CI, document it explicitly as a **required local pre-PR** command in testing docs (AC: 2).
  - [x] Confirm API job still runs **`cd apps/api && mvn -B verify`** (JDK 25 + Docker for Testcontainers); step names should mention **verify** vs **install** tooling.

- [x] **Documentation hub** (AC: 2)
  - [x] Add **`docs/testing.md`** as a **single entry page** that: links to [`docs/testing-backend.md`](../../docs/testing-backend.md), [`docs/testing-mobile.md`](../../docs/testing-mobile.md), [`docs/testing-e2e.md`](../../docs/testing-e2e.md); summarizes **CI vs local**; lists **prerequisites** (JDK 25, Node 20, Docker for `mvn verify`, Maestro for E2E).
  - [x] **Fix** [`docs/testing-backend.md`](../../docs/testing-backend.md) if it still references **`./mvnw`** — this repo **does not** ship the Maven Wrapper ([`README.md`](../../README.md)); use **`mvn -B verify`** when Maven is on `PATH`, consistent with CI.
  - [x] Update **[`README.md`](../../README.md)** Testing / CI sections to **point to `docs/testing.md`** (keep the existing table or replace with one link — avoid duplicating long command lists in two places unless the table remains a thin index).

- [x] **Coverage policy** (AC: 3)
  - [x] Decide and **document**: either (a) **no** coverage gates in CI for now + rationale, or (b) add **informational** reporting only (e.g. Jest `npm test -- --ci --coverage --coverageReporters=text-summary` **without** `coverageThreshold` failing the build; or JaCoCo report artifact for API **without** minimum enforcement). State explicitly: **incremental / informational**, not blocking first Epic 5 merge.

---

## Dev Notes

### Architecture compliance

| Topic | Source |
|-------|--------|
| GitHub Actions CI; API build/test; mobile lint/typecheck | [`architecture.md`](../planning-artifacts/architecture.md) — repo layout `.github/workflows/`, PR checks |
| Tests mirror `main`; Testcontainers for API ITs | [`architecture.md`](../planning-artifacts/architecture.md) — `src/test/java`, integration tests |

### What already exists (do not unknowingly regress)

| Asset | Role |
|-------|------|
| [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) | Two jobs: API `mvn verify`, mobile `npm ci` + lint + typecheck + Jest `--ci` |
| [`README.md`](../../README.md) | Prerequisites, Testing table, CI summary |
| [`docs/testing-backend.md`](../../docs/testing-backend.md), [`docs/testing-mobile.md`](../../docs/testing-mobile.md), [`docs/testing-e2e.md`](../../docs/testing-e2e.md) | Deep dives (5.1–5.3-era content) |

### Library / framework requirements

- **No** new CI platform (stay on GitHub Actions unless product decision changes).
- **Node 20**, **JDK 25**, **Maven** on PATH — match existing [`ci.yml`](../../.github/workflows/ci.yml) and README.

### File structure requirements

- Primary edits: [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml), [`README.md`](../../README.md), new [`docs/testing.md`](../../docs/testing.md), and small fixes to [`docs/testing-backend.md`](../../docs/testing-backend.md) if `./mvnw` is wrong.
- **Do not** add unrelated workflows or dependabot in this story unless you fold them into the same PR with clear scope (prefer **out of scope** — epics suggest those as future topics).

### Testing requirements

- **After** workflow edits: open a **draft PR** or use [`act`](https://github.com/nektos/act) locally if available; at minimum **validate YAML** (indentation, job IDs) and ensure **job names** read well in the GitHub UI.
- **No** new application unit tests required for this story unless you add a script test (not expected).

### Previous story intelligence (5.5)

- Story **5.5** completed **mobile theme + `npm run check:contrast`**. CI today does **not** run contrast; either add it as a CI step or keep it **mandatory locally** and document — avoid silent drift of theme vs check script.
- **5.5** touched [`apps/mobile/scripts/check-issue-theme-contrast.mjs`](../../apps/mobile/scripts/check-issue-theme-contrast.mjs) — unrelated to CI YAML except if you wire the script into the mobile job.

### Git intelligence (recent commits)

- Recent Epic 5 work: **5.1** backend tests, **5.2** mobile Jest + CI, **5.3** Maestro + `docs/testing-e2e.md`, **5.4** API package moves, **5.5** mobile UI. This story **ties the knot** on **discoverability** and **CI ergonomics**.

### Latest technical notes

- GitHub Actions: use **`name:`** on workflow, jobs, and steps for contributor-friendly PR check titles ([Workflow syntax](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)).
- Splitting **`run: |`** blocks into multiple **`run:`** steps is the standard way to satisfy “identify the failing step.”

### Project context reference

- No `project-context.md` in repo; follow **`epics.md` Story 5.6**, this file, and [`architecture.md`](../planning-artifacts/architecture.md).

---

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2)

### Debug Log References

- `apps/mobile`: `npm run lint`, `npm run typecheck`, `npm test -- --ci --coverage --coverageReporters=text-summary`, `npm run check:contrast` — all green.
- `apps/api`: `mvn -B verify` — green (Docker + Testcontainers).

### Completion Notes List

- **CI:** Renamed jobs to `api-verify` and `mobile-quality` with human-readable `name:`; split mobile into named steps; API steps distinguish Maven install vs `mvn -B verify`.
- **Quality:** Mobile CI runs Jest with **informational** `text-summary` coverage (no `coverageThreshold`); added **blocking** `npm run check:contrast`; added `coverage/` to `apps/mobile/.gitignore`.
- **Docs:** New [`docs/testing.md`](../../docs/testing.md) hub (prereqs, CI vs local, coverage policy); fixed `./mvnw` reference in [`docs/testing-backend.md`](../../docs/testing-backend.md); README links to hub; [`docs/testing-mobile.md`](../../docs/testing-mobile.md) and [`docs/testing-e2e.md`](../../docs/testing-e2e.md) updated for contrast + policy links.

### File List

- `.github/workflows/ci.yml`
- `README.md`
- `docs/testing.md`
- `docs/testing-backend.md`
- `docs/testing-mobile.md`
- `docs/testing-e2e.md`
- `apps/mobile/.gitignore`

### Change Log

- 2026-04-07: Story 5.6 — CI naming/steps, `docs/testing.md`, coverage policy, contrast in CI; sprint → **review**.
- 2026-04-07: Code review — branch protection N/A (no required checks); staged hub + story file; `last_updated` restored; sprint → **done**.

### Review Findings

- [x] [Review][Decision] GitHub required checks vs renamed CI jobs — **Dismissed:** repository does not rely on required status checks for the old job ids; no rule update needed.

- [x] [Review][Patch] Stage new tracked artifacts — **Done:** `docs/testing.md` and this story file added to the git index so the PR includes them.

- [x] [Review][Patch] Monotonic `last_updated` in sprint-status — **Done:** set to `2026-04-07T23:59:59Z` (forward of the mistaken `21:30:00Z` stamp). [`_bmad-output/implementation-artifacts/sprint-status.yaml`]

---

## Create-story validation record

**Checklist:** [`.cursor/skills/bmad-create-story/checklist.md`](../../.cursor/skills/bmad-create-story/checklist.md)  
**Validated:** 2026-04-07 (create-story pass)

**Verdict:** **Pass** — ACs trace to `epics.md`; concrete paths (`ci.yml`, `README.md`, `docs/testing*.md`); guardrails on optional coverage, contrast-in-CI vs local; `mvnw` doc drift called out.
